package com.typesafe.training.hakkyhour

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef

object Waiter {
  def props(hakkyHour: ActorRef): Props = Props(new Waiter(hakkyHour))

  case class ServeDrink(drink: Drink)
  case class DrinkServed(drink: Drink)
}

class Waiter(hakkyHour: ActorRef) extends Actor with ActorLogging {
  import Waiter._
  import Barkeeper._
  import HakkyHour._

  override def receive: Receive = {

    case ServeDrink(drink)           => hakkyHour ! ApproveDrink(drink, sender())
    case DrinkPrepared(drink, guest) => guest ! DrinkServed(drink)
  }

}