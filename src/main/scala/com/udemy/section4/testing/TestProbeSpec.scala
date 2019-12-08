package com.udemy.section4.testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.udemy.section4.testing.TestProbeSpec.Master.{Register, RegistrationAck, Report, Work, WorkCompleted}
import com.udemy.section4.testing.TestProbeSpec.Slave.SlaveWork
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class TestProbeSpec extends TestKit(ActorSystem("testprobespec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import TestProbeSpec._

  "A master actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")// Fictious slave actor with assertion capabilities

      master ! Register( slave.ref )
      expectMsg( RegistrationAck )
    }
    "send the work to slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave") // Test probes
      master ! Register( slave.ref )
      expectMsg( RegistrationAck )

      val workload = "i love akka"
      master ! Work( workload )
      // testactor is implicitly passed, in prev step
      // testing the interaction between master and slace
      slave.expectMsg( SlaveWork(workload, testActor))
      slave.reply( WorkCompleted( 3, testActor ))

      expectMsg(Report(3))
    }
    "master actor should aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave") // Test probes
      master ! Register( slave.ref )
      expectMsg( RegistrationAck )
      val workload = "i love akka"
      master ! Work( workload )
      master ! Work( workload )
      // Assert that slavework  and programming of test probe.
      slave.receiveWhile() {
        case SlaveWork( `workload`, `testActor`) => slave.reply( WorkCompleted(3, testActor ))
      }
      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }
}

object TestProbeSpec {

  /* Word Counting actor hierarch */
  // - master sends the slave the work
  // - slave processes work and replies to master
  // - master aggregates the result
  // master sends the total count
  object Master {
    case class Register( slaveRef : ActorRef )
    case class Work( text: String )
    case class WorkCompleted( count:Int, requestor: ActorRef )
    case class Report( totalWc: Int )
    case object RegistrationAck
  }
  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) => context become online(slaveRef, 0)
        sender ! RegistrationAck
    }

    def online( slaveRef: ActorRef, wc: Int ): Receive = {
      case Work(text) => slaveRef ! SlaveWork( text, sender)
      case WorkCompleted(count, origRequestor) =>
        val totalWc = wc + count
        origRequestor ! Report( totalWc)
        context become online( slaveRef, totalWc )
    }
  }
  /** Don't care about this testing just the master */
  object Slave {
    case class SlaveWork( text: String, requestor: ActorRef )
  }
  class Slave extends Actor {
    override def receive: Receive = {
      case SlaveWork( text, requestor ) => {
        val wc = text.split(" ").size
        requestor ! WorkCompleted( wc, requestor )
      }
    }
  }
}
