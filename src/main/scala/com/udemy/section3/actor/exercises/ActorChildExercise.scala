package com.udemy.section3.actor.exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorChildExercise extends App {
  /**
   * 1. Distributed word counting
   *
   */
  val actorSystem = ActorSystem("hierarchy")

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }

  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(n) => {
        println(s"${self.path} - Master Initializing")
        val actorRefs: List[ActorRef] =
          1 to n map { i => context.actorOf(Props[WordCounterWorker], s"WordCounterWorker-$i") } toList

        context.become( initializedWorkers(actorRefs, 0, Map()) )
      }
    }

    def initializedWorkers(actorRefs: List[ActorRef], actorId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case WordCountTask( id, message ) => {
        actorRefs(actorId) ! WordCountTask( id, message )
        val newRequestMap = requestMap + ( id -> sender )
        context.become( initializedWorkers( actorRefs, (actorId + 1) % actorRefs.length, newRequestMap ))
      }
      case WordCountReply( id, n ) => {
        val originalSender = requestMap( id )
        println(s"${self.path} : Word count $id : $n")
        originalSender ! WordCountReply(id,  n )
        context become initializedWorkers( actorRefs, actorId, requestMap - id)
      }
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask( id, message ) => {
        val size = message.split(" ").size
        println(s"${self.path} : Worker counting the words $id : $size")
        sender() ! WordCountReply( id, size )
      }
    }
  }

  class TestActor extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountReply(id, n) => {
        println(s"${self.path} : Word count $id : $n")
      }
      case "test" => {
        import WordCounterMaster._

        val master = actorSystem.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize( 10 )
        for( i <- 1 to 99 ){
          master ! WordCountTask(i, "Create Master")
        }
      }
    }
  }
  /**
   * Create WordCounterMaster
   * Initialize 10 WorkerThreads
   * send "Akka is awesome" to master
   * Sends task to children -> replies with WordCountReply
   * MasterReplies with 3 with sender
   */

  val testActor = actorSystem.actorOf(Props[TestActor], "test")
  testActor ! "test"

}
