package smallcheck

final case class PropertySeries[M[_]](
  examples        : Series[M, PropertySuccess],
  counterExamples : Series[M, PropertyFailure],
  closest         : Series[M, (Property[M], List[Argument])]
)
