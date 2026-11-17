# Contributing to AutoTA

Welcome. AutoTA is an open-source autograder for Java assignments from Google Classroom. There are two ways to contribute:

- **With code**: the Spring Boot service, the Docker grader, CI, or documentation. See the [README](README.md#project-overview) for how the pieces fit together.
- **With instructor tests**: JUnit tests for a new assignment, so AutoTA can grade it. See [Adding instructor tests](#adding-instructor-tests) below.

## Making a change

1. **Fork & clone** this repository.
2. **Set up:** install a JDK (25 or newer) and Docker, then build the grader image and run every test:
   ```bash
   docker build -t autota-grader:latest docker/grader
   ./mvnw verify
   ```
   Maven does not need to be installed; `./mvnw` downloads it. See also: [Set-Up Steps](README.md#set-up-steps), including running without Docker Desktop on macOS.
3. **Branch:**
   ```bash
   git checkout -b your-change-name
   ```
4. **Open a pull request** against `main` with a description of what changed and why. Keep each pull request to one concern.

The **Build and test** check must pass before a pull request can be merged. It builds the grader image and runs all tests, including the ones that start real containers. Pull requests are merged with a merge commit (no squash or rebase).

### Adding instructor tests

Instructor tests are plain JUnit files, one folder per assignment: `grader-tests/<courseWorkId>/`. AutoTA compiles them together with each student's `.java` files and runs them in a locked-down container (see [Grading sandbox](README.md#grading-sandbox)). Before opening a pull request with new tests, check that:

- They are in `grader-tests/<courseWorkId>/`, in the default package (no `package` line), like the student files they test.
- They use only JUnit Jupiter (`org.junit.jupiter.api`) and the Java standard library. The container has no network and no other libraries.
- Every test is worth the same share of the grade, so split checks into tests the way you want points split.
- Each test finishes well within 2 seconds. Slower tests are failed by the per-test timeout.
- They pass against a correct solution and fail against a wrong one. The four sample `Calculator.java` submissions in `src/main/resources/data/mock/files/` show the kinds of submissions to try.

## Repository rules

**Documentation:**
- Keep the [README](README.md) accurate: update the Set-Up Steps, the endpoint table and the Known limitations when behaviour changes. Anything not built yet is marked as planned.
- Explain *why* in the pull request description, especially for trade-offs (sandbox limits, persistence, concurrency).

**Never commit:**
- `.env` files, `secrets/` (Google OAuth client files such as `credentials.json`), `tokens/`, passwords, API keys or other credentials
- Real student data: names, emails, grades or submissions. Use placeholders (Student A, Student B, ...) in mock data and tests.
- The local database (`autota.db*`) or grading scratch files (`grader-work/`); both hold student data
- Build output (`target/`) or other large binary files

These paths are already gitignored. Double-check `git status` before committing anyway.

---

**Need help?** Open a [GitHub issue](https://github.com/htoor01/auto-ta/issues).

**Licensing:** *By contributing, you agree that your work is licensed under the [MIT License](LICENSE).* This means anyone can use, modify and share AutoTA, including in closed-source projects, as long as they keep the copyright and license notice.
