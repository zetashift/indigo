package indigo.shared.temporal

import indigo.shared.time.Seconds

/**
  * SignalReader is basically a wrapper around signal that
  * provides an execution environment. As such it generally
  * delegates to Signal.
  *
  * Unlike a normal Reader, SignalReader is not a Kliesli,
  * since Signals do not compose.
  */
final class SignalReader[R, A](val f: R => Signal[A]) extends AnyVal {
  def toSignal(readFrom: R): Signal[A] =
    f(readFrom)

  def merge[B, C](other: SignalReader[R, B])(f: (A, B) => C): SignalReader[R, C] =
    SignalReader((r: R) => toSignal(r).merge(other.toSignal(r))(f))

  def |>[B](sf: SignalFunction[A, B]): SignalReader[R, B] =
    pipe(sf)
  def pipe[B](sf: SignalFunction[A, B]): SignalReader[R, B] =
    SignalReader((r: R) => sf.run(toSignal(r)))

  def |*|[B](other: SignalReader[R, B]): SignalReader[R, (A, B)] =
    combine(other)
  def combine[B](other: SignalReader[R, B]): SignalReader[R, (A, B)] =
    SignalReader((r: R) => toSignal(r).combine(other.toSignal(r)))

  def clampTime(from: Seconds, to: Seconds): SignalReader[R, A] =
    SignalReader((r: R) => toSignal(r).clampTime(from, to))
  def wrapTime(at: Seconds): SignalReader[R, A] =
    SignalReader((r: R) => toSignal(r).wrapTime(at))
  def affectTime(multiplyBy: Double): SignalReader[R, A] =
    SignalReader((r: R) => toSignal(r).affectTime(multiplyBy))

  def map[B](f: A => B): SignalReader[R, B] =
    SignalReader((r: R) => toSignal(r).map(f))

  def ap[B](f: SignalReader[R, A => B]): SignalReader[R, B] =
    SignalReader((r: R) => toSignal(r).ap(f.toSignal(r)))

  def flatMap[B](f: A => SignalReader[R, B]): SignalReader[R, B] =
    SignalReader((r: R) => toSignal(r).flatMap(a => f(a).toSignal(r)))
}

object SignalReader {

  def fromSignal[R, A](signal: Signal[A]): SignalReader[R, A] =
    apply((_: R) => signal)

  def fixed[R, A](a: A): SignalReader[R, A] =
    apply((_: R) => Signal.fixed(a))

  def apply[R, A](f: R => Signal[A]): SignalReader[R, A] =
    new SignalReader[R, A](f)

}