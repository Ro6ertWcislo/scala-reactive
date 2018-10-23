import akka.actor.{Actor, Timers}
import akka.event.LoggingReceive

object Checkout{

  sealed trait Command
  case object StartCheckout extends Command

  case object CancelCheckout extends Command

  case object CloseCheckout

  sealed trait Event
  case object CheckoutTimeExceeded extends Event
  case object PaymentTimeExceeded extends Event
  case object CheckoutStarted extends Event

  case object CheckoutCanceled extends Event

  case object CheckoutClosed extends Event
}

class Checkout extends Actor with Timers{

  def selectionDelivery: Receive = LoggingReceive {}
  def selectingPaymentMethod: Receive = LoggingReceive {}
  def cancelled: Receive = LoggingReceive {}
  def processingPayment: Receive = LoggingReceive {}
  def selectionDelivery: Receive = LoggingReceive {}

  override def receive: Receive = selectionDelivery
}
