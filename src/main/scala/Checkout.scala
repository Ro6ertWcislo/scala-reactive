import Cart.TimerKey
import akka.actor.{Actor, Timers}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._

object Checkout {

  sealed trait Command

  case object StartCheckout extends Command

  case object CancelCheckout extends Command

  case object CloseCheckout extends Command

  case class SelectDeliveryMethod(deliveryMethod: DeliveryMethod) extends Command

  case class SelectPaymentMethod(paymentMethod: PaymentMethod) extends Command

  case class Payment(status: Boolean) extends Command


  sealed trait Event

  case object CheckoutTimeExceeded extends Event

  case object PaymentTimeExceeded extends Event

  case object CheckoutStarted extends Event

  case object CheckoutCanceled extends Event

  case object CheckoutClosed extends Event


  sealed class DeliveryMethod

  case object Plane extends DeliveryMethod

  case object Train extends DeliveryMethod


  sealed class PaymentMethod

  case object PayPal extends PaymentMethod

  case object Card extends PaymentMethod


  case object PaymentTimerKey

  case object CheckoutTimerKey

}

class Checkout() extends Actor with Timers {

  import Checkout._

  private val timeout = 500.millis
  private val log = Logging(context.system, this)


  private def stop(): Unit = {
    log.info("Exit checkout")
    sender ! CheckoutCanceled
    context.stop(self)
  }

  private def handleUnknown(unknown: Any): Unit = log.warning("Unknown message Encountered: {}", unknown)


  def selectionDelivery: Receive = LoggingReceive {
    case SelectDeliveryMethod(method) =>
      timers.startSingleTimer(CheckoutTimerKey, CheckoutTimeExceeded, timeout)
      log.info("Selected delivery method {}", method)
      context become selectingPaymentMethod

    case CancelCheckout =>
      stop()
    case CheckoutTimeExceeded => stop()
    case unknown => handleUnknown(unknown)
  }

  def selectingPaymentMethod: Receive = LoggingReceive {
    case SelectPaymentMethod(method) =>
      timers.cancel(CheckoutTimerKey)
      timers.startSingleTimer(PaymentTimerKey, PaymentTimeExceeded, timeout)

      log.info("Selected payment method {}", method)
      context become processingPayment

    case CancelCheckout =>
      stop()
    case CheckoutTimeExceeded => stop()
    case unknown => handleUnknown(unknown)
  }


  def processingPayment: Receive = LoggingReceive {
    case Payment(status) =>
      log.info("Payment status: {}", status)
      sender ! CloseCheckout
      context.stop(self)

    case CancelCheckout =>
      stop()
    case PaymentTimeExceeded =>
      stop()

    case unknown => handleUnknown(unknown)
  }

  override def receive: Receive = selectionDelivery
}
