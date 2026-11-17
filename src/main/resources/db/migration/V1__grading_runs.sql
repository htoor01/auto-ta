-- One row per press of "grade": which assignment, and how far the background job got.
CREATE TABLE grading_run (
    id             TEXT PRIMARY KEY,                  -- UUID
    course_id      TEXT NOT NULL,
    course_work_id TEXT NOT NULL,
    status         TEXT NOT NULL,                     -- QUEUED, RUNNING, COMPLETED, FAILED
    created_at     TEXT NOT NULL,                     -- ISO-8601 instants (SQLite has no timestamp type)
    started_at     TEXT,
    finished_at    TEXT,
    error          TEXT
);

CREATE INDEX grading_run_by_course_work ON grading_run (course_id, course_work_id, created_at);

-- One row per graded submission, written as soon as that submission is done.
CREATE TABLE grading_result (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id          TEXT NOT NULL REFERENCES grading_run (id) ON DELETE CASCADE,
    submission_id   TEXT NOT NULL,
    student_id      TEXT,
    student_name    TEXT,
    status          TEXT NOT NULL,                    -- GradeStatus
    tests_passed    INTEGER NOT NULL,
    tests_total     INTEGER NOT NULL,
    suggested_grade REAL,
    max_points      REAL,
    failures        TEXT NOT NULL,                    -- JSON array of {test, message}
    duration_millis INTEGER NOT NULL,
    detail          TEXT,
    UNIQUE (run_id, submission_id)
);
