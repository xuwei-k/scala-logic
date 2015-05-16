package smallcheck

sealed abstract class Quantification extends Product with Serializable
object Quantification{
  case object Forall extends Quantification
  case object Exists extends Quantification
  case object ExistsUnique extends Quantification
}
