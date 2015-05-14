package smallcheck

import logic._
import scalaz._

final case class Series[M[_], A](run: Kleisli[({type l[a] = LogicT[M, a]})#l, Depth, A])

sealed abstract class SeriesInstances {

  implicit def seriesMonadPlus[M[_]]: MonadPlus[({type l[a] = Series[M, a]})#l] =
    new SeriesMonadPlus[M]
}

object Series extends SeriesInstances {
  import Isomorphism._

  def iso[M[_]]: ({type l[a] = Series[M, a]})#l <~> ({type l[a] = Kleisli[({type m[b] = LogicT[M, b]})#m, Depth, a]})#l =
    new IsoFunctorTemplate[({type l[a] = Series[M, a]})#l, ({type l[a] = Kleisli[({type m[b] = LogicT[M, b]})#m, Depth, a]})#l] {
      override def to[A](fa: Series[M, A]) =
        fa.run
      override def from[A](ga: Kleisli[({type m[b] = LogicT[M, b]})#m, Depth, A]) =
        Series(ga)
    }

  implicit def seriesMonadLogic[M[_]](implicit M: MonadLogic[M]): MonadLogic[({type l[a] = Series[M, a]})#l] =
    new SeriesMonadPlus[M] with MonadLogic[({type l[a] = Series[M, a]})#l] {
      override def split[A](m: Series[M, A]): Series[M, Option[(A, Series[M, A])]] =
        Series(
          MonadLogic.kleisliMonadLogic[({type l[a] = LogicT[M, a]})#l, Depth].split(m.run).map{
            _.map(x => x._1 -> Series(x._2))
          }
        )
    }
}

private class SeriesMonadPlus[M[_]] extends IsomorphismMonadPlus[({type l[a] = Series[M, a]})#l, ({type l[a] = Kleisli[({type m[b] = LogicT[M, b]})#m, Depth, a]})#l] {
  override def G = Kleisli.kleisliMonadPlus[({type m[b] = LogicT[M, b]})#m, Depth]
  override def iso = Series.iso[M]
}

