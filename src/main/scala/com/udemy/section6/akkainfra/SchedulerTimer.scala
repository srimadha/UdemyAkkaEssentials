package com.udemy.section6.akkainfra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._

object SchedulerTimer extends App {
class SimpleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case message => log.info( message.toString )
  }
}
  val system = ActorSystem("SchedulerTimerDemo")

  val simpleActor = system.actorOf(Props[SimpleActor])

 // system.log.info("Scheduling reminder")

  //implicit val executionContext = system.dispatcher
  import system.dispatcher // samething
  system.scheduler.scheduleOnce(1 second){
    // simpleActor ! "reminder" // Message is sent after 1 second
  } // (system.dispatcher)

  val routine: Cancellable = system.scheduler.schedule(1 second, 2 second){
    // simpleActor ! "beat"
  }

  system.scheduler.scheduleOnce(10 seconds){
    routine.cancel() // cancellable is cancelled after 10 sec
  }

  /**
   * Implement self closing actor
   *
   * - if the actor recieves message, you have 1 second to send another message
   * - if the timewindow expires the actor will stop itself
   * - if you send another message time is reset
   */

  class SelfClosingActor extends Actor with ActorLogging {
    def createTimeOutWindow: Cancellable = {
      context.system.scheduler.scheduleOnce( 1 seconds ){
        self ! "timeout"
      }
    }
    var schedule = createTimeOutWindow
    override def receive: Receive = {
      case "timeout" =>
        log.info( "stopping myself")
        context.stop(self)
      case message =>
        log.info(s"Recieved message: ${message.toString}")
        schedule.cancel()
        schedule = createTimeOutWindow
    }
  }
  val selfClosingActor = system.actorOf(Props[SelfClosingActor])
  system.scheduler.scheduleOnce(250 millis){
    //selfClosingActor ! "ping"
  }

  system.scheduler.scheduleOnce( 2 seconds){
  //  system.log.info("Sending pong")
   // selfClosingActor ! "pong"
  }

  /**
   * Timer -> safer ways to schedule messages from within actor
   */

  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedHeartBeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1000 millis)
      case Reminder =>
        log.info("Im alive")
      case Stop =>
        log.warning("Stopping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }
  val timerHeartBeatActor = system.actorOf(Props[TimerBasedHeartBeatActor], "timerActor")

  system.scheduler.scheduleOnce(5 seconds){
    timerHeartBeatActor ! Stop
  }
}
