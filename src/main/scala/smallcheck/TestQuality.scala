package smallcheck

sealed abstract class TestQuality extends Product with Serializable
object TestQuality{
  val good: TestQuality = GoodTest
  val bad: TestQuality = BadTest
}
case object GoodTest extends TestQuality
case object BadTest extends TestQuality


