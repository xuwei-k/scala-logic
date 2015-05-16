package smallcheck

sealed abstract class PropertySuccess extends Product with Serializable
object PropertySuccess{
  final case class Exist(args: List[Argument], s: PropertySuccess) extends PropertySuccess
  final case class ExistUnique(args: List[Argument], s: PropertySuccess) extends PropertySuccess
  final case class PropertyTrue(reason: Option[Reason]) extends PropertySuccess
  final case class Vacuously(a: PropertyFailure) extends PropertySuccess
}
