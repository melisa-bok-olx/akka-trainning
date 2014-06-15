package com.typesafe.training.hakkyhour

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef

object Waiter {
  def props(hakkyHour: ActorRef, barkeeper: ActorRef, maxComplaintCount: Int): Props = Props(new Waiter(hakkyHour, barkeeper, maxComplaintCount))

  case class ServeDrink(drink: Drink)
  case class DrinkServed(drink: Drink)
  case class Complaint(desiredDrink: Drink)

  case class FrustratedException(desiredDrink: Drink, guest: ActorRef) extends Exception
}

class Waiter(hakkyHour: ActorRef, barkeeper: ActorRef, maxComplaintCount: Int) extends Actor with ActorLogging {
  import Waiter._
  import Barkeeper._
  import HakkyHour._

  var complaints: Int = 0

  override def receive: Receive = {

    case ServeDrink(drink)           => hakkyHour ! ApproveDrink(drink, sender())
    case DrinkPrepared(drink, guest) => guest ! DrinkServed(drink)
    case Complaint(desiredDrink) => {
      complaints += 1
      if (complaints > maxComplaintCount) {
        throw FrustratedException(desiredDrink, sender())
      } else {
        barkeeper ! PrepareDrink(desiredDrink, sender())
      }
    }
  }

}