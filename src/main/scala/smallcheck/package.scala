import logic.LogicT

import scalaz.Kleisli

package object smallcheck {

  type Depth = Int
  type Argument = String
  type Reason = String

  def generate[M[_], A](f: Depth => List[A]): Series[M, A] =
    Series[M, A](
      Kleisli[({type l[a] = LogicT[M, a]})#l, Depth, A]{ d =>
        val M = LogicT.logicTMonadPlus[M]
        f(d).map(
          LogicT.logicTMonadPlus[M].point(_)
        ).foldLeft(M.empty[A])(M.plus(_, _))
      }
    )

  def suchThat[M[_], A](s: Series[M, A])(p: A => Boolean): Series[M, A] =
    s.flatMap{ x =>
      scalaz.std.boolean.pointOrEmpty[
        ({type l[a] = Series[M, a]})#l, A
      ](p(x))(x)
    }

}
