package smallcheck

final case class Env[M[_]](
  quantification: Quantification,
  testHook: TestQuality => M[Unit]
)
