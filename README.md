This is a JAVA Gradle project.
The source code is under the following path
`./src/main/java`

The configuration file to connect to database is located in `./src/main/resources` folder

The default configuration is as follows

| Key         | Value       |
| -----------: |:---------------|
| neo4jdbpath | /home/vishal/dev/proteins/databases/graph.db   |
|  proteinsfolderpath | /home/vishal/dev/courses/CSCI729/HW4/Proteins/target |
| proteinsquerypath | /home/vishal/dev/courses/CSCI729/HW4/Proteins/query        |
|proteingroundtruthpath |/home/vishal/dev/courses/CSCI729/HW4/Proteins/ground_truth |


The `./src/test/java` contains test cases that uses `JUNIT`

The basic framework is in the file `./src/main/java/CPI`
The Query decompositions are in the `./src/main/java/QueryGraph` class.

The main `Program.java` has all the options to run the program.