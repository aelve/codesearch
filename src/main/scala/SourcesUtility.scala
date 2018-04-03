object SourcesUtility {
  def update(downloadIndex: Boolean): Unit = {
    if (downloadIndex) {
      VersionsUtility.updateIndex()
    }

    val currentVersions = VersionsUtility.loadCurrentVersions()
    val lastVersions = VersionsUtility.updateVersions()

    println(currentVersions.size)
    println(lastVersions.size)
  }
}
