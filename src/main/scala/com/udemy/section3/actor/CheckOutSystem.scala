package com.udemy.section3.actor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object CheckOutSystem extends App {

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
          log.info(s"Order $orderId for $item")
          sender ! DispatchConfirmed
        }
      }
    }

  val checkoutSystem = ActorSystem("checkout-system")

  val checkoutActor = checkoutSystem.actorOf(Props[CheckoutActor])

  checkoutActor ! Checkout( "Rock It", "40")

}
