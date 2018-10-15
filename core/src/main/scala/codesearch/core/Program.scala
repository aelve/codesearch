package codesearch.core

import java.nio.ByteBuffer

import cats.effect._
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.foldable._
import cats.syntax.traverse._
import codesearch.core.Main.{LangRep, Params}
import codesearch.core.model.DefaultTable
import com.softwaremill.sttp.SttpBackend
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.ExecutionContext

class Program(
    langReps: Map[String, LangRep[_ <: DefaultTable]],
    logger: Logger[IO],
    http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    ec: ExecutionContext
) {

  def run(params: Params): IO[ExitCode] =
    for {
      _ <- if (params.lang == "all") {
        logger.info("Codesearch-core started for all supported languages")
      } else {
        logger.info(s"Codesearch-core started for language ${params.lang}")
      }

      _ <- initDb(params).whenA(params.initDB)
      _ <- downloadMeta(params).whenA(params.downloadMeta)
      _ <- updatePackages(params).whenA(params.updatePackages)
      _ <- buildIndex(params).whenA(params.buildIndex)

      _ <- IO(http.close())

    } yield ExitCode.Success

  object InvalidLang extends RuntimeException(s"Unsupported language")

  def findRepositories(lang: String): IO[List[LangRep[_]]] = {
    if (lang == "all") {
      IO.pure(langReps.values.toList)
    } else {
      langReps.get(lang) match {
        case Some(l) => IO.pure(List(l))
        case None    => IO.raiseError(InvalidLang)
      }
    }
  }

  def initDb(params: Params): IO[Unit] =
    for {
      languages <- findRepositories(params.lang)
      _         <- languages.traverse_(_.db.initDB)
    } yield ()

  def downloadMeta(params: Params): IO[Unit] = {
    for {
      languages <- findRepositories(params.lang)
      _         <- languages.traverse_(_.langIndex.downloadMetaInformation)
    } yield ()
  }

  def updatePackages(params: Params): IO[Unit] =
    for {
      languages <- findRepositories(params.lang)
      updated   <- languages.traverse(_.langIndex.updatePackages)
      _         <- logger.info(s"Updated: ${updated.sum}")
    } yield ()

  def buildIndex(params: Params): IO[Unit] =
    for {
      languages <- findRepositories(params.lang)
      _         <- languages.traverse_(_.langIndex.buildIndex)
      _         <- logger.info(s"${params.lang} packages successfully indexed")
    } yield ()
}
