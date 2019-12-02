package com.udemy.section3.actor.exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorChangeActorBehavior extends App {
  /**
   * 1. Recreate CounterActor with context become and no mutable state
   */
  val actorSystem = ActorSystem("actor-counter-system")

  class CounterActor extends Actor {
    import CounterActor._

    override def receive: Receive = countReceive( 0 )
    def countReceive( counter: Int ): Receive = {
      case Increment => {
        context.become( countReceive( counter + 1) )
        println(s"$self : Increment by 1 : New Value :${counter+1}")
      }
      case Decrement=> {
        context.become( countReceive( counter - 1) )
        println(s"$self : Decrement by 1 : New Value :${counter-1}")
      }
      case Printer => {
        println(s"$self : Value : $counter")
      }
    }
  }
  object CounterActor {
    case object Increment
    case object Decrement
    case object Printer
  }

  import CounterActor._
  val counterActor = actorSystem.actorOf(Props[CounterActor], "counter-actor")

  ( 1 to 5 ).foreach(_ => counterActor ! Increment)
  ( 1 to 3 ).foreach(_ => counterActor ! Decrement)
  counterActor ! Printer

  /**
   *  2. Simplified voting system
   *
   */
  case class Vote( candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply( candidate: Option[String])
  class Citizen extends Actor {

    override def receive: Receive = {
      case Vote(candidate) => {
        context.become( voted(Some(candidate )))
      }
      case VoteStatusRequest => {
        sender() ! VoteStatusReply( None )
      }
    }
    def voted( candidate: Option[String]): Receive = {
      case VoteStatusRequest => sender ! VoteStatusReply( candidate )
    }
  }

  case class AggregateVotes( citizens : Set[ActorRef])
  case object Print
  class VoteAggregator extends Actor {

    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {
      case AggregateVotes( citizens ) => {
        citizens.foreach( _ ! VoteStatusRequest )
        context.become(awaitingStatus(citizens, Map()))
      }
    }
    def awaitingStatus(stillWaiting: Set[ActorRef], stats: Map[String, Int]): Receive = {
      case VoteStatusReply(None) => sender ! VoteStatusRequest
      case VoteStatusReply( c ) => {
        val newStillWaiting = stillWaiting - sender()
        val currentVotes = stats.getOrElse( c.get, 0 )
        val newStats = stats + ( c.get -> (currentVotes + 1))
        if( newStillWaiting.size == 0 ){
          println( newStats )
        } else {
          context.become(awaitingStatus(newStillWaiting, newStats))
        }
      }
    }
  }

  val system = ActorSystem( "voting-system")
  val alice = system.actorOf(Props[Citizen], "alice")
  val bob = system.actorOf(Props[Citizen], "bob")
  val charlie = system.actorOf(Props[Citizen], "charlie")
  val daniel = system.actorOf(Props[Citizen], "daniel")

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator], "vote-agg")
  voteAggregator ! AggregateVotes( Set(alice, bob, charlie, daniel))
}
