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
- This project was developed for the Quakerbridge Learning Center's 2026 Java Batch. To simulate this, local JSON files and mock data is used to mimic Classroom API responses and student data in the SQLite database.
- To avoid leaking sensitive student data, all implementation and documentation was done by Haaris Toor (without AI support).

# Set Up Steps

1. Ensure Git is installed on your device.
