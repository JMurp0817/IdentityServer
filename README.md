## Project #2 <br>
## Project Title: Identity Server (Part 1) <br>
## Project Members: Justin Fernandez, Stefan Diklic, Joseph Murphy <br>
## Course Number: 455 <br>
## Course Title: Distributed Systems <br>
## Spring 2022 <br>

### File Folder/Manifest

Our main program source files are found in `src/main/java` 

`Queries.java` holds the RMI functions that are to be implemented in `idServer.java` and used by the `idClient.java` class through argument parsing with the Apache Commons CLI package. 

`User.java` holds the general information that a login user would typically have to have to keep track of within the system.

Some miscellaneous files are `pom.xml` which is used by the Maven build system, `mysecurity.policy` which is used for the various security features in the project, and a `.gitignore` that our team used to help manage some extraneous files due to differences in development environments between members of the team while working and collaborating on the project over Git and Github.

While running, a new file called `registry.backup` at the root level directory is created on server close or after the server has been running on intervals of 5 minutes. This holds the user data that will be loaded on server start for future server start-ups.

### Building/Running

First, use `$ mvn package` in the command-line from the root directory of the project.

Then, still from the root directory, use the following command to run the server:

`$ java -cp target/p2-1.0-jar-with-dependencies.jar -Djava.security.policy=./mysecurity.policy IdServer [--numport <port#>] [--verbose]`

To run the client, 

`$ java -cp target/p2-1.0-jar-with-dependencies.jar -Djava.security.policy=./mysecurity.policy IdClient [--server <serverhost>] [--numport <port#>] <query>`

With the different queries being:
* `--create <loginname> [<real name>] [--password <password>]`
* `--lookup <loginname>`
* `--reverse-lookup <UUID>`
* `--modify <oldloginname> <newloginname> [--password <password>]`
* `--delete <loginname> [--password <password>]`
* `--get users|uuids|all`

### Testing

Our group primarily used an "eyetest" testing method since many of the client tests were "one-shot" in the sense that due to the way the assignment and project was set-up, each client connections would be one query, which indicated that we could develop and test the queries relatively independent of each other. We would walk-through each query testing various argument inputs and whether or not some certain special characters should be supported normally by the system. We would understand that a certain query is funcitonal and working by having its results meet the criteria set by the project document.

### Observations/Reflection

The development process for this project was a little rocky as this project contained some concepts that we initially struggled with wrapping our heads around and ramping up our understanding to get to a practical able-to-write code level. 

Our troubles were further exacerbated when we had trouble coordinating times to meet-up and work as coursework in our other classes quickly escalated as well and we all had prior engagements for the week of Spring break, we had worked on the project little by little prior to this point, but we were all feeling stressed and had to ask for a couple days due date extension.

We all were heavily involved with the design questions and structure of our distributed system as the project document and rubric were very loose in the sense of wanting a particular implementation or design, so we all played parts in that discussion. When it came to coding, we adopted the method from project 1 in which we would primarily have one person actually coding while the others would watch/work side-by-side or screen-share over Zoom and we felt like that helped sharpen our code and make our overall codebase cleaner as we would usually write once without any major refactoring headaches with "spaghetti code".

For our next project, our thoughts/goals are probably going be to ask more questions early on especially since we kind of struggled on our own with each other rather than trying to reach out to the Professor or peers on Discord or Piazza. We all felt that our questions were relatively minor or something that was covered in class that we just couldn't "quite" get right and so we banged our heads against a wall. This included some questions about implementation on the project document especially surrounding the Server implementation and was where most of our structure discussion would center around.

### Video Testing 

Link: [CS 455 Project 2 (Part 1)](https://www.youtube.com/watch?v=onGZwOXG7VI) <br>
Raw: https://www.youtube.com/watch?v=onGZwOXG7VI
