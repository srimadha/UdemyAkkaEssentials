package com.udemy.section4.testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.udemy.section4.testing.BasicSpec.{BlackHole, LabTestActor, SimpleActor}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random
class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    // system is a member of test kit
    TestKit.shutdownActorSystem(system)
  }

  "A simple actor" should { // Test suite
    "send back the same message" in { // tests
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello test"
      echoActor ! message
      expectMsg( message )
    }
  }

  "A blackholde actor" should { // Test suite
    "send back the same message" in { // tests
      val echoActor = system.actorOf(Props[BlackHole])
      val message = "hello test"
      echoActor ! message
      // akka.test
      expectNoMessage( 2 second ) //expect message fails after default timeout
      // testactor is passed implicitly ( because ImplicitSender
    }
  }

  "A labtest actor" should { // Test suite
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn the string in uppercase" in { // tests
      val message = "hello test"
      labTestActor ! message
      val reply = expectMsgType[String]

      assert( reply == "HELLO TEST")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }

    "reply with fav tech" in {
      labTestActor ! "favtech"
      expectMsgAllOf("scala", "akka")
    }

    "reply with fav tech in differnt way" in {
      labTestActor ! "favtech"
      val messages = receiveN(2)
      // free to do more assertion
    }
    "reply with cool fav tech in assertion" in {
      labTestActor ! "favtech"
      expectMsgPF() {
        case "scala" =>
      }
    }

  }
}

object BasicSpec {

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender ! message
    }
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" => {
        if( random.nextBoolean())
          sender ! "hi"
        else
          sender ! "hello"
      }
      case "favtech" => {
        sender ! "scala"
        sender ! "akka"
      }
      case message: String => sender ! message.toUpperCase()
    }
  }

}