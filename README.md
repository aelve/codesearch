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

### Running the project

You can run Postgres by yourself, but it's better to use Docker. If you have
Docker installed, you can do this:

    $ make build   # Build the project
    $ make db      # Download and start Postgres (wait a bit after this step)
    $ make tables  # Create tables
    $ make serve   # Run the server

If you head to <http://localhost:9000> now, you should see the project running.

Note: if you get an error at the `make tables` stage, you probably haven't
waited enough. Do `make db-kill` and start from `make db` again.

### Indexing packages

After the previous step the project is running, but the indices are empty.
To download some packages, do this:

    $ make download-haskell

At first it will download the Hackage index (taking about 30 seconds), then
it will start downloading packages. You likely don't want to download the
whole Hackage, so stop it after a minute or less.

Next, index the packages:

    $ make index-haskell

After that you should be able to visit <http://localhost:9000/haskell> and
play with some queries (e.g. `module` should bring up a lot of results).

The full list of supported languages can be found in the `Makefile`.

### Build on MacOS

MacOS has `make` version 3.81 by default. In order to avoid error on running
`make` you have to install version 3.82 or above.

For `brew` just run `brew install homebrew/core/make` ([source](https://apple.stackexchange.com/questions/261918/how-to-upgrade-gnu-make-in-os-x-el-capitan)).
Last `make` will be installed as `gmake`, so use `gmake whatever` further. 
