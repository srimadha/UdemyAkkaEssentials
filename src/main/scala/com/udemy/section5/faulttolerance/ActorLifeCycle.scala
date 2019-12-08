package com.udemy.section5.faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifeCycle extends App {

  object StartChild
  class LifeActor extends Actor with ActorLogging {
    override def preStart(): Unit =  log.info("I am starting")

    override def postStop(): Unit = log.info("I have stopped")
    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifeActor], "child")
    }
  }

  val sys = ActorSystem("LifeCycleDemo")
  /*val parent = sys.actorOf(Props[LifeActor], "parent")
  parent ! StartChild
  parent ! PoisonPill*/

  /**
   * Restart
   */

  val supervisor = sys.actorOf(Props[ParentActor], "supervisor")
  supervisor ! FailChild

  class ParentActor extends Actor with ActorLogging {
    private val child = context.actorOf(Props[ChildActor], "ParentActor")

    override def receive: Receive = {
      case FailChild => child ! Fail
    }
  }
  object Fail
  object FailChild
  class ChildActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case Fail =>
        log.warning("Child will fail now")
        throw new RuntimeException("I failed")
    }

    override def preStart(): Unit = log.info("Supervised child started")

    override def postStop(): Unit = log.info("Supervised child stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"Supervised actor restarting because of ${reason.getMessage}")
    }

    override def postRestart(reason: Throwable): Unit = {
      log.info(s"Supervised actor restarted : ${reason.getMessage}")
    }
  }
  //Supervision strategy

}
