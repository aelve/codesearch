package codesearch.core

import cats.effect._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.Main.{LangRep, Params}
import codesearch.core.config.{Config, DatabaseConfig}
import codesearch.core.db.DataSource
import codesearch.core.model.DefaultTable
import codesearch.core.util.manatki.Raise
import codesearch.core.util.manatki.syntax.raise._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

case class InvalidLang(lang: String) extends RuntimeException(s"Unsupported language $lang")

class Program[F[_]: Async: ContextShift: Raise[?[_], InvalidLang]](
    langReps: Map[String, LangRep[_ <: DefaultTable]],
    logger: Logger[F]
) {

  def run(params: Params, config: Config): F[ExitCode] = {
    for {
      _ <- if (params.lang == "all") {
        logger.info("Codesearch-core started for all supported languages")
      } else {
        logger.info(s"Codesearch-core started for language ${params.lang}")
      }

      _ <- migrate(config.db)
      _ <- downloadMeta(params).whenA(params.downloadMeta)
      _ <- updatePackages(params).whenA(params.updatePackages)
      _ <- buildIndex(params).whenA(params.buildIndex)

    } yield ExitCode.Success
  }

  private def migrate(config: DatabaseConfig): F[Unit] = {
    DataSource.transactor[F](config).use { xa =>
      for { _ <- xa.configure(DataSource.migrate[F]) } yield ()
    }
  }

  private def findRepositories(lang: String): F[List[LangRep[_]]] = {
    if (lang == "all")
      langReps.values.toList.pure[F].widen
    else
      langReps.get(lang) match {
        case Some(l) => List(l).pure[F].widen
        case None    => InvalidLang(lang).raise
      }
  }

  private def downloadMeta(params: Params): F[Unit] = {
    for {
      languages <- findRepositories(params.lang)
      //      _         <- languages.traverse_(_.langIndex.downloadMetaInformation)
    } yield ()
  }

  private def updatePackages(params: Params): F[Unit] =
    for {
      languages <- findRepositories(params.lang)
      //      updated   <- languages.traverse(_.langIndex.updatePackages)
      //      _         <- logger.info(s"Updated: ${updated.sum}")
    } yield ()

  private def buildIndex(params: Params): F[Unit] =
    for {
      languages <- findRepositories(params.lang)
      //      _         <- languages.traverse_(_.langIndex.buildIndex)
      _ <- logger.info(s"${params.lang} packages successfully indexed")
    } yield ()
}

object Program {
  def apply[F[_]: Async: ContextShift](langReps: Map[String, LangRep[_ <: DefaultTable]]): F[Program[F]] =
    Slf4jLogger.fromClass[F](getClass).map(logger => new Program(langReps, logger))
}
