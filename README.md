# codesearch
## Deployment instructions
1. Install `csearch/cindex` from github.com/google/codesearch
2. Install `JDK`.
3. Install `Postgres`.
4. Specify database name, user, and password for postgres in [the application.conf](https://github.com/aelve/codesearch/blob/master/core/src/main/resources/application.conf)
5. Install `scala` with version >= 2.12.4
6. Install `sbt` with version >= 1.0.2
7. Install `cargo`, `ruby`
8. Install `cmake`
9. Install `cargo-clone`
10. `sbt web-server/assembly` from the root of the project should create `codesearch-server.jar` which could be run using `java -jar codesearch-server.jar` or added as a daemon.
11. `sbt web-server/run` from the root of the project should run server in the debug mode.
12. `sbt core/assembly` from the root of the project will create codesearch-core.jar which can download index, update it, also it can add it to the google's codesearch index.
