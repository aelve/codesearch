package codesearch.core.config

import java.net.URI
import java.nio.file.{Path, Paths}

import pureconfig.ConfigReader

import scala.util.Try

trait ConfigReaders {

  implicit def uriConfigReader: ConfigReader[URI] = ConfigReader.fromNonEmptyStringTry { rawString =>
    Try(new URI(rawString))
  }

  implicit def pathConfigReader: ConfigReader[Path] = ConfigReader.fromNonEmptyStringTry { rawString =>
    Try(Paths.get(rawString))
  }
}
