package codesearch.core.index.details
import java.nio.file.Paths

import cats.effect.IO
import codesearch.core.index.repository.DownloadException
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.{Uri, _}
import fs2.{Chunk, Pipe, Sink, Stream}
import fs2.io._
import io.circe.{Decoder, HCursor}
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

final case class PackageKey(key: String) extends AnyVal
final case class NpmPackagesKeys(rows: List[PackageKey])

final case class NpmPackageDetails(name: String, version: LatestTag)
final case class LatestTag(latest: String) extends AnyVal

private[index] final class NpmDetails(implicit ec: ExecutionContext, http: SttpBackend[Future, Nothing]) {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val FsIndexPath        = Paths.get("./index/npm/names.json")
  private val NpmRegistryUrl     = uri"https://replicate.npmjs.com/"
  private val NpmPackagesKeysUrl = uri"$NpmRegistryUrl/_all_docs"

  private implicit val npmPackageDetailsDecoder: Decoder[NpmPackageDetails] = (c: HCursor) => {
    for {
      name <- c.get[String]("name")
      tag  <- c.get[LatestTag]("dist-tag")
    } yield NpmPackageDetails(name, tag)
  }

  def index: Stream[Future, Unit] =
    allPackages
      .evalMap(latestVersion)
      .through(packageToString)
      .to(toFile)

  private def allPackages: Stream[Future, NpmPackagesKeys] =
    Stream.eval(download[NpmPackagesKeys](NpmPackagesKeysUrl))

  private def download[A: Decoder](url: Uri): Future[A] = {
    sttp
      .get(url)
      .response(asString)
      .send
      .flatMap(_.body.fold(
        error => Future.failed(DownloadException(error)),
        result =>
          decode[A](result) match {
            case Left(error)  => Future.failed(error)
            case Right(value) => Future.successful(value)
        }
      ))
  }

  private def latestVersion(keys: NpmPackagesKeys): Future[NpmPackageDetails] = {}

  private def packageToString[F[_]]: Pipe[Future, NpmPackageDetails, Byte] = { in =>
    in.flatMap(pack => Stream.chunk(Chunk.array(pack.asJson.noSpaces.getBytes)))
  }

  private def toFile: Sink[Future, Byte] = file.writeAllAsync(FsIndexPath)

}

object NpmDetails {
  def apply()(
      implicit ec: ExecutionContext,
      http: SttpBackend[Future, Nothing]
  ) = new NpmDetails()
}
