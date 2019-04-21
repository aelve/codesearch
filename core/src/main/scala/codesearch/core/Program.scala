package codesearch.core

import cats.effect._
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.traverse._
import cats.syntax.functor._
import codesearch.core.Main.{LangRep, Params}
import codesearch.core.model.DefaultTable
import io.chrisdavenport.log4cats.Logger
import codesearch.core.util.manatki.syntax.raise._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

case class InvalidLang(lang: String) extends RuntimeException(s"Unsupported language $lang")

class Program[F[_]: Sync: ContextShift](
    langReps: Map[String, LangRep[_ <: DefaultTable]],
    logger: Logger[F]
) {

  def run(params: Params): F[ExitCode] =
    for {
      _ <- if (params.lang == "all") {
        logger.info("Codesearch-core started for all supported languages")
      } else {
        logger.info(s"Codesearch-core started for language ${params.lang}")
      }

      _ <- downloadMeta(params).whenA(params.downloadMeta)
      _ <- updatePackages(params).whenA(params.updatePackages)
      _ <- buildIndex(params).whenA(params.buildIndex)

    } yield ExitCode.Success

  def findRepositories(lang: String): F[List[LangRep[_]]] = {
    if (lang == "all") {
      langReps.values.toList.pure[F].widen
    } else {
      langReps.get(lang) match {
        case Some(l) => List(l).pure[F].widen
        case None    => InvalidLang(lang).raise
      }
    }
  }

  def downloadMeta(params: Params): F[Unit] = {
    for {
      languages <- findRepositories(params.lang)
      _         <- languages.traverse_(_.metaDownloader.download)
    } yield ()
  }

  def updatePackages(params: Params): F[Unit] =
    for {
      languages <- findRepositories(params.lang)
      updated   <- languages.traverse(_.langIndex.updatePackages(params.limitedCountPackages))
      _         <- logger.info(s"Updated: ${updated.sum}")
    } yield ()

  def buildIndex(params: Params): F[Unit] =
    for {
      languages <- findRepositories(params.lang)
      _         <- languages.traverse_(_.langIndex.buildIndex)
      _         <- logger.info(s"${params.lang} packages successfully indexed")
    } yield ()
}

object Program {
  def apply[F[_]: Sync](
      langReps: Map[String, LangRep[_ <: DefaultTable]]
  ): F[Program[F]] = Slf4jLogger.fromClass[F](getClass).map(logger => new Program(langReps, logger))
}
