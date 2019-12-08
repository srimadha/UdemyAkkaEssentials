package com.udemy.section4.testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class TimedAssertionSpec extends TestKit(
  ActorSystem("TimedSpec", ConfigFactory.load().getConfig("specialTimedAssestion")))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import TimedAssertionSpec._

  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])
    "reply with meaning of life in timely manner" in {
      within(500 millis, 1 second) { // Timed box test
        workerActor ! "work"
        expectMsg(WorkerResult(42))
      }
    }
    "reply with valid word at a reasonable cadence" in {
      within( 1 second ){
        workerActor ! "worksequence"
        val results = receiveWhile[Int]( max = 2 second, idle = 500 millisecond, 10){
          case WorkerResult(result) => result
        }

        assert( results.sum > 5 )
      }
    }
    "reply to test probe in a timely manner" in {
      within( 1 second ) {
        val probe = TestProbe()
        probe.send(workerActor, "work")
        probe.expectMsg(WorkerResult(42)) // timeout of 0.3s, failure because of probe
      }
    }
  }
}


object TimedAssertionSpec {

  case class WorkerResult(i: Int)

  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" => //Hard Computation
        Thread.sleep(500)
        sender ! WorkerResult(42)
      case "worksequence" =>
        val r = new Random()
        for(i <- 1 to 10 ){
          Thread.sleep( r.nextInt(50) )
          sender() ! WorkerResult( 1 )
        }
    }
  }

}
