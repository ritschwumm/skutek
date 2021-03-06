package skutek
import _internals._

sealed trait Choice extends FilterableEffect
object Choice extends NullaryEffectCompanion[Choice]

case class Choose[A](values: Iterable[A]) extends Operation[A, Choice]

object NoChoice extends Choose(Iterable.empty[Nothing])
object Choose {
  def from[A](as: A*) = Choose(as)
}

case object ChoiceHandler extends AllChoiceHandler {
  case object FindFirst extends FirstChoiceHandler
}

protected abstract class BaseChoiceHandler extends NullaryHandler[Choice] {
  override val onFilterFail = Some(NoChoice)
  type Op[A] = Choose[A]
}


protected class AllChoiceHandler extends BaseChoiceHandler {
  type Result[A] = Vector[A]

  def onReturn[A](a: A) = Return(Vector(a))

  def onOperation[A, B, U](op: Op[A], k: A => Vector[B] !! U): Vector[B] !! U = {
    iterate(op.values.iterator, k)
  }

  def onProduct[A, B, C, U](a_! : Vector[A] !! U, b_! : Vector[B] !! U, k: ((A, B)) => Vector[C] !! U): Vector[C] !! U =
    (a_! *! b_!).flatMap { 
      case (as, bs) => {
        val it = for { 
          a <- as.iterator
          b <- bs.iterator 
        } yield (a, b)
        iterate(it, k)
      }
    }


  private def iterate[A, B, U](todos: Iterator[A], k: A => Vector[B] !! U): Vector[B] !! U = {
    def loop(accum: Vector[B]): Vector[B] !! U = {
      if (todos.isEmpty)
        Return(accum)
      else {
        val a = todos.next
        for {
          bs <- k(a)
          bs2 <- loop(accum ++ bs)
        } yield bs2
      }
    }
    loop(Vector())
  }
}


protected abstract class FirstChoiceHandler extends BaseChoiceHandler {
  type Result[A] = Option[A]

  def onReturn[A](a: A) = Return(Some(a))

  def onOperation[A, B, U](op: Op[A], k: A => Option[B] !! U): Option[B] !! U = {
    iterate(op.values, k)
  }

  def onProduct[A, B, C, U](a_! : Option[A] !! U, b_! : Option[B] !! U, k: ((A, B)) => Option[C] !! U): Option[C] !! U =
    (a_! *! b_!).flatMap { 
      case (Some(a), Some(b)) => k((a, b))
      case _ => Return(None)
    }


  private def iterate[A, B, U](todos: Iterable[A], k: A => Option[B] !! U): Option[B] !! U = {
    def loop(todos: Iterable[A]): Option[B] !! U = {
      if (todos.isEmpty)
        Return(None)
      else {
        for {
          b_? <- k(todos.head)
          b2_? <- b_? match {
            case Some(b) => Return(Some(b))
            case None => loop(todos.tail)
          }
        } yield b2_?
      }
    }
    loop(todos)
  }
}
