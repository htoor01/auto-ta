# Setup guide

How to install, run and configure AutoTA. For how it works inside, see [Architecture](architecture.md).

- [Prerequisites](#prerequisites)
- [Build and test](#build-and-test)
- [Run with mock data](#run-with-mock-data)
- [API](#api)
- [Connect to Google Classroom](#connect-to-google-classroom)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

## Prerequisites

- **Git**: run `git version` to check (see the [Git install docs](https://git-scm.com/install/mac)).
- **A JDK, version 25 or newer.** Maven does not need to be installed: the included Maven Wrapper (`./mvnw`) downloads the right version on first use.
- **Docker**, to run the grader. On macOS without Docker Desktop, use [Colima](https://github.com/abiosoft/colima):
  ```bash
  brew install colima docker docker-buildx
  mkdir -p ~/.docker && echo '{ "cliPluginsExtraDirs": ["/opt/homebrew/lib/docker/cli-plugins"] }' > ~/.docker/config.json
  colima start --cpu 2 --memory 4
  docker run --rm hello-world     # check that Docker works
  ```

## Build and test

```bash
git clone https://github.com/htoor01/auto-ta.git
cd auto-ta
docker build -t autota-grader:latest docker/grader   # the image every submission runs in
./mvnw verify                                        # build and run all tests
```

`./mvnw verify` runs the unit tests, then the Docker integration test (`DockerGradingIT`), which grades the sample submissions in real containers. Without the grader image the integration test is skipped locally; in CI it is required.

## Run with mock data

```bash
./mvnw spring-boot:run
```

The default `mock` profile serves sample data from `src/main/resources/data/mock/`: one course, two assignments and five placeholder students (Student A to E). The four sample `Calculator.java` submissions are deliberately varied to exercise the grader: one correct, one partially wrong, one that does not compile, and one with an infinite loop.

Check that it is up:

```bash
curl localhost:8080/actuator/health     # {"status":"UP",...}
curl localhost:8080/api/courses
```

```json
[{"id":"course-java-2026","name":"Intro to Java","section":"2026 Batch","state":"ACTIVE"}]
```

Grading results are stored in `autota.db` (SQLite) in the folder AutoTA runs from. It holds student names and grades, so it is gitignored; keep it private.

## API

| Endpoint | Returns |
|---|---|
| `GET /api/courses` | Active courses you teach |
| `GET /api/courses/{courseId}/coursework` | Assignments in a course |
| `GET /api/courses/{courseId}/coursework/{courseWorkId}/submissions` | Student submissions for an assignment |
| `POST /api/courses/{courseId}/coursework/{courseWorkId}/grade` | Starts grading every submission; `202 Accepted` with the new run |
| `GET /api/grading-runs/{runId}` | A grading run and the results saved so far |
| `GET /api/courses/{courseId}/coursework/{courseWorkId}/grading-runs` | The 20 most recent runs for an assignment, newest first |
| `GET /actuator/health` | Whether the service is up |

### Grading an assignment

```bash
curl -i -X POST localhost:8080/api/courses/course-java-2026/coursework/cw-calculator/grade
```

AutoTA checks the assignment exists and has instructor tests, queues the run and answers straight away:

```
HTTP/1.1 202
Location: /api/grading-runs/d1acc3fb-304a-4910-b8c8-ab2842168f28
```

```json
{"id":"d1acc3fb-304a-4910-b8c8-ab2842168f28","courseId":"course-java-2026","courseWorkId":"cw-calculator",
 "status":"QUEUED","createdAt":"2026-11-17T02:49:45.378939Z","startedAt":null,"finishedAt":null,"error":null,"results":[]}
```

Poll the `Location` until `status` is `COMPLETED` (or `FAILED`). Results appear one by one while the run is `RUNNING`; the sample assignment takes about 5 seconds.

```bash
curl localhost:8080/api/grading-runs/d1acc3fb-304a-4910-b8c8-ab2842168f28
```

```json
{"id":"d1acc3fb-304a-4910-b8c8-ab2842168f28","status":"COMPLETED",
 "startedAt":"2026-11-17T02:49:45.379608Z","finishedAt":"2026-11-17T02:49:50.491552Z","error":null,
 "results":[
  {"submissionId":"sub-002","studentId":"student-002","studentName":"Student B","status":"GRADED",
   "testsPassed":5,"testsTotal":7,"suggestedGrade":71.43,"maxPoints":100.0,
   "failures":[{"test":"subtractsInOrder()","message":"expected: <6> but was: <-6>"},
               {"test":"divideByZeroThrows()","message":"Expected java.lang.ArithmeticException to be thrown, but nothing was thrown."}],
   "durationMillis":857,"detail":null},
  ...]}
```

(Shortened: the other fields and results are left out.) What each result `status` means is described in [Architecture](architecture.md#grading-results).

### Errors

Errors are [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem responses:

| Status | When |
|---|---|
| `404 Not Found` | Unknown course, assignment or grading run, or an assignment with no instructor tests |
| `403 Forbidden` | Google refused the request (permissions) |
| `502 Bad Gateway` | Google Classroom failed |
| `503 Service Unavailable` | Too many grading runs are already queued; retry after the `Retry-After` seconds |

For example, grading an assignment that has no tests yet:

```json
{"detail":"No grader tests found for coursework cw-strings (expected a folder grader-tests/cw-strings)",
 "instance":"/api/courses/course-java-2026/coursework/cw-strings/grade","status":404,"title":"Not Found"}
```

### Adding tests for an assignment

Put the instructor's JUnit tests in `grader-tests/<courseWorkId>/`. See [CONTRIBUTING.md](../CONTRIBUTING.md#adding-instructor-tests) for the checklist.

## Connect to Google Classroom

1. In [Google Cloud Console](https://console.cloud.google.com/), create a project and enable the **Google Classroom API** and **Google Drive API**.
2. Configure the OAuth consent screen and add your Google account as a test user.
3. Create an OAuth client ID of type **Desktop app** and download its JSON.
4. Save it as `secrets/credentials.json` in the project root. The `secrets/` folder is gitignored; never commit it.
5. Run with the `google` profile:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=google
   ```
   A browser window opens for consent the first time; tokens are then cached in `tokens/` (also gitignored).

## Configuration

Settings live in `src/main/resources/application.yml` and can be overridden with environment variables or `--name=value` arguments.

| Setting | Default | Meaning |
|---|---|---|
| `spring.profiles.active` | `mock` | `mock` for sample data, `google` for real Classroom |
| `spring.datasource.url` | `jdbc:sqlite:autota.db?...` | Where grading results are stored |
| `autota.classroom.credentials-path` | `secrets/credentials.json` | Google OAuth client file |
| `autota.classroom.tokens-dir` | `tokens` | Where Google sign-in tokens are cached |
| `autota.grader.docker-command` | `docker` | Docker CLI; use a full path if AutoTA's `PATH` doesn't include it |
| `autota.grader.image` | `autota-grader:latest` | Image built from `docker/grader` |
| `autota.grader.tests-dir` | `grader-tests` | Instructor tests, one folder per assignment |
| `autota.grader.work-dir` | `grader-work` | Scratch space for each submission (see Troubleshooting for Colima) |
| `autota.grader.timeout` | `60s` | Wall-clock limit for one container |
| `autota.grader.memory` | `512m` | Container memory limit (no swap) |
| `autota.grader.cpus` | `1.0` | Container CPU limit |
| `autota.grader.pids-limit` | `128` | Maximum processes in a container |
| `autota.grader.queue-capacity` | `10` | Runs that may wait behind the current one before requests get `503` |

## Troubleshooting

- **Every result is `ERROR` with "no test report".** Check that Docker is running (`docker info`; with Colima, `colima start` after a reboot) and that the image exists (`docker image ls autota-grader`).
- **Grading fails on macOS with Colima, but `docker run hello-world` works.** Colima only shares your home folder with its VM, so `autota.grader.work-dir` must be under it. The default `grader-work/` inside the project is.
- **`docker: command not found` from AutoTA but not from your terminal.** Your IDE may start AutoTA with a shorter `PATH`; set `autota.grader.docker-command` to the full path (`which docker`).
- **A grading run is `FAILED` with "AutoTA stopped before this run finished".** AutoTA was stopped mid-run. Results finished before that are kept; start the run again.
- **Maven fails with missing class files right after you edit code.** An IDE (for example VS Code's Java extension) may be rebuilding into `target/` at the same time. Wait a moment and run `./mvnw clean verify`.
