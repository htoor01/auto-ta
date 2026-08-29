# AutoTA

Purpose:
To reduce feedback time for students and workload for teaching assistants (TAs).

Workflow:
- Pull submissions from Google Classroom
- Run JUnit tests in a Docker container
- Evaluate and manually approve auto-grades in GUI
- Push final grades back to Google Classroom

Architecture:
- REST APIs (pulling submissions and grades from Google Classroom)
- JUnit tests (deterministic evaluation of Java submissions)
- Docker containers (checking untested or malicious code)
- Java (JUnit requires JRE to execute)
- SQLite (persistence, in case of app crash or work breaks)

Note: Everything was designed, built and documented by Haaris Toor, without the use of any AI support.