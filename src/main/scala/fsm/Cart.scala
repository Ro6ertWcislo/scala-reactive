package fsm

import akka.actor.{Actor, ActorRef, FSM, Props, Timers}
import akka.event.{Logging, LoggingReceive}
import fsm.Checkout._

import scala.collection.mutable
import scala.concurrent.duration._
import Cart.{Uninitialized, _}

object Cart {

  sealed trait Command

  case class AddItem[T](item: T) extends Command

  case class RemoveItem[T](item: T) extends Command


  sealed trait Event

  case class ItemAdded[T](item: T) extends Event

  case class ItemRemoved[T](item: T) extends Event

  case object TimeExceeded extends Event


  sealed trait DocMessages

  case class Error[T](item: T, message: String) extends DocMessages


  case object TimerKey


  sealed trait CartData

  case object Uninitialized extends CartData

  case class SomeData[T](item: T) extends CartData

  sealed trait CartState

  case object Empty extends CartState

  case object NonEmpty extends CartState

  case object InCheckout extends CartState

}


class Cart[T] extends FSM[CartState, CartData] with Timers {


  private val timeoutt = 500.millis

  private val items = mutable.HashSet[T]()

  private def handleUnknown(unknown: Any): Unit = log.warning("Unknown message Encountered: {}", unknown)


  private def addItem(item: T): Unit = {
    items.add(item)
    sender ! ItemAdded(item)
  }

  private def removeItem(item: T): Unit = {
    if (items.remove(item))
      sender ! ItemAdded(item)
    else sender ! Error(item, "No such item in the cart.")
  }

  startWith(Empty, Uninitialized)

  when(Empty) {
    case Event(AddItem(item: T), _) =>
      addItem(item)
      goto(NonEmpty)
  }


  when(NonEmpty) {
    case Event(AddItem(item: T), _) =>
      addItem(item)
      timers.startSingleTimer(TimerKey, TimeExceeded, timeoutt)
      stay

    case Event(RemoveItem(item: T), _) =>
      removeItem(item)
      if (items.isEmpty) {
        timers.cancel(TimerKey)
        goto(Empty)
      } else
        timers.startSingleTimer(TimerKey, TimeExceeded, timeoutt)
      stay
    case Event(TimeExceeded, _) =>
      log.info("become.Cart time exceeded. Removing items from cart")
      items.clear()
      goto(Empty)
    case Event(StartCheckout, _) =>
      if (items.nonEmpty) {
        sender ! CheckoutStarted
        timers.cancel(TimerKey)
        goto(InCheckout)
      }else stay


  }


  when(InCheckout) {
    case Event(CancelCheckout, _) => goto(NonEmpty)
    case Event(CloseCheckout, _) => goto(Empty)

  }
  whenUnhandled {
    case Event(message, _) => handleUnknown(message)
      stay
  }

}
