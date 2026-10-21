package io.github.htoor01.autota.grader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JUnitReportParserTest {

    @Test
    void countsFailuresAndErrorsAsFailedTests() throws Exception {
        TestReport report = JUnitReportParser.parse(sampleReport());

        assertThat(report.total()).isEqualTo(3);
        assertThat(report.passed()).isEqualTo(1);
        assertThat(report.failures()).containsExactly(
                new TestFailure("subtractsInOrder()", "expected: <6> but was: <-6>"),
                new TestFailure("multipliesByNegativeNumber()", "multipliesByNegativeNumber() timed out after 2 seconds"));
    }

    @Test
    void rejectsDoctypeSoStudentCodeCannotSmuggleInEntities(@TempDir Path dir) throws IOException {
        Path report = dir.resolve(JUnitReportParser.REPORT_FILE);
        Files.writeString(report, """
                <?xml version="1.0"?>
                <!DOCTYPE testsuite [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <testsuite><testcase name="&secret;"/></testsuite>
                """);

        assertThatThrownBy(() -> JUnitReportParser.parse(report)).isInstanceOf(IOException.class);
    }

    @Test
    void rejectsMalformedReport(@TempDir Path dir) throws IOException {
        Path report = Files.writeString(dir.resolve(JUnitReportParser.REPORT_FILE), "<testsuite>");

        assertThatThrownBy(() -> JUnitReportParser.parse(report)).isInstanceOf(IOException.class);
    }

    static Path sampleReport() {
        try {
            return Path.of(JUnitReportParserTest.class.getResource("/grader/" + JUnitReportParser.REPORT_FILE).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
