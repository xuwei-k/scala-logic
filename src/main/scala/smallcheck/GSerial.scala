package smallcheck

trait GSerial[M[_], F[_]] {
  def gSeries[A]: Series[M, F[A]]
}
