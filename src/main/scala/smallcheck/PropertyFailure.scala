package smallcheck


/*
data PropertyFailure
  = NotExist
  | AtLeastTwo [Argument] PropertySuccess [Argument] PropertySuccess
  | CounterExample [Argument] PropertyFailure
  | PropertyFalse (Maybe Reason)

 */

sealed abstract class PropertyFailure extends Product with Serializable
object PropertyFailure{
  case object NotExit extends PropertyFailure
  final case class AtLeastTwo(arg1: List[Argument], s1: PropertySuccess, arg2: List[Argument], s2: PropertySuccess) extends PropertyFailure
  final case class CounterExample(args: List[Argument], f: PropertyFailure) extends PropertyFailure
  final case class PropertyFalse(reason: Option[Reason]) extends PropertyFailure

}
