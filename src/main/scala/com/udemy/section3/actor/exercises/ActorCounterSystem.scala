package com.udemy.section3.actor.exercises

import akka.actor.{Actor, ActorSystem, Props}
/**
 * counter actor
 * increment
 * decrement
 * print
 */
object ActorCounterSystem extends App {

  val actorSystem = ActorSystem("actor-counter-system")

  class CounterActor extends Actor {
    import CounterActor._
    var counter = 0;
    override def receive: Receive = {
      case Increment => {
        counter += 1;
        println(s"$self : Increment by 1 : New Value :$counter")
      }
      case Decrement=> {
        counter -= 1;
        println(s"$self : Decrement by 1 : New Value :$counter")
      }
      case Print => {
        println(s"$self : Value : $counter")
      }
    }
  }
  object CounterActor {
    case object Increment
    case object Decrement
    case object Print

  }
  import CounterActor._
  val counterActor = actorSystem.actorOf(Props[CounterActor], "counter-actor")

  ( 1 to 5 ).foreach(_ => counterActor ! Increment)
  ( 1 to 3 ).foreach(_ => counterActor ! Decrement)
  counterActor ! Print

}
