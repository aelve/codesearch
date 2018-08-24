package codesearch.core.index.repository
import java.io.File
import java.net.URI
import java.nio.file.Path

import org.apache.commons.io.FilenameUtils.getExtension

import sys.process._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object SourceRepository {
  implicit def packageDownloader[A <: SourcePackage]: Download[A] = new Download[A] {
    override def downloadSources(pack: A): Future[Int] = {
      for {
        archive   <- download(pack.url, pack.fsPath)
        directory <- unzip(archive)
        countArch <- deleteExcessFiles(archive, pack.extentions)
        countDir  <- deleteExcessFiles(directory, pack.extentions)
      } yield directory
    }.map(_ => 1).recover { case _ => 0 }

  }

  def download(from: URI, to: Path): Future[File] = {
    Future {
      Seq("curl", "-o", to.toString, from.getPath) !!;
      to
    }
    ???
  }

  def unzip(file: File): Future[File] = {
    ???
  }

  def deleteExcessFiles(directory: File, allowedExtentions: Set[String]): Future[Int] = Future {
    def filterFiles(all: List[File], excess: List[File] = Nil): List[File] = all match {
      case Nil                                => excess
      case file :: others if file.isDirectory => filterFiles(others ++ file.listFiles(), excess)
      case file :: others =>
        if (allowedExtentions.contains(getExtension(file.getName)))
          filterFiles(others, excess)
        else filterFiles(others, file :: excess)
    }
    filterFiles(List(directory)).map(_.delete()).count(identity)
  }
}
