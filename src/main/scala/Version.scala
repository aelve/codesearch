import scala.math.Ordered.orderingToOrdered

case class Version(verString: String) extends Ordered[Version] {
  val version: Iterable[Int] = verString.split('.').map(_.toInt)

  override def compare(that: Version): Int = this.version compare that.version
}
