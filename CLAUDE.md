# CLAUDE.md

Guidance for Claude Code sessions in this repository.

## Project

AutoTA is an autograder for Java assignments from Google Classroom: pull submissions, run instructor JUnit tests against each one in a sandboxed Docker container, suggest a grade, and let the instructor approve it. It is being built as a production-style (SRE) project for the MLH Fellowship Production Engineering track, so reliability, observability and documented trade-offs matter as much as features.

Version 1 was a script (not in this repo). Version 2 (this repo) currently only **pulls** data; see "Status" below.

## Commands

```
./mvnw verify                                        # build + all tests (what CI runs)
./mvnw test -Dtest=CourseControllerTest              # one test class
./mvnw spring-boot:run                               # run with mock data (default profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=google   # real Classroom (needs secrets/credentials.json)
```

Smoke test: `curl localhost:8080/api/courses` and `curl localhost:8080/actuator/health`.

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
- Config is bound from `autota.classroom.*` in `application.yml` via the `ClassroomProperties` record. Only `/actuator/health` is exposed.
- Style: package-private classes where possible, constructor injection, records for data, Javadoc only where the "why" isn't obvious.

## Status and roadmap

Done: Classroom pull (courses, coursework, submissions, attachment download), REST API, mock data, 18 tests, CI, protected `main`.

Next, in order (plan all three together; check feasibility first):
1. **Grader**: Docker image (JDK + JUnit console launcher), instructor `CalculatorTest`, runner that starts one locked-down container per submission (`--network none`, memory/CPU/pids limits, read-only mount, wall-clock timeout, labelled for orphan cleanup), JUnit XML report → score, grading endpoint, Docker tests in CI.
2. **SQLite** for grading runs/results (plain Spring JDBC + Flyway; single writer, single instance, document the trade-off).
3. **Metrics**: Micrometer → `/actuator/prometheus`, Prometheus + Grafana in Docker Compose. Grading counters by outcome, container duration timer, pending-approval gauge.
4. Approval UI (includes the reusable test library from issue #10), then pushing grades.

Deliberate decisions:
- **No grade push on `main`.** Classroom only allows grade changes on coursework created by the same Google Cloud project, so pushing is deferred. The push code is saved on branch `api-config` at commit `aa85d08`; don't delete that branch.
- The four mock `Calculator.java` submissions are test fixtures for the grader: correct, partially wrong, does not compile, infinite loop. Keep them that way.

## Rules for working in this repo

- **Never commit, push, or open/merge PRs.** Staging (`git add`) is allowed. Finish work by listing the files for the commit, a suggested commit message, and a PR description; the owner commits and opens PRs.
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
- Docker runs through Colima (no Docker Desktop). The socket is `~/.colima/default/docker.sock`, not `/var/run/docker.sock`; Colima must be started (`colima start`) after a reboot.
- Secrets live in `secrets/` and OAuth tokens in `tokens/`; both are gitignored and must never be read into commits or output.
