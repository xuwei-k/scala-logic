package smallcheck

import scalaz.Reader

final case class Property[M[_]](
  run: Reader[Env[M], PropertySeries[M]]
)
