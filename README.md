# codesearch
## Deployment instructions
1. Install csearch/cindex from github.com/google/codesearch
2. Install JDK.
3. Install scala with version >= 2.12.4
4. Install sbt with version >= 1.0.2
5. `sbt web-server/assembly` from the root of the project should create `codesearch-server.jar` which could be run using `java -jar codesearch-server.jar` or added as a daemon.
6. `sbt web-server/run` from the root of the project should run server in the debug mode.
7. `sbt core/assembly` from the root of the project will create codesearch-core.jar which can download index, update it, also it can add it to the google's codesearch index.
