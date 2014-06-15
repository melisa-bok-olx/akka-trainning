package com.typesafe.training.hakkyhour

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import java.util.concurrent.TimeUnit

object HakkyHour {
  def props(maxDrinkCount: Int): Props = Props(new HakkyHour(maxDrinkCount: Int))
  case class CreateGuest(favoriteDrink: Drink)
  case class ApproveDrink(drink: Drink, guest: ActorRef)
  case object NoMoreDrinks
}

class HakkyHour(maxDrinkCount: Int) extends Actor with ActorLogging {
  import HakkyHour._
  import Barkeeper._

  log.debug("Hakky Hour is open!")
  val finishDrinkDurationLong: Long = context.system.settings.config.getDuration("hakky-hour.guest.finish-drink-duration", TimeUnit.SECONDS)
  val finishDrinkDuration: FiniteDuration = FiniteDuration(finishDrinkDurationLong, TimeUnit.SECONDS)
  val barkeeperDrinkDurationLong: Long = context.system.settings.config.getDuration("hakky-hour.barkeeper.prepare-drink-duration", TimeUnit.SECONDS)
  val barkeeperDrinkDuration: FiniteDuration = FiniteDuration(barkeeperDrinkDurationLong, TimeUnit.SECONDS)

  val barkeeper = createBarkeeper()
  val waiter = createWaiter("waiter")

  def createGuest(favoriteDrink: Drink, finishDrinkDuration: FiniteDuration): ActorRef =
    context.actorOf(Guest.props(waiter, favoriteDrink, finishDrinkDuration))

  def createWaiter(name: String): ActorRef =
    context.actorOf(Waiter.props(self), name)

  def createBarkeeper(): ActorRef =
    context.actorOf(Barkeeper.props(barkeeperDrinkDuration), "barkeeper")

  var drinks = Map[ActorRef, Int]()

  override def receive: Receive = {
    case CreateGuest(favoriteDrink) => createGuest(favoriteDrink, finishDrinkDuration)
    case ApproveDrink(drink, guest) => { //message binding, msg is a reference to the matching

      val drinksCount = drinks.get(guest) match {
        case Some(count) => count
        case None        => 0
      }

      if (drinksCount < maxDrinkCount) {
        barkeeper forward PrepareDrink(drink, guest)
        drinks = drinks + (guest -> (drinksCount + 1))
      } else {
        log.info(s"Sorry, $guest, but we won't serve you more than $maxDrinkCount drinks!")
        guest ! NoMoreDrinks
      }
    }
  }
}
