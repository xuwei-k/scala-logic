package smallcheck

import scalaz.Monad

trait Serial[M[_], A] extends Monad[M] {

  def series: Series[M, A]

}
