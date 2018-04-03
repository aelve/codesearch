import sys.process._
import java.io._
import java.net.URL
import java.nio.ByteBuffer

import org.apache.commons.io.IOUtils
import boopickle.Default._
import ammonite.ops.{Path, pwd}
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}

import scala.math.Ordered.orderingToOrdered

case class Version(verString: String) extends Ordered[Version] {
  val version: Iterable[Int] = verString.split('.').map(_.toInt)

  override def compare(that: Version): Int = this.version compare that.version
}

object VersionsUtility {

  val INDEX_LINK: String = "http://hackage.haskell.org/packages/index.tar.gz"
  val INDEX_SOURCE_GZ: Path = pwd / 'data / "index.tar.gz"
  val INDEX_SOURCE_DIR: Path = pwd / 'data / 'index / "index"

  val VERSIONS_FILE: Path = pwd / 'data / "versions.obj"

  def updateIndex(): Unit = {

    new URL(INDEX_LINK) #> new File(INDEX_SOURCE_GZ.toString()) !!

    val archive = new File(INDEX_SOURCE_GZ.toString)
    val destination = new File(INDEX_SOURCE_DIR.toString)

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    archiver.extract(archive, destination)
  }

  def updateVersions(): Map[String, Version] = {

    val indexDir = new File(VersionsUtility.INDEX_SOURCE_DIR.toString)
    val packageNames = indexDir.listFiles.filter(_.isDirectory)

    val lastVersions = packageNames.flatMap(packagePath =>
      packagePath.listFiles.filter(_.isDirectory).map(versionPath =>
        (packagePath.getName, Version(versionPath.getName))
      )
    ).groupBy(_._1).mapValues(_.map(_._2).max)

    saveLastVersions(lastVersions)

    lastVersions

  }


  def loadCurrentVersions(): Map[String, Version] = {
    try {
      val fis = new FileInputStream(VERSIONS_FILE.toIO)

      val result = Unpickle[Map[String, Version]]
        .fromBytes(ByteBuffer.wrap(IOUtils.toByteArray(fis)))
      fis.close()

      result
    }
    catch {
      case e: Exception =>
        println(e.getMessage)
        Map()
    }
  }

  def saveLastVersions(lastVersions: Map[String, Version]): Unit = {
    val buf = Pickle.intoBytes(lastVersions)

    val oos = new FileOutputStream(VERSIONS_FILE.toIO)
    oos.write(buf.array())
    oos.close()
  }
}
