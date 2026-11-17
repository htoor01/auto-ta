<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/autota-logo-dark.svg">
  <img src="docs/assets/autota-logo-light.svg" alt="AutoTA" width="316">
</picture>

# AutoTA: An Auto-Grader for Java Submissions
[![CI](https://github.com/htoor01/auto-ta/actions/workflows/ci.yml/badge.svg)](https://github.com/htoor01/auto-ta/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

AutoTA pulls Java submissions from Google Classroom, runs the instructor's JUnit tests against each one in a locked-down Docker container, and suggests a grade for the instructor to approve.

> **Status:** Version 2 is in progress. Pulling submissions, sandboxed grading and storing results are built; metrics and an approval UI are next. See the [roadmap](docs/architecture.md#roadmap).  
> **Additionally:** Version 2 is the public-facing version of a similar tool used at the center. This repository has diverged from the center's internal tool, which continues to be developed and maintained separate from this repository as per the center's needs. This repository includes local JSON files and mock data to simulate a generic classroom environment.

## What it does

- ✅ **Pulls** courses, assignments and submissions from Google Classroom (or from built-in mock data)
- ✅ **Grades** each submission in its own throwaway container: no network, read-only, memory/CPU/process limits, timeouts
- ✅ **Stores** grading runs and results in SQLite; grading runs in the background and survives restarts
- 🚧 **Approve** suggested grades in a web UI; for now the instructor enters approved grades in Classroom

## Quick start

Needs a JDK (25+) and Docker. On macOS, [Colima](https://github.com/abiosoft/colima) works instead of Docker Desktop.

```bash
git clone https://github.com/htoor01/auto-ta.git && cd auto-ta
docker build -t autota-grader:latest docker/grader   # the image student code runs in
./mvnw spring-boot:run                               # starts with mock data, no Google account needed
curl -X POST localhost:8080/api/courses/course-java-2026/coursework/cw-calculator/grade
```

The last command returns `202 Accepted` with a grading run; follow its `Location` (`GET /api/grading-runs/{id}`) to see the results. For the sample assignment:

| Sample submission | Status | Suggested grade |
|---|---|---|
| Correct | `GRADED`, 7/7 | 100 |
| Partially wrong | `GRADED`, 5/7 | 71.43 |
| Does not compile | `COMPILE_ERROR` | 0 (compiler output included) |
| Infinite loop | `GRADED`, 6/7 (only the looping test times out) | 85.71 |
| Nothing attached | `NO_SUBMISSION` | none |

Grades are only suggestions; nothing is sent to Classroom.

## Documentation

- **[Setup guide](docs/setup.md)**: installation, every API endpoint with sample responses, configuration, connecting real Google Classroom, troubleshooting
- **[Architecture](docs/architecture.md)**: how grading works, the sandbox, background jobs and persistence, crash recovery, known limitations and roadmap

## Background

This project was partially inspired by AutoLab, although no code from that open-source app was used.

Version 1 was a script I used as a TA at [Quakerbridge Learning Center](https://www.quaker-bridge.org/#/) for its 2026 Java batch. It processed 200+ submissions and cut the time to grade an assignment from 3-6 hours to roughly 20-30 minutes. Version 2 (this repository) rebuilds it as a Spring Boot service with sandboxed grading, automated tests and CI. I chose a standalone service over Google Apps Script for control over containerization (for untrusted student code) and CI pipelines.

## Contributing

Contributions are welcome: code, documentation, or instructor tests for new assignments. See [CONTRIBUTING.md](CONTRIBUTING.md). Future updates may include supporting other languages like Python.

## License

AutoTA is released under the [MIT License](LICENSE).
