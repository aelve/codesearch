package codesearch.core.index.details
import java.io.File
import java.nio.file.Paths

import codesearch.core.index.repository.DownloadException
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.{Uri, _}
import io.circe.{Decoder, HCursor, Json}
import io.circe.parser.decode
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

final case class PackageKey(key: String) extends AnyVal
final case class NpmPackagesKeys(rows: List[PackageKey])

final case class NpmPackageDetails(name: String, tag: LatestTag)
final case class LatestTag(latest: String) extends AnyVal

private[index] final class NpmDetails(implicit ec: ExecutionContext, http: SttpBackend[Future, Nothing]) {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val FsIndexPath        = Paths.get("./data/js/names.json")
  private val NpmRegistryUrl     = uri"https://replicate.npmjs.com/"
  private val NpmPackagesKeysUrl = uri"$NpmRegistryUrl/_all_docs"

  private implicit val npmPackageDetailsDecoder: Decoder[NpmPackageDetails] = (c: HCursor) => {
    for {
      name <- c.get[String]("name")
      tag  <- c.get[LatestTag]("dist-tag")
    } yield NpmPackageDetails(name, tag)
  }

  def index: Future[File] = {
    for {
      npmKeys        <- download[NpmPackagesKeys](NpmPackagesKeysUrl)
      packageDetails <- traverse(npmKeys)
      indexFile      <- saveIndex(packageDetails)
    } yield indexFile
  }.andThen { case Failure(ex) => logger.error(ex.getMessage) }

  private def download[A: Decoder](url: Uri): Future[A] = {
    sttp
      .get(url)
      .response(asString)
      .send()
      .flatMap(_.body.fold(
        error => Future.failed(DownloadException(error)),
        result =>
          decode[A](result) match {
            case Left(error)  => Future.failed(error)
            case Right(value) => Future.successful(value)
        }
      ))
  }

  private def traverse(npmKeys: NpmPackagesKeys): Future[List[NpmPackageDetails]] = Future.sequence(
    npmKeys.rows.map(row => download[NpmPackageDetails](npmPackageDetailsUrl(row.key)))
  )

  private def npmPackageDetailsUrl(packageName: String) = uri"$NpmRegistryUrl/$packageName"

  private def saveIndex(packageDetails: List[NpmPackageDetails]) = Future {
    val indexFile   = FsIndexPath.toFile
    val jsonToWrite = packageDetails.asJson.noSpaces.getBytes
    FileUtils.writeByteArrayToFile(indexFile, jsonToWrite)
    indexFile
  }

}

object NpmDetails {
  def apply()(
      implicit ec: ExecutionContext,
      http: SttpBackend[Future, Nothing]
  ) = new NpmDetails()
}
