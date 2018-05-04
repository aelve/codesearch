package codesearch.core.model

case class Version(verString: String) extends Ordered[Version] {
  import scala.math.Ordered.orderingToOrdered

  val version: Iterable[Long] = ("""\d+""".r findAllIn verString).toSeq.map(_.toLong)

  override def compare(that: Version): Int = this.version compare that.version
}

object Version {
  def less(ver1: String, ver2: String): Boolean = Version(ver1) < Version(ver2)
}

