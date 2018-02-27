import ammonite.ops._

object Main {

  case class Config(updatePackages: Boolean = false,
                    sourcesDir: FilePath = pwd / 'sources
                   )

  val parser = new scopt.OptionParser[Config]("main") {
    head("\nCodesearch command line interface\n\n")

    opt[String]('u', "update-packages") action { (v, c) =>
      c.copy(updatePackages = true)
    } text "update package-index"
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) foreach { c =>
      if (c.updatePackages) {
        Updater.update()
      }
    }
  }
}
