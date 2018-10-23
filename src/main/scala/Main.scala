import Cart._
import Checkout._
import akka.actor.{ActorSystem, Props}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  case class Item(id: Int, name: String){
    override def equals(o: scala.Any): Boolean = o match {
      case obj : Item =>
        val f  = obj.id == id
        f
      case _ => false
    }
  }

  val system = ActorSystem("Reactive2")
  val mainActor = system.actorOf(Props[Cart[Item]], "Cart")


  mainActor ! AddItem(Item(1,"abc"))
  mainActor ! RemoveItem(Item(1,"abc"))
  mainActor ! AddItem(Item(1,"abc"))
  mainActor ! AddItem(Item(2,"abc"))
  mainActor ! AddItem(Item(1,"abd"))
  Thread.sleep(3000)
  mainActor ! AddItem(Item(1,"abd"))
  mainActor ! StartCheckout
  mainActor ! SelectDeliveryMethod(Train)
  mainActor ! SelectPaymentMethod(PayPal)
  mainActor ! Payment(true)
  Thread.sleep(100)
  mainActor ! AddItem(Item(1,"abd"))
  mainActor ! StartCheckout
  Thread.sleep(3000)



  Await.result(system.whenTerminated, Duration.Inf)




}
