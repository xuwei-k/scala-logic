package logic

import scalaz._
import Id._
import scalaz.syntax.bind._

trait LogicT[F[_], A] {

  def apply[R](l: F[R])(f: A => F[R] => F[R]): F[R]

  def observe(implicit M: Applicative[F]): F[Option[A]] =
    this(M.pure(None: Option[A]))(a => Function.const(M.pure(Some(a))))

  def observeAll(implicit M: Applicative[F]): F[List[A]] =
    this(M.pure(Nil: List[A]))(a => b => M.map(b)(a :: _))

  def observeMany(n: Int)(implicit M: Monad[F]): F[List[A]] = {
    def sk[X](o: Option[(A, LogicT[F, A])])(x: X): F[List[A]] = o match {
      case None => M.pure(Nil)
      case Some((a, m)) => M.map(m.observeMany(n - 1))(a :: _)
    }
    if(n <= 0) M.pure(Nil)
    else if(n == 1) this(M.pure(Nil: List[A]))(a => Function.const(M.pure(List(a))))
    else MonadLogic[LogicT[F, ?]].split(this)(M.pure(Nil: List[A]))(sk)
  }
}

object LogicT extends LogicTInstances {

  implicit val logicTraverse: Traverse[Logic] = new Traverse[Logic] {
    def traverseImpl[F[_], A, B](l: Logic[A])(f: A => F[B])(implicit F: Applicative[F]) = {
      def cons(b: B)(ll: Logic[B]): Logic[B] = logicTMonadPlus.plus(logicTMonadPlus.pure(b), ll)
      l(F.pure(logicTMonadPlus[Id].empty[B]))(a => ft => F.ap(ft)(F.map(f(a))(cons _)))
    }
  }
}

sealed abstract class LogicTInstances3 {

  implicit def logicTMonadPlus[F[_]]: MonadPlus[LogicT[F, ?]] =
    new LogicTMonadPlus[F]{}

  implicit val logicTMonadTrans: MonadTrans[LogicT] = new MonadTrans[LogicT] {
    def liftM[G[_] : Monad, A](m: G[A]) = new LogicT[G, A] {
      def apply[R](l: G[R])(f: A => G[R] => G[R]): G[R] = m.flatMap(a => f(a)(l))
    }
    def apply[G[_]: Monad] = logicTMonadPlus[G]
  }

  implicit def logicTFoldable[F[_]](implicit T: Foldable[F], S: Applicative[F]): Foldable[LogicT[F, ?]] = new Foldable[LogicT[F, ?]] {

    def foldMap[A, B](fa: LogicT[F, A])(f: A => B)(implicit M: Monoid[B]) =
      T.fold(fa(S.pure(M.zero))(a => b => S.map(b)(M.append(f(a), _))))

    def foldRight[A, B](fa: LogicT[F, A], z: => B)(f: (A, => B) => B) =
      foldMap(fa)((a: A) => (Endo.endo(f(a, _: B)))) apply z
  }
}

sealed abstract class LogicTInstances2 extends LogicTInstances3 {

  implicit def logicTMonadLogic[F[_]](implicit F: Monad[F]): MonadLogic[LogicT[F, ?]] =
    new LogicTMonadPlus[F] with MonadLogic[LogicT[F, ?]] {
      override def split[A](m: LogicT[F, A]): LogicT[F, Option[(A, LogicT[F, A])]] =
        MonadTrans[LogicT].liftM(
          m.apply(F.point(Option.empty[(A, LogicT[F, A])]))(
            a => fk => F.point(
              Some((
                a,
                MonadTrans[LogicT].liftM(fk).flatMap(
                  MonadLogic.reflect[LogicT[F, ?], A]
                )
                ))
            )
          )
        )
    }
}

sealed abstract class LogicTInstances1 extends LogicTInstances2 {

  def logicTMonadReader[F[_, _], R](implicit F0: MonadReader[F, R]): MonadReader[Lambda[(A, B) => LogicT[F[A, ?], B]], R] =
    new LogicTMonadReader[F, R] {
      implicit def F: MonadReader[F, R] = F0
    }
}

sealed abstract class LogicTInstances0 extends LogicTInstances1 {
  def logicTMonadState[F[_, _], S](implicit F0: MonadState[F, S]): MonadState[Lambda[(A, B) => LogicT[F[A, ?], B]], S] =
    new LogicTMonadState[F, S] {
      implicit def F: MonadState[F, S] = F0
    }
}

sealed abstract class LogicTInstances extends LogicTInstances0 {
  def logicTMonadError[F[_, _], E](implicit F0: MonadError[F, E]): MonadError[Lambda[(A, B) => LogicT[F[A, ?], B]], E] =
    new LogicTMonadError[F, E] {
      implicit def F: MonadError[F, E] = F0
    }
}

private abstract class LogicTMonadPlus[F[_]] extends MonadPlus[LogicT[F, ?]] {
  override final def map[A, B](lt: LogicT[F, A])(f: A => B) = new LogicT[F, B] {
    def apply[R](l: F[R])(sk: B => F[R] => F[R]) = lt(l)(a => sk(f(a)))
  }

  override final def point[A](a: => A) = new LogicT[F, A] {
    def apply[R](fk: F[R])(sk: A => F[R] => F[R]) = sk(a)(fk)
  }

  override final def bind[A, B](fa: LogicT[F, A])(f: A => LogicT[F, B]) = new LogicT[F, B] {
    def apply[R](fk: F[R])(sk: B => F[R] => F[R]) = fa(fk)(a => fkk => f(a)(fkk)(sk))
  }

  override final def empty[A] = new LogicT[F, A] {
    def apply[R](fk: F[R])(sk: A => F[R] => F[R]) = fk
  }

  override final def plus[A](a: LogicT[F, A], b: => LogicT[F, A]) = new LogicT[F, A] {
    def apply[R](fk: F[R])(sk: A => F[R] => F[R]) = a(b(fk)(sk))(sk)
  }
}

private trait LogicTMonadReader[F[_, _], R] extends LogicTMonadPlus[F[R, ?]] with MonadReader[Lambda[(A, B) => LogicT[F[A, ?], B]], R] {

  implicit def F: MonadReader[F, R]

  type G[X] = F[R, X]

  def ask = LogicT.logicTMonadTrans.liftM[G, R](F.ask)
  def local[A](f: R => R)(m: LogicT[G, A]): LogicT[G, A] = new LogicT[G, A] {
    def apply[X](l: G[X])(sk: A => G[X] => G[X]) =
      m(F.local(f)(l))(x => sk(x))
  }
}

private trait LogicTMonadState[F[_, _], S] extends LogicTMonadPlus[F[S, ?]] with MonadState[Lambda[(A, B) => LogicT[F[A, ?], B]], S] {

  implicit def F: MonadState[F, S]

  type G[X] = F[S, X]

  def init = LogicT.logicTMonadTrans.liftM[G, S](F.init)
  def get = LogicT.logicTMonadTrans.liftM[G, S](F.get)
  def put(s: S) = LogicT.logicTMonadTrans.liftM[G, Unit](F.put(s))
}

private trait LogicTMonadError[F[_, _], E] extends LogicTMonadPlus[F[E, ?]] with MonadError[Lambda[(A, B) => LogicT[F[A, ?], B]], E] {

  implicit def F: MonadError[F, E]

  type G[X] = F[E, X]

  def raiseError[A](e: E) = LogicT.logicTMonadTrans.liftM[G, A](F.raiseError(e))
  def handleError[A](l: LogicT[G, A])(f: E => LogicT[G, A]): LogicT[G, A] = new LogicT[G, A] {
    def apply[X](fk: G[X])(sk: A => G[X] => G[X]) = {
      def handle(r: G[X]): G[X] = F.handleError(r)(e => f(e)(fk)(sk))
      handle(l(fk)(a => x => sk(a)(handle(x))))
    }
  }
}
