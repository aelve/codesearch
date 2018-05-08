# codesearch

## Deployment instructions

### codesearch

Google's `codesearch` is the underlying search engine that we use. Install it from <https://github.com/google/codesearch>.

### Scala

The project is written in Scala. You need to install the following:

* JDK
* Scala >= 2.12.4
* `sbt` >= 1.0.2

### PostgreSQL

Install PostgreSQL. Specify database name, user, and password in [application.conf](https://github.com/aelve/codesearch/blob/master/core/src/main/resources/application.conf).

### cargo-clone

To fetch Rust packages we use `cargo-clone`. Install Cargo, Ruby, and `cmake`. Then you can use Cargo to install `cargo-clone`:

    $ cargo install cargo-clone

### Running the project

* `sbt web-server/assembly` from the root of the project should create `codesearch-server.jar` which could be run using `java -jar codesearch-server.jar` or added as a daemon.

* `sbt web-server/run` from the root of the project should run server in the debug mode.

* `sbt core/assembly` from the root of the project will create `codesearch-core.jar` which can download index, update it, also it can add it to the `codesearch`'s index.
