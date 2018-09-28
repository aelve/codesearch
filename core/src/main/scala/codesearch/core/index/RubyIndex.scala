package codesearch.core.index

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.file.Path

import ammonite.ops.pwd
import cats.effect.IO
import codesearch.core.db.GemDB
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.repository.Extensions._
import codesearch.core.index.repository.GemPackage
import codesearch.core.model.{GemTable, Version}
import com.softwaremill.sttp.SttpBackend
import fs2.Stream
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

class RubyIndex(
    private val ec: ExecutionContext,
    private val httpClient: SttpBackend[IO, Stream[IO, ByteBuffer]]
) extends LanguageIndex[GemTable] with GemDB {

  override protected implicit def executor: ExecutionContext                    = ec
  override protected implicit def http: SttpBackend[IO, Stream[IO, ByteBuffer]] = httpClient

  override protected val logger: Logger    = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String = ".gem_csearch_index"
  override protected val langExts: String  = ".*\\.(rb)$"

  private val GEM_INDEX_URL     = "http://rubygems.org/latest_specs.4.8.gz"
  private val GEM_INDEX_ARCHIVE = pwd / 'data / 'ruby / "ruby_index.gz"
  private val GEM_INDEX_JSON    = pwd / 'data / 'ruby / "ruby_index.json"

  private val DESERIALIZER_PATH = pwd / 'scripts / "update_index.rb"

  override protected def updateSources(name: String, version: String): Future[Int] = {
    logger.info(s"downloading package $name")
    archiveDownloadAndExtract(GemPackage(name, version))
  }

  override def downloadMetaInformation(): Unit = {
    (pwd / 'data / 'ruby).toIO.mkdirs()
    Seq("curl", "-o", GEM_INDEX_ARCHIVE.toString, GEM_INDEX_URL) !!

    Seq("/usr/bin/ruby", DESERIALIZER_PATH.toString(), GEM_INDEX_ARCHIVE.toString(), GEM_INDEX_JSON.toString()) !!
  }

  override protected def getLastVersions: Map[String, Version] = {
    val stream = new FileInputStream(GEM_INDEX_JSON.toIO)
    val obj    = Json.parse(stream).as[Seq[Seq[String]]]
    stream.close()
    obj.map { case Seq(name, ver, _) => (name, Version(ver)) }.toMap
  }

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://rubygems.org/gems/$packageName/versions/$version"

  override protected def buildFsUrl(packageName: String, version: String): Path =
    GemPackage(packageName, version).packageDir
}

object RubyIndex {
  def apply()(
      implicit ec: ExecutionContext,
      http: SttpBackend[IO, Stream[IO, ByteBuffer]]
  ) = new RubyIndex(ec, http)
}
