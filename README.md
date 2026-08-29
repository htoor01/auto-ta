# AutoTA

Purpose:
To reduce feedback time for students and workload for teaching assistants (TAs).

Plan:
- Pull submissions from Google Classroom
- Run JUnit tests in a Docker container
- Evaluate and manually approve auto-grades in GUI
- Push final grades back to Google Classroom

Architecture:
- Submissions and grades via Google Classroom (REST API)
- JUnit tests (deterministic, LLM not needed)
- Docker containers (checking potential accidentally malicious code)
- Java (JUnit requires JRE to execute)
