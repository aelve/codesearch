# codesearch

## Deployment instructions

### codesearch indexer

Google's `codesearch` is the underlying search engine that we use (with some
modifications). Install it from <https://github.com/aelve/codesearch-engine>.

<details><summary>macOS instructions</summary>

1. Install Go: `brew install go`
2. Add `$HOME/go/bin` to the PATH
3. Download and build `codesearch` (should take 5 to 10 seconds):
   `go get github.com/aelve/codesearch-engine/cmd/...`

</details>

### Scala

The project is written in Scala. You need to install the following:

* JDK 8 (later versions will not work)
* Scala >= 2.12.4
* sbt >= 1.0.2

### Running the project

You can run Postgres by yourself, but it's better to use Docker. If you have
Docker installed, you can do this:

    $ make build   # Build the project
    $ make db      # Download and start Postgres (wait a bit after this step)
    $ make tables  # Create tables
    $ make serve   # Run the server

If you head to <http://localhost:9000> now, you should see the project
running. The port can be changed:

    $ make serve port=7000

Note: if you get an error at the `make tables` stage, you probably haven't
waited enough. Do `make db-kill` and start from `make db` again.

### Indexing packages

After the previous step the project is running, but the indices are empty.
Download the package index for Haskell (should take about 30 seconds):

    $ make download-haskell

Download some packages. You likely don't want to download the whole Hackage,
so interrupt it (Ctrl+C) after a minute or less:

    $ make update-haskell

Next, index the code contained in the packages:

    $ make index-haskell

After that you should be able to visit <http://localhost:9000/haskell> and
play with some queries (e.g. `module` should bring up a lot of results).

The full list of supported languages can be found in the `Makefile`.
