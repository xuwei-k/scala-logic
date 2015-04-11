package logic

import scalaz._
import scalaz.std.AllInstances._
import FunctionEqual._

object LogicSpec extends ScalazCheck {

  private[this] implicit def stateTEqual[F[_], A, B](implicit F: Equal[A => F[(A, B)]]): Equal[StateT[F, A, B]] =
    F.contramap(_.apply _)

  private[this] implicit def kleisliEqual[F[_], A: Gen, B](implicit E: Equal[F[B]]): Equal[Kleisli[F, A, B]] =
    Equal[A => F[B]].contramap(_.run)

  private[this] implicit def logicGen[A: Gen]: Gen[Logic[A]] =
    Gen[List[A]].map(xs =>
      new Logic[A]{
        def apply[R](l: R)(f: A => R => R) =
          xs.foldRight(l)((a, b) => f(a)(b))
      }
    )

  private[this] implicit def logicEqual[A: Equal] =
    Equal.equal[Logic[A]] { (a, b) =>
      import scalaz.syntax.equal._
      val f1 = (l: Logic[A]) => l.observe
      val f2 = (l: Logic[A]) => l.observeAll
      (f1(a) === f1(b)) && (f2(a) === f2(b))
    }

  val testLaws =
    Properties.either(
      "Logic",
      scalaz.props.monadPlus.laws[Logic],
      scalaz.props.traverse.laws[Logic]
    )

  val testListMonadLogicLaws =
    monadLogicLaw.laws[List]

  val testStateT =
    monadLogicLaw.laws[StateT[List, Int, ?]]

  val testKleisli =
    monadLogicLaw.laws[Kleisli[List, Int, ?]]

  val testWriter =
    monadLogicLaw.laws[WriterT[List, Int, ?]]

  object instances {
    def functor[F[_] : Functor] = Functor[LogicT[F, ?]]
    def apply[F[_] : Apply] = Apply[LogicT[F, ?]]
    def plus[F[_] : Plus] = Plus[LogicT[F, ?]]
    def empty[F[_] : PlusEmpty] = PlusEmpty[LogicT[F, ?]]

    // checking absence of ambiguity
    def functor[F[_] : Monad] = Functor[LogicT[F, ?]]
    def apply[F[_] : Monad] = Apply[LogicT[F, ?]]
    def plus[F[_] : PlusEmpty] = Plus[LogicT[F, ?]]
    def empty[F[_] : MonadPlus] = PlusEmpty[LogicT[F, ?]]
  }
}
