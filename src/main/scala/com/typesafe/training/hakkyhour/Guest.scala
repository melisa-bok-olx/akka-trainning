package com.typesafe.training.hakkyhour

import scala.concurrent.duration.FiniteDuration
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy

object Guest {
  def props(waiter: ActorRef, favoriteDrink: Drink, finishDrinkDuration: FiniteDuration, isStubborn: Boolean, maxDrinkCount: Int): Props = Props(new Guest(waiter, favoriteDrink, finishDrinkDuration, isStubborn, maxDrinkCount))

  private case object DrinkFinished
  case object DrunkException extends Exception
}

class Guest(waiter: ActorRef, favoriteDrink: Drink, finishDrinkDuration: FiniteDuration, isStubborn: Boolean, maxDrinkCount: Int) extends Actor with ActorLogging {
  import Waiter._
  import Guest._
  import HakkyHour._
  import context.dispatcher

  waiter ! ServeDrink(favoriteDrink)

  var drinkCount: Int = 0

  override def receive: Receive = {

    case DrinkServed(drink) => {

      if (drink == favoriteDrink) {
        drinkCount = drinkCount + 1
        log.info(s"Enjoying my $drinkCount. yummy $drink!")

        context.system.scheduler.scheduleOnce(
          finishDrinkDuration,
          self,
          DrinkFinished)
      } else {
        waiter ! Complaint(favoriteDrink)
      }
    }
    case DrinkFinished => {
      if (drinkCount > maxDrinkCount)
        throw DrunkException
      waiter ! ServeDrink(favoriteDrink)
    }

    case NoMoreDrinks => {
      if (isStubborn) {
        //waiter ! ServeDrink(favoriteDrink)
        self ! DrinkFinished
      } else {
        log.info("All right, time to go home!")
        context.stop(self)
      }
    }
  }

  override def postStop = {
    log.info("Good-bye!")
  }

}