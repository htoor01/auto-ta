package io.github.htoor01.autota.grader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads the JUnit XML report the grader container writes.
 *
 * <p>The report is written next to student code, so it is untrusted input: DOCTYPEs are rejected
 * (no XXE or entity expansion) and oversized files are refused before parsing.
 */
final class JUnitReportParser {

    static final String REPORT_FILE = "TEST-junit-jupiter.xml";
    private static final long MAX_REPORT_BYTES = 5 * 1024 * 1024;
    private static final int MAX_MESSAGE_LENGTH = 500;

    private JUnitReportParser() {
    }

    static TestReport parse(Path report) throws IOException {
        if (Files.size(report) > MAX_REPORT_BYTES) {
            throw new IOException("Test report is larger than " + MAX_REPORT_BYTES + " bytes");
        }
        try (InputStream in = Files.newInputStream(report)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Element root = factory.newDocumentBuilder().parse(in).getDocumentElement();

            NodeList testCases = root.getElementsByTagName("testcase");
            List<TestFailure> failures = new ArrayList<>();
            for (int i = 0; i < testCases.getLength(); i++) {
                Element testCase = (Element) testCases.item(i);
                Element problem = firstChild(testCase, "failure", "error");
                if (problem != null) {
                    failures.add(new TestFailure(testCase.getAttribute("name"), truncate(problem.getAttribute("message"))));
                }
            }
            return new TestReport(testCases.getLength(), failures);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Test report is not valid JUnit XML: " + e.getMessage(), e);
        }
    }

    private static Element firstChild(Element parent, String... tagNames) {
        for (String tagName : tagNames) {
            NodeList children = parent.getElementsByTagName(tagName);
            if (children.getLength() > 0) {
                return (Element) children.item(0);
            }
        }
        return null;
    }

    private static String truncate(String message) {
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }
}
