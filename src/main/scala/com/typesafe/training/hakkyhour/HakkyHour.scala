package com.typesafe.training.hakkyhour

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import java.util.concurrent.TimeUnit
import akka.actor.PoisonPill
import akka.actor.Terminated
import akka.actor.SupervisorStrategy
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import akka.routing.FromConfig

object HakkyHour {
  def props(maxDrinkCount: Int): Props = Props(new HakkyHour(maxDrinkCount: Int))
  case class CreateGuest(favoriteDrink: Drink, isStubborn: Boolean, maxDrinkCount: Int)
  case class ApproveDrink(drink: Drink, guest: ActorRef)
  case object NoMoreDrinks
}

class HakkyHour(maxDrinkCount: Int) extends Actor with ActorLogging {
  import HakkyHour._
  import Barkeeper._

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case Guest.DrunkException => Stop
    case Waiter.FrustratedException(desiredDrink, guest) =>
      // tell barkeeper to prepare the desired drink
      //barkeeper forward PrepareDrink(desiredDrink, guest)
      barkeeper.tell(PrepareDrink(desiredDrink, guest), waiter)
      Restart
  }

  log.debug(s"Hakky Hour is open! ${self.path.name}")
  val finishDrinkDurationLong: Long = context.system.settings.config.getDuration("hakky-hour.guest.finish-drink-duration", TimeUnit.SECONDS)
  val finishDrinkDuration: FiniteDuration = FiniteDuration(finishDrinkDurationLong, TimeUnit.SECONDS)
  val barkeeperDrinkDurationLong: Long = context.system.settings.config.getDuration("hakky-hour.barkeeper.prepare-drink-duration", TimeUnit.SECONDS)
  val barkeeperPrepareDrinkDuration: FiniteDuration = FiniteDuration(barkeeperDrinkDurationLong, TimeUnit.SECONDS)
  val maxComplaintCount: Int = context.system.settings.config.getInt("hakky-hour.max-complaint-count")
  val barkeeperAccuracy: Int = context.system.settings.config.getInt("hakky-hour.barkeeper.accuracy")

  val barkeeper = createBarkeeper()
  val waiter = createWaiter()

  def createGuest(favoriteDrink: Drink, finishDrinkDuration: FiniteDuration, isStubborn: Boolean, maxDrinkCount: Int): Unit = {
    val guest = context.actorOf(Guest.props(waiter, favoriteDrink, finishDrinkDuration, isStubborn, maxDrinkCount))
    context.watch(guest)
  }

  def createWaiter(): ActorRef =
    context.actorOf(Waiter.props(self, barkeeper, maxComplaintCount), "waiter")

  def createBarkeeper(): ActorRef =
    context.actorOf(Barkeeper.props(barkeeperPrepareDrinkDuration, barkeeperAccuracy).withRouter(FromConfig()), "barkeeper")

  var drinks = Map[ActorRef, Int]()
  var hadMaxDrinks = Set[ActorRef]()

  override def receive: Receive = {
    case CreateGuest(favoriteDrink, isStubborn, maxDrinkCount) => createGuest(favoriteDrink, finishDrinkDuration, isStubborn, maxDrinkCount)

    case ApproveDrink(drink, guest) => { //message binding, msg is a reference to the matching

      val drinksCount = drinks.get(guest) match {
        case Some(count) => count
        case None        => 0
      }

      if (drinksCount < maxDrinkCount) {
        barkeeper forward PrepareDrink(drink, guest)
        drinks = drinks + (guest -> (drinksCount + 1))
      } else if (hadMaxDrinks(guest)) { //is stubborn
        guest ! PoisonPill
      } else {
        log.info(s"Sorry, ${guest.path.name}, but we won't serve you more than $maxDrinkCount drinks!")
        guest ! NoMoreDrinks
        hadMaxDrinks = hadMaxDrinks + guest
      }
    }
    case Terminated(guest) => {
      drinks = drinks - guest
      hadMaxDrinks = hadMaxDrinks - guest
      log.info(s"Thanks, ${guest.path.name}, for being our guest!")
    }
  }
}
