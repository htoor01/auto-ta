# CLAUDE.md

Guidance for Claude Code sessions in this repository.

## Project

AutoTA is an autograder for Java assignments from Google Classroom: pull submissions, run instructor JUnit tests against each one in a sandboxed Docker container, suggest a grade, and let the instructor approve it. It is being built as a production-style (SRE) project for the MLH Fellowship Production Engineering track, so reliability, observability and documented trade-offs matter as much as features.

Version 1 was a script (not in this repo). Version 2 (this repo) pulls data and grades it in Docker; see "Status" below.

## Commands

```
docker build -t autota-grader:latest docker/grader   # grader image (needed for grading and *IT tests)
./mvnw verify                                        # build + all tests (what CI runs; *IT skipped without the image)
./mvnw test -Dtest=CourseControllerTest              # one test class
./mvnw spring-boot:run                               # run with mock data (default profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=google   # real Classroom (needs secrets/credentials.json)
```

Smoke test: `curl localhost:8080/api/courses`, `curl localhost:8080/actuator/health`, and `curl -X POST localhost:8080/api/courses/course-java-2026/coursework/cw-calculator/grade` (expect 100, 71.43, COMPILE_ERROR, 85.71, NO_SUBMISSION).

## Stack

Java 25, Spring Boot 4.1, Maven Wrapper (Maven 3.9.16), JUnit 5, Google Classroom + Drive API clients. CI: `.github/workflows/ci.yml` (job name "Build and test").

Spring Boot 4 differs from most examples online; check before writing code:
- Web starter is `spring-boot-starter-webmvc`; `@WebMvcTest` is in `org.springframework.boot.webmvc.test.autoconfigure` (needs `spring-boot-starter-webmvc-test`).
- Jackson 3: `tools.jackson.databind.json.JsonMapper`, not `com.fasterxml.jackson.databind.ObjectMapper`.
- Mock beans in tests: `@MockitoBean`, not the removed `@MockBean`.

## Architecture

- `classroom/ClassroomClient` is the only way the app talks to Classroom. Two implementations, selected by Spring profile:
  - `google/GoogleClassroomClient` (`google` profile): real API. `GoogleConfig` builds the clients, `GoogleCredentialFactory` runs the desktop OAuth flow, `GoogleModelMapper` converts Google types.
  - `mock/MockClassroomClient` (`mock`, the default): read-only, serves `src/main/resources/data/mock/`.
- `classroom/model/` holds AutoTA's own records. Google's model classes must not leak outside `classroom/google/`.
- Errors: clients throw `ResourceNotFoundException` or `ClassroomException` (carries Google's HTTP status). `web/ApiExceptionHandler` maps them to RFC 9457 `ProblemDetail`: 404, 403 (Google permission errors), 502 (other Google failures).
- `grader/GradingService` grades an assignment: downloads `.java` attachments (file names filtered), runs `ContainerRunner` per submission, parses the JUnit XML with `JUnitReportParser` (untrusted input: no DOCTYPE, size cap), scores passed/total × maxPoints. Statuses: GRADED, COMPILE_ERROR (0), TIMEOUT (0), NO_SUBMISSION and ERROR (no grade). Exposed by `web/GradingController` (`POST .../grade`, synchronous).
- `grader/DockerContainerRunner` shells out to the `docker` CLI with the sandbox flags (no network, read-only, cap-drop ALL, nobody, memory/CPU/pids limits, wall-clock timeout, `autota.grader=true` label; orphans removed on startup). `docker/grader/grade.sh` exit codes: 0 pass, 1 failures, 2 no tests, 3 compile error. Per-test timeout is 2 s.
- Instructor tests: `grader-tests/<courseWorkId>/` (default package, like the student classes).
- Config is bound from `autota.classroom.*` and `autota.grader.*` in `application.yml` via the `ClassroomProperties` and `GraderProperties` records. Only `/actuator/health` is exposed.
- Style: package-private classes where possible, constructor injection, records for data, Javadoc only where the "why" isn't obvious.

## Status and roadmap

Done: Classroom pull (courses, coursework, submissions, attachment download), REST API, mock data, Docker grader with grading endpoint, 31 unit tests + 1 Docker integration test (`DockerGradingIT`, run by failsafe; CI builds the image and sets `AUTOTA_REQUIRE_DOCKER=true` so it can't silently skip), CI, protected `main`.

Next, in order (check feasibility first):
1. ~~Grader~~ (done)
2. **SQLite** for grading runs/results (plain Spring JDBC + Flyway; single writer, single instance, document the trade-off).
3. **Metrics**: Micrometer → `/actuator/prometheus`, Prometheus + Grafana in Docker Compose. Grading counters by outcome, container duration timer, pending-approval gauge.
4. Approval UI (includes the reusable test library from issue #10), then pushing grades.

Deliberate decisions:
- **No grade push on `main`.** Classroom only allows grade changes on coursework created by the same Google Cloud project, so pushing is deferred. The push code is saved on branch `api-config` at commit `aa85d08`; don't delete that branch.
- The four mock `Calculator.java` submissions are test fixtures for the grader: correct, partially wrong, does not compile, infinite loop. Keep them that way (file-004's loop must truly never end; an `int` counter that overflows terminates in about a second).
- An infinite loop gets partial credit (per-test timeout), not 0; the container timeout (0 + TIMEOUT) is only the backstop.

## Rules for working in this repo

- **Never commit, push, or open/merge PRs.** Staging (`git add`) is allowed. Finish work by listing the files for the commit, a suggested commit message, and a PR description; the owner commits and opens PRs.
- **No AI attribution** in suggested commit messages or PR descriptions (no `Co-Authored-By`, no "Generated with" footer).
- Large changes are split into 2-3 commits by layer (e.g. image/fixtures, app code, tests/CI) within one PR.
- **Before suggesting any commit, scan the staged content for secrets and confidential data** (OAuth client secrets, tokens, API keys, private keys, `credentials.json`, `.env`, real student names/emails/grades). If anything is found, don't suggest the commit: flag it (without printing the value) and let the owner decide on `.gitignore` vs `.env`. Report the scan result even when clean.
- **Student data must be placeholders** (Student A, Student B, ...). Never real names.
- **Check external constraints during planning, not after.** Before designing around an external API or tool, read the reference docs for the methods it relies on (permissions, quotas, licensing, privacy) and mark assumptions as verified/unverified.
- One concern per PR, from a branch off an up-to-date `main`. Only merge commits are allowed (squash breaks branches that carry two PRs). Don't push a branch's second commit until its first PR is merged.
- `main` is protected: PRs need the "Build and test" check to pass.
- Keep `README.md` accurate: anything not built yet is marked planned. README changes go in their own `docs/...` PR.
- **Three corrections make a rule.** When the owner corrects the same kind of mistake, record it (and how many times it has happened) in Claude's memory so it is counted across sessions. On the third correction, add a rule for it to this file and say so, so the owner can review it and include it in the next commit.

## Local environment gotchas

- VS Code's Java extension rebuilds into `target/` when `pom.xml` or sources change, which can make Maven fail with missing class files. Wait a moment and rerun `./mvnw clean verify`.
- The shell is zsh: unquoted `$var` holding several paths is not word-split (use `xargs`). macOS has no `timeout` command.
- Docker runs through Colima (no Docker Desktop). The socket is `~/.colima/default/docker.sock`, not `/var/run/docker.sock`; Colima must be started (`colima start`) after a reboot. Only the home folder is shared with the VM, so grader bind mounts can't use the system temp dir.
- `ADD --chmod` only sets the file's mode; parent folders it creates are root-only, so the `nobody` user can't reach them.
- `gh` is not authenticated; read CI status from the public API (`curl https://api.github.com/repos/htoor01/auto-ta/actions/runs?branch=...`).
- Secrets live in `secrets/` and OAuth tokens in `tokens/`; both are gitignored and must never be read into commits or output.
