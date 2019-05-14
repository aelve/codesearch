package codesearch.core.config

case class SourcesFilesConfig(
    testDirsNames: Set[String],
    allowedFileNames: Set[String],
    filesExtensions: FilesExtensions
)

case class FilesExtensions(
    commonExtensions: Set[String],
    sourceExtensions: Set[String],
) { def extensions: Set[String] = commonExtensions ++ sourceExtensions }
