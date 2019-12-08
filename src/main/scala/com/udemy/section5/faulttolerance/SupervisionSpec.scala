package com.udemy.section5.faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SupervisionSpec extends
  TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  import SupervisionSpec._

  "An all for one supervisor" should {
    "apply all for one strategy" in {
      val superVisor = system.actorOf(Props[AllForOneSupervisor], "allForOne")
      superVisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      superVisor ! Props[FussyWordCounter]
      val child2 = expectMsgType[ActorRef]

      child2 ! "Akka is cool"
      child2 ! Report
      expectMsg(3)


      EventFilter[NullPointerException]() intercept {
        child ! ""
      }
      Thread.sleep( 500 )
      child2 ! Report
      expectMsg( 0 )

    }
  }
  "A kinder supervisor" should {
    "not kill children in case of restart" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "Akka is cool"
      child ! Report
      expectMsg(3)
      child ! 45
      child ! Report
      expectMsg(0)


    }
  }
  "A supervisor" should {
    "resume the child in case of minor fault" in {
      val supervisor = system.actorOf(Props[SupervisorActor])
      supervisor ! Props[FussyWordCounter]

      val child = expectMsgType[ActorRef]

      child ! "I love akka"
      child ! Report
      expectMsg(3)

      child ! "I love akka I love akka I love akka I love akka I love akka I love akka I love akka" //Runtime
      child ! Report
      expectMsg(3)

      child ! "" //Null
      child ! Report
      expectMsg(0) // restart

      child ! "I love akka"
      child ! Report
      expectMsg(3)

      watch(child)
      child ! "i love akka" // Should stop the child
      val terminatedMsg = expectMsgType[Terminated]
      assert(terminatedMsg.actor == child)
    }

    "Supervisor should escalate when all doors are closed" in {
      val supervisor = system.actorOf(Props[SupervisorActor])
      supervisor ! Props[FussyWordCounter]

      val child = expectMsgType[ActorRef]

      child ! "I love akka"
      child ! Report
      expectMsg(3)

      watch(child)
      child ! 43
      val terminatedMsg = expectMsgType[Terminated]
      assert(terminatedMsg.actor == child)

    }
  }

}

object SupervisionSpec {

  class AllForOneSupervisor extends SupervisorActor {
    override val supervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  class SupervisorActor extends Actor {

    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends SupervisorActor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {

    }
  }

  case object Report

  class FussyWordCounter extends Actor {
    var words = 0

    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("Sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("Sentence is too big ")
        else if (!Character.isUpperCase(sentence(0)))
          throw new IllegalArgumentException("Sentence must start with uppercase")
        else words += sentence.split(" ").length
      case _ => throw new Exception("I can only work with strings")
    }
  }

}