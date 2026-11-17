package io.github.htoor01.autota.grader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.htoor01.autota.classroom.ClassroomClient;
import io.github.htoor01.autota.classroom.ResourceNotFoundException;
import io.github.htoor01.autota.classroom.model.Attachment;
import io.github.htoor01.autota.classroom.model.AttachmentContent;
import io.github.htoor01.autota.classroom.model.CourseWork;
import io.github.htoor01.autota.classroom.model.Submission;

/**
 * Grades every submission for an assignment: downloads the student's {@code .java} files, runs the
 * instructor tests from {@code <testsDir>/<courseWorkId>/} in a container, and turns the report into a
 * suggested grade. One failing submission never stops the others.
 *
 * <p>Split into {@link #prepare} (fast checks, run while the request waits, so mistakes fail with a 404)
 * and {@link #grade} (slow, run in the background by {@link GradingQueue}).
 */
@Service
public class GradingService {

    private static final Logger log = LoggerFactory.getLogger(GradingService.class);
    /** File names come from students, so only plain Java file names are written to disk. */
    private static final Pattern SAFE_JAVA_FILE = Pattern.compile("[A-Za-z0-9_$-]+\\.java");
    private static final int MAX_DETAIL_LENGTH = 4000;

    private final ClassroomClient classroomClient;
    private final ContainerRunner containerRunner;
    private final GraderProperties properties;

    GradingService(ClassroomClient classroomClient, ContainerRunner containerRunner, GraderProperties properties) {
        this.classroomClient = classroomClient;
        this.containerRunner = containerRunner;
        this.properties = properties;
    }

    /** Checks the assignment exists and has instructor tests. */
    public GradingJob prepare(String courseId, String courseWorkId) {
        CourseWork courseWork = classroomClient.listCourseWork(courseId).stream()
                .filter(work -> work.id().equals(courseWorkId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Coursework " + courseWorkId + " not found in course " + courseId));
        return new GradingJob(courseId, courseWorkId, courseWork.maxPoints(), testsDirFor(courseWorkId));
    }

    /** Grades every submission, handing each result to {@code onResult} as soon as it is ready. */
    public void grade(GradingJob job, Consumer<GradingResult> onResult) {
        for (Submission submission : classroomClient.listSubmissions(job.courseId(), job.courseWorkId())) {
            onResult.accept(gradeSubmission(submission, job.maxPoints(), job.testsDir()));
        }
    }

    /** An assignment that passed {@link #prepare} and is ready to grade. */
    public record GradingJob(String courseId, String courseWorkId, Double maxPoints, Path testsDir) {
    }

    private Path testsDirFor(String courseWorkId) {
        Path root = properties.testsDir().toAbsolutePath().normalize();
        Path dir = root.resolve(courseWorkId).normalize();
        // courseWorkId comes from the URL; don't let "../" pick a folder outside the tests root.
        if (!dir.getParent().equals(root) || !Files.isDirectory(dir)) {
            throw new ResourceNotFoundException("No grader tests found for coursework " + courseWorkId
                    + " (expected a folder " + properties.testsDir().resolve(courseWorkId) + ")");
        }
        return dir;
    }

    private GradingResult gradeSubmission(Submission submission, Double maxPoints, Path testsDir) {
        long start = System.nanoTime();
        Path workDir = properties.workDir().toAbsolutePath().resolve(UUID.randomUUID().toString());
        try {
            Path submissionDir = Files.createDirectories(workDir.resolve("submission"));
            if (downloadJavaFiles(submission, submissionDir) == 0) {
                return result(submission, GradeStatus.NO_SUBMISSION, null, maxPoints, null, start,
                        "No .java files attached");
            }
            Path reportsDir = Files.createDirectories(workDir.resolve("reports"));
            // The container runs as "nobody", which must be able to write the report.
            Files.setPosixFilePermissions(reportsDir, PosixFilePermissions.fromString("rwxrwxrwx"));

            ContainerRunner.Run run = containerRunner.run(submissionDir, testsDir, reportsDir);
            if (run.timedOut()) {
                return result(submission, GradeStatus.TIMEOUT, null, maxPoints, 0.0, start,
                        "Stopped after " + properties.timeout().toSeconds() + "s");
            }
            if (run.exitCode() == ContainerRunner.Run.COMPILE_ERROR) {
                return result(submission, GradeStatus.COMPILE_ERROR, null, maxPoints, 0.0, start, run.output());
            }
            Path report = reportsDir.resolve(JUnitReportParser.REPORT_FILE);
            if (!Files.isRegularFile(report)) {
                return result(submission, GradeStatus.ERROR, null, maxPoints, null, start,
                        "Grader exited with code " + run.exitCode() + " and no test report:\n" + run.output());
            }
            TestReport tests = JUnitReportParser.parse(report);
            return result(submission, GradeStatus.GRADED, tests, maxPoints, score(tests, maxPoints), start, null);
        } catch (IOException | RuntimeException e) {
            log.error("Grading submission {} failed", submission.id(), e);
            return result(submission, GradeStatus.ERROR, null, maxPoints, null, start, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return result(submission, GradeStatus.ERROR, null, maxPoints, null, start, "Interrupted");
        } finally {
            deleteQuietly(workDir);
        }
    }

    private int downloadJavaFiles(Submission submission, Path submissionDir) throws IOException {
        int written = 0;
        for (Attachment attachment : submission.attachments()) {
            if (!SAFE_JAVA_FILE.matcher(attachment.title()).matches()) {
                log.info("Skipping attachment '{}' on submission {}", attachment.title(), submission.id());
                continue;
            }
            AttachmentContent content = classroomClient.downloadAttachment(attachment.driveFileId());
            Files.write(submissionDir.resolve(attachment.title()), content.content());
            written++;
        }
        return written;
    }

    /** Share of tests passed times the assignment's points, rounded to 2 decimals. */
    static Double score(TestReport tests, Double maxPoints) {
        if (maxPoints == null || tests.total() == 0) {
            return null;
        }
        return Math.round(maxPoints * tests.passed() / tests.total() * 100) / 100.0;
    }

    private static GradingResult result(Submission submission, GradeStatus status, TestReport tests,
                                        Double maxPoints, Double grade, long start, String detail) {
        return new GradingResult(
                submission.id(),
                submission.studentId(),
                submission.studentName(),
                status,
                tests == null ? 0 : tests.passed(),
                tests == null ? 0 : tests.total(),
                grade,
                maxPoints,
                tests == null ? List.of() : tests.failures(),
                (System.nanoTime() - start) / 1_000_000,
                detail == null || detail.length() <= MAX_DETAIL_LENGTH ? detail : detail.substring(0, MAX_DETAIL_LENGTH) + "...");
    }

    private static void deleteQuietly(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            log.warn("Could not delete grading work folder {}", dir, e);
        }
    }
}
