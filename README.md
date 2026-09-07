# AutoTA Overview
(Project in progress)

Purpose: To reduce feedback time for students and workload for teaching assistants (TAs).  
Demo video: [Link]

Workflow:
- Pull submissions from Google Classroom
- Run JUnit tests in a Docker container
- Evaluate and manually approve auto-grades in GUI
- Push final grades back to Google Classroom

Concepts learned: 
- RESTful APIs
- Docker containers
- Unit testing
- Data persistence
- Dependency management
- Version control

Architecture:
- REST APIs (pulling submissions and grades from Google Classroom)
- JUnit tests (deterministic evaluation of Java submissions)
- Docker containers (checking untested or malicious code)
- Java (JUnit requires JRE to execute)
- SQLite (persistence, in case of app crash or work breaks)
- Maven (handle dependencies conveniently)

File structure:
- src/
   - main/
     - java/
       - App.java
     - resources/
       - data/
         - empty.txt
  - test/
- .gitignore (preserve secrets)
- pom.xml (Maven dependencies)
- README.md (this file)

Notes: 
- This project was developed for the Quakerbridge Learning Center's 2026 Java Batch. To simulate this, local JSON files and mock data is used to mimic API responses and student data.
- To avoid leaking sensitive student data, all implementation and documentation was done by Haaris Toor (without AI support).

# Set-Up Steps

1. Ensure Git is installed on your device. See <a href="https://git-scm.com/install/mac">Git Install documentation</a>. To verify, run `git version` in your terminal.

The result should be your version number. For example, "`git version 2.52.0`". 

2. Choose a file location to pull this repository. Open that folder, establish a connection, and pull with the following steps:

- `git init` to initialize an empty git repository in your folder
- `git remote add origin https://github.com/htoor01/auto-ta.git` to add this repository as a connection
- `git pull origin main` to pull the latest version from this repository

If the third command fails, it is probably because your local branch is not named 'main'. Check to see your local branch's name. If it is master, replace 'main' with 'master'. Else, replace with whatever your branch name is.

3. 
