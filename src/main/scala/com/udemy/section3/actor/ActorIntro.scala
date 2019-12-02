package com.udemy.section3.actor

import akka.actor.{Actor, ActorSystem, Props}

object ActorIntro extends App{
  // Actor System
  val actorSystem = ActorSystem("first-actor-system")
  println( actorSystem.name )


  //Create Actors
  // Word Count Actor
  class WordCountActor extends Actor {
    //internal data
    var totalWords = 0

    //behavior
    override def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[Word Counter] I have receieved message: $message")
        totalWords += message.split(" ").length
      case msg => println(s"I cannot understand")
    }
  }

  //Instantiate your counter
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val awordCounter = actorSystem.actorOf(Props[WordCountActor], "awordCounter")


  //Communicate with actor
  // ! is asynchronous also called as tell
  wordCounter ! "I am learning akka"
  awordCounter ! "Different word counter"

  // How to instatantiate actors with constructor

  class Person( name: String ) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi my name is $name")
      case _ =>
    }
  }
  object Person {
    def props(name : String) = Props( new Person(name))
  }
  // can call new Person within prop not used outside, legal but discouraged.
  val person = actorSystem.actorOf(Props(new Person("Bob")))
  person ! "hi"

  //Good way, use companion object to create props
  val personGood = actorSystem.actorOf(Person.props("Good Bob"))
  personGood ! "hi"
}

