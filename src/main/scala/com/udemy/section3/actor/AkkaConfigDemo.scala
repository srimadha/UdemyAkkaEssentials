package com.udemy.section3.actor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object AkkaConfigDemo extends App {

  /**
   * 1. Inline Configuration
   */

    val configString =
      """
        | akka {
        |   loglevel = "INFO"
        | }
        |""".stripMargin
  val config = ConfigFactory.parseString( configString )
  val actorSystem = ActorSystem("AkkaConfigDemo", ConfigFactory.load(config))

  class LoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => {
        log.info(message.toString)
      }
    }
  }

  val simpleActor = actorSystem.actorOf(Props[LoggingActor])
  // simpleActor ! "Hello"
  /**
   * 2. Picks up config from application.conf
   */
  val actorSystemWithAppConfig = ActorSystem("AkkaConfigWithConfigDemo")
  val defaultConfigActor = actorSystemWithAppConfig.actorOf(Props[LoggingActor])
  //defaultConfigActor ! "Remember Me!"

  /**
   * 3. Special Config
   */
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialActorSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor = specialActorSystem.actorOf(Props[LoggingActor])
  specialConfigActor ! "Remember Me!"

  /**
   * 4. Seperate config from another file
   */
  val seperateConfig = ConfigFactory.load("akkaconfig/special-actor.conf")
  val seperateConfigActorSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val seperateConfigActorSystemActor = seperateConfigActorSystem.actorOf(Props[LoggingActor])
  seperateConfigActorSystemActor ! "Remember Me!"
  println( s"Separate Conf : ${seperateConfig.getString("akka.loglevel")}")

  val jsonConfig = ConfigFactory.load("json/jsonconfig.json")
  println( s"Json Config  : ${jsonConfig.getString("akka.loglevel")}")

  val propsConfig = ConfigFactory.load("props/propsconfig.properties")
  println( s"Props Config  : ${propsConfig.getString("akka.loglevel")}")


}
