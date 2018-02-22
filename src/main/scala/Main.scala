
object Main {

  case class Config(updatePackages: Boolean = false)

  val parser = new scopt.OptionParser[Config]("main") {
    head("\nCodesearch command line interface\n\n")

    opt[String]('u', "update-packages") action { (v, c) =>
      c.copy(updatePackages = true)
    } text "update package-index"
  }

  def main(args: Array[String]): Unit = {
  }
}
