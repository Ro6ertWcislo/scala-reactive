import Cart._
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


  val x  = mutable.HashSet[Int]()
  x.add(1)
  x.add(2)
  x.add(1)

  mainActor ! AddItem(Item(1,"abc"))
  mainActor ! RemoveItem(Item(1,"abc"))
  mainActor ! AddItem(Item(1,"abc"))
  mainActor ! AddItem(Item(2,"abc"))
  mainActor ! AddItem(Item(1,"abd"))
  Thread.sleep(2000)
  mainActor ! AddItem(Item(1,"abd"))
  mainActor ! Checkout


  Await.result(system.whenTerminated, Duration.Inf)




}
