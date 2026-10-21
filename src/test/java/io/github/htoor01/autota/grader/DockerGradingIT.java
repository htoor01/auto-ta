package io.github.htoor01.autota.grader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Grades the four mock Calculator submissions in real containers. Needs Docker and the grader image
 * ({@code docker build -t autota-grader:latest docker/grader}); skipped without them, except in CI where
 * {@code AUTOTA_REQUIRE_DOCKER=true} turns a missing image into a failure.
 */
@SpringBootTest
class DockerGradingIT {

    @Autowired
    private GradingService gradingService;

    @Autowired
    private GraderProperties properties;

    @BeforeAll
    static void requireGraderImage() throws Exception {
        boolean available;
        try {
            Process inspect = new ProcessBuilder("docker", "image", "inspect", "autota-grader:latest")
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            available = inspect.waitFor(30, TimeUnit.SECONDS) && inspect.exitValue() == 0;
        } catch (IOException e) {
            available = false;
        }
        if (!"true".equals(System.getenv("AUTOTA_REQUIRE_DOCKER"))) {
            assumeThat(available).as("Docker and the autota-grader image are available").isTrue();
        }
        assertThat(available).as("autota-grader image is required (AUTOTA_REQUIRE_DOCKER=true)").isTrue();
    }

    @Test
    void gradesEachKindOfSampleSubmission() {
        List<GradingResult> results = gradingService.gradeAssignment("course-java-2026", "cw-calculator");
        Map<String, GradingResult> byId = results.stream()
                .collect(Collectors.toMap(GradingResult::submissionId, Function.identity()));

        assertThat(byId.get("sub-001").status()).isEqualTo(GradeStatus.GRADED);
        assertThat(byId.get("sub-001").suggestedGrade()).isEqualTo(100.0);

        assertThat(byId.get("sub-002").status()).isEqualTo(GradeStatus.GRADED);
        assertThat(byId.get("sub-002").testsPassed()).isEqualTo(5);
        assertThat(byId.get("sub-002").failures()).extracting(TestFailure::test)
                .containsExactlyInAnyOrder("subtractsInOrder()", "divideByZeroThrows()");

        assertThat(byId.get("sub-003").status()).isEqualTo(GradeStatus.COMPILE_ERROR);
        assertThat(byId.get("sub-003").suggestedGrade()).isEqualTo(0.0);

        // The infinite loop costs only the test that triggers it (per-test timeout in grade.sh).
        assertThat(byId.get("sub-004").status()).isEqualTo(GradeStatus.GRADED);
        assertThat(byId.get("sub-004").failures()).extracting(TestFailure::test)
                .containsExactly("multipliesByNegativeNumber()");

        assertThat(byId.get("sub-005").status()).isEqualTo(GradeStatus.NO_SUBMISSION);
        assertThat(properties.workDir().toFile().list()).isNullOrEmpty();
    }
}
