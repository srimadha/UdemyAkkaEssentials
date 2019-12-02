package com.udemy.section3.actor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehavior extends App {

  val actorSystem = ActorSystem("change-actor-behavior")

  object FussyKid {
    case object KidAccept
    case object KidTantrum
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class FussyKid extends Actor {

    import FussyKid._
    import Mom._

    var state = HAPPY

    override def receive: Receive = {
      case Food(VEG) => state = SAD
      case Food(CHOC) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) { /** This logic can become very messy */
          sender ! KidAccept
        } else {
          sender ! KidTantrum
        }
    }
  }

  object Mom {
    case class MomStart(kid: ActorRef)
    case class Food(food: String)
    case class Ask(message: String)
    val VEG = "veg"
    val CHOC = "choc"
  }
  class StateLessFussyKid extends Actor { // Better way of handling state
    import FussyKid._
    import Mom._
    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEG) => context become ( sadReceive, false ) // With false, receive handlers is maintained in in stack, stack push
      case Food(CHOC) =>
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEG) => context become ( sadReceive, false )
      case Food(CHOC) => context unbecome // stack.pop
      case Ask(_) => sender() ! KidTantrum
    }
  }

  class Mom extends Actor {

    import Mom._
    import FussyKid._

    override def receive: Receive = {
      case MomStart(kid) => {
        kid ! Food(VEG)
        kid ! Food(VEG)
        kid ! Food(CHOC)
        kid ! Food(CHOC)
        kid ! Ask("..")
      }
      case KidAccept => println("Yayy! Kid is happy")
      case KidTantrum => println("My kid is sad, atleast he is healthy")
    }
  }

  import Mom._

  val momActor = actorSystem.actorOf(Props[Mom], "mom-actor")
  val kidActor = actorSystem.actorOf(Props[FussyKid], "kid-actor")
  momActor ! MomStart(kidActor)

  val statelessKidActor = actorSystem.actorOf(Props[StateLessFussyKid], "stateless-kid-actor")
  momActor ! MomStart(statelessKidActor)
}
