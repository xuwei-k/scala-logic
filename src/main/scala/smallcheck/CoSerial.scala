package smallcheck

import scalaz.{Maybe, Monad}

trait CoSerial[M[_], A] extends Monad[M] {

  def coseriesP[B](fa: Series[M, B]): Series[M, A => Maybe[B]]

}
