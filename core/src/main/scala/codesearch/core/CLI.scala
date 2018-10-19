package codesearch.core

import cats.ApplicativeError
import cats.effect.IO
import codesearch.core.Main.Params

object CLI {

  private val parser = new scopt.OptionParser[Params]("main") {
    head("\nCodesearch command line interface\n\n")

    opt[Unit]('u', "update-packages") action { (_, c) =>
      c.copy(updatePackages = true)
    } text "update package-sources"

    opt[Unit]('d', "download-meta") action { (_, c) =>
      c.copy(downloadMeta = true)
    } text "update package meta information"

    opt[Unit]('i', "init-database") action { (_, c) =>
      c.copy(initDB = true)
    } text "create tables for database"

    opt[Unit]('b', "build-index") action { (_, c) =>
      c.copy(buildIndex = true)
    } text "build index with only latest packages"

    opt[String]('l', "lang").action { (l, c) =>
      c.copy(lang = l)
    }
  }

  def params(args: Seq[String]): IO[Params] = {
    ApplicativeError.liftFromOption[IO](parser.parse(args, Params()), InvalidArgs)
  }

  object InvalidArgs extends RuntimeException("Can't parse arguments")

}