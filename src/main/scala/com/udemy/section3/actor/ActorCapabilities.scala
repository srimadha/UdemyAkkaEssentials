package com.udemy.section3.actor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    // receive is a partial function
    override def receive: Receive = {
      case "Hi" => {
        println( s"[Simple Actor:${self}]: I have received Hi")
        context.sender() ! "Hello There"
      } // Replying to sender
      case message: String => println( s"[Simple Actor:${self}]: I have received $message")
      case number: Int => println(s"[Simple Actor]: I have received $number")
      case specialMessage: SpecialMessage => println(s"[Simple Actor]: I have received something special $specialMessage")
      case SendMessageToYourSelf( content ) => self ! content
      case SayHiTo(ref) => ref ! "Hi"
      case WirelessMessage(content, ref) => ref forward( content+"s")
    }
  }

  val actorSystem = ActorSystem("actor-capabilities")

  val simpleActor = actorSystem.actorOf(Props[SimpleActor], "simpleActor")

  // 1. Messages can be of any type
  // messages must be implemented in Immutable
  // messages must be Serializable
  simpleActor ! "Hello Simple Actor"
  simpleActor ! 42 // Who is the sender actually null

  case class SpecialMessage ( content: String )
  simpleActor ! SpecialMessage("Special Message")

  // 2. Actors have information about context and about themselves
  // context.self is equivalent of this

  case class SendMessageToYourSelf( content : String)
  simpleActor ! SendMessageToYourSelf( "I'm an actor and proud of it")

  // 3. Actors can reply to messages
  val alice = actorSystem.actorOf( Props[SimpleActor], "alice")
  val bob = actorSystem.actorOf( Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  //4. Deadletter
  alice ! "Hi" //Deadletters fake actor

  //5. Forwarding messages
  case class WirelessMessage( content : String, ref: ActorRef)
  alice ! WirelessMessage("Hi Wireless", bob)




}
