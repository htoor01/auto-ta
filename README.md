# AutoTA: An Auto-Grader for Java Submissions
Documentation last updated on: `(insert date)`

This project was partially inspired by AutoLab, although no code from that open-source app was used.

It was tested in a work environment with actual grades, while I was a TA at [Quakerbridge Learning Center](https://www.quaker-bridge.org/#/). It successfully processed 200+ submissions and reduced the grading time of assignments from 3-6 hours to roughly 20-30 minutes. To simulate the original environment, local JSON files and mock data is used to mimic API responses and student data.

Future updates may include expanding to support other languages. Please see [Collaboration Guidelines](README.md#Collaboration-Guidelines).

# Table of Contents

Below is a list of contents in this documentation.
- Set up guide
  - Installation prerequisites
  - How to run locally
  - Sample scripts, code snippets, and/or expected responses
- Project Overview
  - Project structure/architecture
    - Image/gif of site or architecture
  - Version history
- Collaboration guidelines

# Set-Up Steps

To be finished.

1. Ensure Git is installed on your device. See <a href="https://git-scm.com/install/mac">Git Install documentation</a>. To verify, run `git version` in your terminal.

The result should be your version number. For example, "`git version 2.52.0`". 

2. Choose a file location to pull this repository. Open that folder, establish a connection, and pull with the following steps:

- `git init` to initialize an empty git repository in your folder
- `git remote add origin https://github.com/htoor01/auto-ta.git` to add this repository as a connection
- `git pull origin main` to pull the latest version from this repository

If the third command fails, it is probably because your local branch is not named 'main'. Check to see your local branch's name. If it is master, replace 'main' with 'master'. Else, replace with whatever your branch name is.

3. 

# Project Overview

Purpose:  
To reduce feedback time for students and workload for teaching assistants (TAs). Chose against Google-native App Scripts so that I had more control over containerization (for untested student code) and CI pipelines (to learn about unit/integration tests).  

Demo video: [Link]()

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

## Project structure

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

## Version history

Version 1 of this project was developed for the [Quakerbridge Learning Center](https://www.quaker-bridge.org)'s 2026 Java Batch. It included a basic JUnit testing suite and manual pulling of submissions. Close to 200+ submissions were processed and autograded, reducing the average time for grading an assignment from 3-6 hours to roughly 20-30 minutes.  

Version 2 of this project was mostly developed after the summer. To simulate the original environment, local JSON files and mock data is used to mimic API responses and student data.

# Collaboration Guidelines

To be finished.

Future updates may include supporting other languages like Python.
