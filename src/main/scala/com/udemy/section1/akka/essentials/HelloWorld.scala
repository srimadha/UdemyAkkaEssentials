package com.udemy.section1.akka.essentials

import akka.actor.ActorSystem

object HelloWorld extends App {
  val actorSystem = ActorSystem("HelloAkka")
  println(actorSystem.name)
}
