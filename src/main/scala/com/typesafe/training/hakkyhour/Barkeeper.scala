package com.typesafe.training.hakkyhour

import scala.concurrent.duration.FiniteDuration
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ActorLogging

object Barkeeper {
  case class PrepareDrink(drink: Drink, guest: ActorRef)
  case class DrinkPrepared(drink: Drink, guest: ActorRef)

  def props(prepareDrinkDuration: FiniteDuration): Props = Props(new Barkeeper(prepareDrinkDuration))
}

class Barkeeper(prepareDrinkDuration: FiniteDuration) extends Actor with ActorLogging {
  log.debug("barkeeper duration: " + prepareDrinkDuration)
  import Barkeeper._

  override def receive: Receive = {

    case PrepareDrink(drink, guest) => {
      busy(prepareDrinkDuration)
      sender() ! DrinkPrepared(drink, guest)
    }
  }

}