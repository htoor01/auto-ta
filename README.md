# AutoTA

Purpose: To reduce feedback time for students and workload for teaching assistants (TAs).  
Demo video: [Link]

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

Notes: 
- A variation of this project was developed for the Summer 2026 Java QBLC Batch. It processed 200+ submissions, enabling us to challenge our students. This contributed to a 91% pass rate on the WWP district exam.
- Planning was done with the support of AI. All implementation and documentation was done by Haaris Toor.

# Set Up Steps

1. Ensure Git is installed on your device.
