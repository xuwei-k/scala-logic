package smallcheck

trait GCoSerial[M[_], F[_]] {
  def gCoseries[A, B](b: Series[M, B]): Series[M, F[A] => B]
}
