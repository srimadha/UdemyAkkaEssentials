package com.udemy.section4.testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class InterceptLoggingSpec
  extends TestKit(ActorSystem("intercepting", ConfigFactory.load().getConfig("interceptingLogMessages")))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import InterceptLoggingSpec._

  "checkout flow" should {
    val item = "Rock the jvm"
    "correctly log dispatch of an order" in {
      EventFilter.info(pattern = s"Order [0-9]+ for $item", occurrences = 1) intercept { //Event filter log level info
        val checkoutActor = system.actorOf(Props[CheckoutActor])
        checkoutActor ! Checkout(item, "123")
      }
    }
    "Freak out if payment is denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        val checkoutActor = system.actorOf(Props[CheckoutActor])
        checkoutActor ! Checkout(item, "023")
      }
    }
  }
}

object InterceptLoggingSpec {

  case class Checkout(itme: String, card: String)

  case class AuthorizeCard(card: String)

  case object PaymentAccepted

  case object PaymentDenied

  case class DispatchOrder(item: String)

  case object DispatchConfirmed

  case object DispatchDenied

  class CheckoutActor extends Actor {
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulFillmentManager = context.actorOf(Props[FulFillmentManager])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, card) => {
        paymentManager ! AuthorizeCard(card)
        context become pendingPayment(item)
      }
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted => fulFillmentManager ! DispatchOrder(item)
        context become pendingFulfilement(item)
      case PaymentDenied => throw new RuntimeException("Freak out")
    }

    def pendingFulfilement(item: String): Receive = {
      case DispatchConfirmed => context become awaitingCheckout
    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizeCard(card) => {
        if (card.startsWith("0"))
          sender ! PaymentDenied
        else
          sender ! PaymentAccepted
      }
    }
  }

  class FulFillmentManager extends Actor with ActorLogging {
    var orderId = 43

    override def receive: Receive = {
      case DispatchOrder(item) => {
        orderId += 1
        Thread.sleep(4000)
        log.info(s"Order $orderId for $item")
        sender ! DispatchConfirmed
      }
    }
  }

}
