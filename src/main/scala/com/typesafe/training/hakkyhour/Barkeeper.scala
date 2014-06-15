package com.typesafe.training.hakkyhour

import scala.concurrent.duration.FiniteDuration
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ActorLogging
import scala.util.Random

object Barkeeper {
  case class PrepareDrink(drink: Drink, guest: ActorRef)
  case class DrinkPrepared(drink: Drink, guest: ActorRef)

  def props(prepareDrinkDuration: FiniteDuration, accuracy: Int): Props = Props(new Barkeeper(prepareDrinkDuration, accuracy))
}

class Barkeeper(prepareDrinkDuration: FiniteDuration, accuracy: Int) extends Actor with ActorLogging {
  log.debug("barkeeper duration: " + prepareDrinkDuration)
  import Barkeeper._

  override def receive: Receive = {

    case PrepareDrink(desiredDrink, guest) => {
      val preparedDrink =
        if (Random.nextInt(100) < accuracy) desiredDrink
        else Drink.anyOther(desiredDrink)

      busy(prepareDrinkDuration)
      sender() ! DrinkPrepared(preparedDrink, guest)

    }
  }

}