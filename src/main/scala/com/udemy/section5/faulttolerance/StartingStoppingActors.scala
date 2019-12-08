package com.udemy.section5.faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}
object StartingStoppingActors extends App {

  val system = ActorSystem("StoppingActorDemo")

  object Parent {
    case class StartChild( name: String )
    case class StopChild( name: String )
    case object Stop
  }
  class Parent extends Actor with ActorLogging {
    override def receive: Receive = withChildren(Map())
    import Parent._
    def withChildren(map: Map[String, ActorRef]) : Receive = {
      case StartChild( name ) => {
        log.info( s"Starting child with $name")
        val childActor = context.actorOf(Props[Child], name)
        val newMap = map + ( name -> childActor)
        context become withChildren( newMap )
      }
      case StopChild( name ) => {
        log.info( s"Stopping child with name $name")
        val childOption = map.get(name)
        childOption.foreach( childRef => {
          context.stop(childRef) // Asynchronous
        })
      }
      case Stop =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info( message.toString)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info( message.toString )
    }
  }

  /**
   * Method 1 => context.stop
   */
  import Parent._

  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild( "child1")

  val child = system.actorSelection("/user/parent/child1")
  child ! "Hi"

  parent ! StopChild("child1")
  //for( _ <- 1 to 50) child ! "are u there"

  parent ! StartChild("child2")
  val child2 = system.actorSelection("/user/parent/child2")
  child2 ! "Hi 2"
  parent ! Stop
  for( i <- 1 to 10) parent ! s"parent are you still there: $i"
  for( i <- 1 to 100) child2 ! s"are u there $i"


  /**
   * Method 2 - using special message
   */

  val looseActor = system.actorOf(Props[Child])
  looseActor ! "Hello, loose actor"
  looseActor ! PoisonPill // cannot handle them in your own receiver
  looseActor ! "Are you still there"

  val killAcort = system.actorOf(Props[Child])
  killAcort ! "Hello, loose actor"
  killAcort ! Kill // Its more brutal, it makes actor throw  KillException
  killAcort ! "Are you still there"

  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching $name")
        context.watch(child) //when child dies this actor recieves notification
      case Terminated(ref) => {
        log.info(s"The reference that i'm watching $ref, stopped")
        context.unwatch(ref) // no need to watch this actor anymore
      }
    }
  }
  val watherActor = system.actorOf(Props[Watcher], "watcher")
  watherActor ! StartChild("watched")

  val watched = system.actorSelection("/user/watcher/watched")
  Thread.sleep(500)
  watched ! PoisonPill
}
