package io.github.htoor01.autota.grader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Stores grading runs and results in SQLite (schema: {@code db/migration}). */
@Repository
public class GradingRunRepository {

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;

    GradingRunRepository(JdbcClient jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    GradingRun create(String courseId, String courseWorkId) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        jdbc.sql("""
                        INSERT INTO grading_run (id, course_id, course_work_id, status, created_at)
                        VALUES (?, ?, ?, ?, ?)""")
                .params(id, courseId, courseWorkId, GradingRunStatus.QUEUED.name(), now.toString())
                .update();
        return new GradingRun(id, courseId, courseWorkId, GradingRunStatus.QUEUED, now, null, null, null, List.of());
    }

    void markRunning(String runId) {
        jdbc.sql("UPDATE grading_run SET status = ?, started_at = ? WHERE id = ?")
                .params(GradingRunStatus.RUNNING.name(), Instant.now().toString(), runId)
                .update();
    }

    void finish(String runId, GradingRunStatus status, String error) {
        jdbc.sql("UPDATE grading_run SET status = ?, finished_at = ?, error = ? WHERE id = ?")
                .params(status.name(), Instant.now().toString(), error, runId)
                .update();
    }

    /** Marks runs that were queued or running when AutoTA stopped as failed. Returns how many. */
    int failUnfinished(String reason) {
        return jdbc.sql("UPDATE grading_run SET status = ?, finished_at = ?, error = ? WHERE status IN (?, ?)")
                .params(GradingRunStatus.FAILED.name(), Instant.now().toString(), reason,
                        GradingRunStatus.QUEUED.name(), GradingRunStatus.RUNNING.name())
                .update();
    }

    void saveResult(String runId, GradingResult result) {
        jdbc.sql("""
                        INSERT INTO grading_result (run_id, submission_id, student_id, student_name, status,
                            tests_passed, tests_total, suggested_grade, max_points, failures, duration_millis, detail)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")
                .params(runId, result.submissionId(), result.studentId(), result.studentName(), result.status().name(),
                        result.testsPassed(), result.testsTotal(), result.suggestedGrade(), result.maxPoints(),
                        jsonMapper.writeValueAsString(result.failures()), result.durationMillis(), result.detail())
                .update();
    }

    public Optional<GradingRun> find(String runId) {
        return jdbc.sql("SELECT * FROM grading_run WHERE id = ?")
                .param(runId)
                .query(this::mapRun)
                .optional();
    }

    /** Most recent runs first. */
    public List<GradingRun> findByCourseWork(String courseId, String courseWorkId, int limit) {
        return jdbc.sql("""
                        SELECT * FROM grading_run WHERE course_id = ? AND course_work_id = ?
                        ORDER BY created_at DESC LIMIT ?""")
                .params(courseId, courseWorkId, limit)
                .query(this::mapRun)
                .list();
    }

    private GradingRun mapRun(ResultSet rs, int row) throws SQLException {
        String id = rs.getString("id");
        return new GradingRun(
                id,
                rs.getString("course_id"),
                rs.getString("course_work_id"),
                GradingRunStatus.valueOf(rs.getString("status")),
                instant(rs.getString("created_at")),
                instant(rs.getString("started_at")),
                instant(rs.getString("finished_at")),
                rs.getString("error"),
                results(id));
    }

    private List<GradingResult> results(String runId) {
        return jdbc.sql("SELECT * FROM grading_result WHERE run_id = ? ORDER BY id")
                .param(runId)
                .query((rs, row) -> new GradingResult(
                        rs.getString("submission_id"),
                        rs.getString("student_id"),
                        rs.getString("student_name"),
                        GradeStatus.valueOf(rs.getString("status")),
                        rs.getInt("tests_passed"),
                        rs.getInt("tests_total"),
                        nullableDouble(rs, "suggested_grade"),
                        nullableDouble(rs, "max_points"),
                        List.of(jsonMapper.readValue(rs.getString("failures"), TestFailure[].class)),
                        rs.getLong("duration_millis"),
                        rs.getString("detail")))
                .list();
    }

    /** sqlite-jdbc can't convert a column with getObject(..., Double.class), so read it the classic way. */
    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static Instant instant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
