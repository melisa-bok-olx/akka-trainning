package com.typesafe.training.hakkyhour

import scala.concurrent.duration.FiniteDuration

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._

object Guest {
  def props(waiter: ActorRef, favoriteDrink: Drink, finishDrinkDuration: FiniteDuration): Props = Props(new Guest(waiter, favoriteDrink, finishDrinkDuration))

  private case object DrinkFinished
}

class Guest(waiter: ActorRef, favoriteDrink: Drink, finishDrinkDuration: FiniteDuration) extends Actor with ActorLogging {
  import Waiter._
  import Guest._
  import HakkyHour._
  import context.dispatcher

  waiter ! ServeDrink(favoriteDrink)

  var drinkCount: Int = 0

  override def receive: Receive = {

    case DrinkServed(drink) => {
      drinkCount = drinkCount + 1
      log.info(s"Enjoying my $drinkCount. yummy $drink!")

      context.system.scheduler.scheduleOnce(
        finishDrinkDuration,
        self,
        DrinkFinished)
    }
    case DrinkFinished => waiter ! ServeDrink(favoriteDrink)

    case NoMoreDrinks => {
      log.info("All right, time to go home!")
      context.stop(self)
    }
  }

  override def postStop = {
    log.info("Good-bye!")
  }
}