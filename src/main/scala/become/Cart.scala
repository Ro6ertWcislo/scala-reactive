package become

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.{Logging, LoggingReceive}
import become.Checkout.{CancelCheckout, CheckoutCanceled, CloseCheckout, Payment, SelectDeliveryMethod, SelectPaymentMethod, StartCheckout, _}

import scala.collection.mutable
import scala.concurrent.duration._


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

}


class Cart[T] extends Actor with Timers {

  import Cart._

  private val log = Logging(context.system, this)
  private val timeout = 500.millis

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

  def empty: Receive = LoggingReceive {
    case AddItem(item: T) =>
      addItem(item)
      context become nonEmpty

    case unknown => handleUnknown(unknown)
  }


  def nonEmpty: Receive = LoggingReceive {
    case AddItem(item: T) =>
      addItem(item)
      timers.startSingleTimer(TimerKey, TimeExceeded, timeout)

    case RemoveItem(item: T) =>
      removeItem(item)
      if (items.isEmpty) {
        timers.cancel(TimerKey)
        context become empty
      } else
        timers.startSingleTimer(TimerKey, TimeExceeded, timeout)

    case StartCheckout =>
      if (items.nonEmpty)
        sender ! CheckoutStarted
      timers.cancel(TimerKey)
      context become inCheckout(context.actorOf(Props[Checkout], "become.Checkout"))

    case TimeExceeded =>
      log.info("become.Cart time exceeded. Removing items from cart")
      items.clear()
      context become empty

    case unknown => handleUnknown(unknown)
  }

  def inCheckout(checkout: ActorRef): Receive = LoggingReceive {
    case CancelCheckout =>
      checkout ! CancelCheckout

    case CheckoutCanceled =>
      context become nonEmpty

    case CloseCheckout =>
      sender ! CheckoutClosed
      context become empty

    case SelectDeliveryMethod(method) =>
      checkout ! SelectDeliveryMethod(method)

    case SelectPaymentMethod(method) =>
      checkout ! SelectPaymentMethod(method)

    case Payment(status) =>

      checkout ! Payment(status)

    case unknown => handleUnknown(unknown)

  }

  override def receive: Receive = empty
}
