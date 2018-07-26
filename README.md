Subgraph Matching by Postponing Cartesian Products using NEO4J Graph DB and Protiens dataset.

Implemented the algorithm in paper Efficient Subgraph Matching by Postponing Cartesian Products by Fei Biy, Lijun Changy, Xuemin Liny, Lu Qinz, Wenjie Zhangy.


Designed and implemented query decomposition described in Section 3

Designed and implemented CPI using Neo4j along with root node selection described in Appendix A.6.

Using the ground truth in Proteins, decomposed the different query graphs, computed CPIs for the decompositions, and provided a Java program that automatically checks if all the expected solutions are contained in the computed CPIs



This is a JAVA Gradle project.
The source code is under the following path
`./src/main/java`

The configuration file to connect to database is located in `./src/main/resources` folder

The default configuration is as follows

| Key         | Value       |
| -----------: |:---------------|
| neo4jdbpath | /pathtographdb/graph.db   |
|  proteinsfolderpath | /pathtotarget/target |
| proteinsquerypath | /pathtoquery/query        |
|proteingroundtruthpath |/pathtogroundtruth/ground_truth |


The `./src/test/java` contains test cases that uses `JUNIT`

The basic framework is in the file `./src/main/java/CPI`
The Query decompositions are in the `./src/main/java/QueryGraph` class.

The main `Program.java` has all the options to run the program.
