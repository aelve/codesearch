# codesearch

## Deployment instructions

### codesearch

Google's `codesearch` is the underlying search engine that we use. Install
it from <https://github.com/google/codesearch>.

### Scala

The project is written in Scala. You need to install the following:

* JDK
* Scala >= 2.12.4
* `sbt` >= 1.0.2

### cargo-clone

To fetch Rust packages we use `cargo-clone`. Install Cargo, Ruby, and
`cmake`. Then you can use Cargo to install `cargo-clone`:

    $ cargo install cargo-clone

### Running the project

You can run Postgres by yourself, but it's better to use Docker. If you have
Docker installed, you can do this:

    $ make build   # Build the project
    $ make db      # Download and start Postgres (wait a bit after this step)
    $ make tables  # Create tables
    $ make server  # Run the server

If you head to <http://localhost:9000> now, you should see the project running.

Note: if you get an error at the `make tables` stage, you probably haven't
waited enough. Do `make db-kill` and start from `make db` again.
