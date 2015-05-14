package smallcheck

import scalaz._

final case class StaticArrow[F[_], A[_, _], B, C](run: F[A[B, C]])

sealed abstract class StaticArrowInstances0{

  implicit def staticArrowCategory[A[_, _]: Category, F[_]: Applicative]: Category[({type l[a, b] = StaticArrow[F, A, a, b]})#l] =
    new StaticArrowCategory[A, F]
}

object StaticArrow extends StaticArrowInstances0{

  implicit def staticArrowArrow[A[_, _]: Arrow, F[_]: Applicative]: Arrow[({type l[a, b] = StaticArrow[F, A, a, b]})#l] =
    new StaticArrowArrow[A, F]

}

private class StaticArrowCategory[A[_, _], F[_]](implicit
  A: Category[A],
  F: Applicative[F]
) extends Category[({type l[a, b] = StaticArrow[F, A, a, b]})#l] {

  override def id[B] =
    StaticArrow(F.point(A.id))

  override def compose[X, Y, Z](f: StaticArrow[F, A, Y, Z], g: StaticArrow[F, A, X, Y]) =
    StaticArrow(F.apply2(f.run, g.run)(A.compose))

}

private class StaticArrowArrow[A[_, _], F[_]](implicit
  A: Arrow[A],
  F: Applicative[F]
) extends StaticArrowCategory[A, F] with Arrow[({type l[a, b] = StaticArrow[F, A, a, b]})#l]{

  override def arr[X, Y](f: X => Y): StaticArrow[F, A, X, Y] =
    StaticArrow(F.point(A.arr(f)))

  override def first[X, Y, Z](f: StaticArrow[F, A, X, Y]): StaticArrow[F, A, (X, Z), (Y, Z)] =
    StaticArrow(F.map(f.run)(A.first))
}

