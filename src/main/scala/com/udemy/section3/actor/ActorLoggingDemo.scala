package com.udemy.section3.actor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {

  class SimpleActorWithExplicitLogger extends Actor {
    val logger = Logging( context.system, this )

    override def receive: Receive = {
      /*
        debug, info, warning, error
       */
      case message =>
        logger.info( message.toString )
    }
  }

  val actorSystem = ActorSystem( "ActorLoggerSystem")
  val actor = actorSystem.actorOf( Props[SimpleActorWithExplicitLogger], "SimpleExplicitLogger")

  actor ! "Logging a simple message"

  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case ( a, b) => log.info("Two parameters: {} and {}", a, b);
      case message => log.info(message.toString)
    }
  }

  val simpleActor = actorSystem.actorOf( Props[ActorWithLogging], "SimpleLogger")

  simpleActor ! "Logging a simple message"
  simpleActor ! ("Lebron", "Janes")
}
