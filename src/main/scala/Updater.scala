import ammonite.ops._

object Updater {
  private val VERSIONS_FILE = pwd / 'data / "package_versions.json"

  def update(downloadIndex: Boolean): Unit = {
    if (downloadIndex) {
      SourcesUtility.updateIndex()
    }

    val currentVersions = SourcesUtility.loadCurrentVersions()
    val lastVersions = SourcesUtility.updateVersions()

    println(currentVersions.size)
    println(lastVersions.size)
  }
}
