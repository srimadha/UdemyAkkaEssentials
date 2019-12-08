package com.udemy.section5.faulttolerance

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffSupervisor}

import scala.concurrent.duration._
import scala.io.Source

object BackOffSuperVisorPattern extends App {

  val system = ActorSystem("BackOffSupervisorDemo")

  //val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
  //simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackOffActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

  //val simpleSupervisor = system.actorOf(simpleSupervisorProps)

  /**
   * Supervision strategy is the default one ( restarting )
   *  -> first attempt after 3 seconds
   *  -> next attempt is 2x the previous attempt
   *  -> 0.2 adds randomness
   */
  //simpleSupervisor ! ReadFile

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FileBasedPersistentActor],
      "stopBackOffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy( OneForOneStrategy() {
      case _ => Stop
    })
  )

  //val stopSupervisorActor = system.actorOf(stopSupervisorProps, "StopSupervisor")
  //stopSupervisorActor ! ReadFile

  class EagerFileBasedPersistentActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("EagerFileBasedPersistentActor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/important1.txt"))
    }
  }

  //val eagorActor = system.actorOf(Props[EagerFileBasedPersistentActor])

  val repeatedSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[EagerFileBasedPersistentActor],
      "repeatedBackOffActor",
      1 seconds,
      30 seconds,
      0.1
    )
  )

  val repeatedeagorActor = system.actorOf(repeatedSupervisorProps, "repeatedEagerActor")
  // restarts after 1, 2, 4, 8, 16 seconds and then dies of because 32 > 30
  case object ReadFile
  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit = {
      log.info("Persistent actor starting")
    }

    override def postStop(): Unit = {
      log.warning("Persistent actor has stopped")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.warning("Persistent actor restarting")
    }

    override def receive: Receive = {
      case ReadFile =>
        if( dataSource == null)
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/important1.txt"))
        log.info("I've just read some data : " + dataSource.getLines().toList)
    }
  }

}
