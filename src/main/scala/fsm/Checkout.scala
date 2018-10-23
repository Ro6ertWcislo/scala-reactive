package fsm


import akka.actor.{Actor, FSM, Timers}
import akka.event.{Logging, LoggingReceive}
import fsm.Checkout.{CheckoutData, CheckoutState}

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


  sealed trait CheckoutData

  case object Uninitialized extends CheckoutData


  sealed trait CheckoutState

  case object SelectionDelivery extends CheckoutState

  case object SelectingPaymentMethod extends CheckoutState

  case object ProcessingPayment extends CheckoutState

}

class Checkout() extends FSM[CheckoutState, CheckoutData] with Timers {

  import Checkout._

  private val timeoutt = 500.millis


  private def stopWork(): Unit = {
    log.info("Exit checkout")
    sender ! CheckoutCanceled
    context.stop(self)
  }

  private def handleUnknown(unknown: Any): Unit = log.warning("Unknown message Encountered: {}", unknown)


  startWith(SelectionDelivery, Uninitialized)

  when(SelectionDelivery) {
    case Event(SelectDeliveryMethod(method), _) =>
      timers.startSingleTimer(CheckoutTimerKey, CheckoutTimeExceeded, timeoutt)
      log.info("Selected delivery method {}", method)
      goto(SelectingPaymentMethod)


  }

  when(SelectingPaymentMethod) {
    case Event(SelectPaymentMethod(method), _) =>
      timers.cancel(CheckoutTimerKey)
      timers.startSingleTimer(PaymentTimerKey, PaymentTimeExceeded, timeoutt)

      log.info("Selected payment method {}", method)
      goto(ProcessingPayment)
  }

  when(ProcessingPayment) {
    case Event(Payment(status), _) =>
      log.info("Payment status: {}", status)
      sender ! CloseCheckout
      context.stop(self)
      stay()
  }

  whenUnhandled {
    case Event(CancelCheckout, _) => stop()
    case (Event(CheckoutTimeExceeded, _)) => stop()
    case Event(PaymentTimeExceeded, _) => stop()
    case Event(unknown, _) => handleUnknown(unknown); stay()
  }

  def processingPayment: Receive = LoggingReceive {
    case Payment(status) =>


    case CancelCheckout =>
      stopWork()

  }

}
