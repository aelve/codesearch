package codesearch.core.meta.downloader

import cats.effect.{ContextShift, Sync}
import cats.syntax.functor._
import codesearch.core.config.JavaScriptConfig
import codesearch.core.db.repository.PackageIndexDbRepository
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.parser.JavaScriptIndexParser
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object NpmIndexDownloader {
  def apply[F[_]: Sync: ContextShift](
      config: JavaScriptConfig,
      downloader: Downloader[F],
      xa: Transactor[F]
  ): F[RepositoryIndexDownloader[F]] = {
    for {
      logger <- Slf4jLogger.create
    } yield
      new ByteStreamIndexDownloader(
        config,
        downloader,
        PackageIndexDbRepository(xa),
        JavaScriptIndexParser(config),
        logger
      )
  }
}
