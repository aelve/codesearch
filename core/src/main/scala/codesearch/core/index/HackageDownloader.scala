package codesearch.core.index
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.io.File

import sys.process._
import ammonite.ops.pwd

class HackageDownloader(hp: HackagePackage)
    extends Download[HackagePackage] with Extract[HackagePackage] with SourcesDownloader {

  override val repoUrl: String = "https://hackage.haskell.org/package/"

  override def downloadSources: Future[File] = {
    Future {
      val packageURL = s"$packageURL/${hp.name}-${hp.version}/${hp.name}-${hp.version}.tar.gz"
      val file       = (pwd / 'data / hp.repository / hp.name / hp.version / s"${hp.version}.tar.gz").toIO
      Seq("curl", "-o", file.getCanonicalPath, packageURL) !!;
      file
    }
  }
  override def extractSources: Future[File] = {
    downloadSources.map(f => {
      val to = (pwd / 'data / hp.repository / hp.name / hp.version / hp.version).toIO
      Seq("tar", "-xvf", f.getCanonicalPath, "-C", to.getCanonicalPath) !!;
      to
    })
  }
}
