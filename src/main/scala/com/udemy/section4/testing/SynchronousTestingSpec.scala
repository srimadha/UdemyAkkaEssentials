package com.udemy.section4.testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import com.udemy.section4.testing.SynchronousTestingSpec.{CounterActor, Inc, Read}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends WordSpecLike with BeforeAndAfterAll {
  implicit val system = ActorSystem("synchronous-testing")

  override def afterAll(): Unit = {
    system.terminate()
  }

  "A counter" should {
    "synly increasing its counter" in {
      val counter = TestActorRef[CounterActor](Props[CounterActor])
      counter ! Inc
      assert( counter.underlyingActor.count == 1 )
    }
    "synly increase it counter at the call of recieve fundction" in {
      val counter = TestActorRef[CounterActor](Props[CounterActor])
      counter.receive(Inc)
      assert( counter.underlyingActor.count == 1 )
    }
    "work on the callin thread dispatcher" in {
      // makes it synchronous
      val ctr = system.actorOf(Props[CounterActor].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()
      probe.send( ctr, Read)
      probe.expectMsg(Duration.Zero, 0)
    }
  }
}

object SynchronousTestingSpec {
  case object Inc
  case object Read
  class CounterActor extends Actor {
    var count = 0
    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender ! count
    }
  }
}