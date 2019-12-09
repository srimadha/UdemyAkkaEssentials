package com.udemy.section6.akkainfra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Routers extends App {

  /*
  1. Manual Router
   */
  class Master extends Actor with ActorLogging {
    // 1. create routees
    private val slaves =
      for (i <- 1 to 5) yield {
        val slave = context.actorOf(Props[Slave], s"Slave$i")
        context.watch(slave)
        ActorRefRoutee(slave)
      }

    // 2 define router
    private val router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {
      // 3. route the message
      case message =>
        router.route(message, sender)
      // 4. Handle the termination
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }

  val system = ActorSystem("RotersDemo",
    ConfigFactory.load().getConfig("routersDemo"))
  val masterActor = system.actorOf(Props[Master], "master")

  /*for(i <- 1 to 10) {
    masterActor ! s"$i : hello from the world"
  }*/

  /**
   * 2. Router actor with its own children
   * POOL router
   */
    // 2.1 Programatically
  val poolMaster = system.actorOf(RoundRobinPool(3).props(Props[Slave]))
  for(i <- 1 to 10) {
    // poolMaster ! s"$i : hello from the world"
  }
  // 2.2, from configuration
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  /*for(i <- 1 to 10) {
    poolMaster2 ! s"$i : hello from the world"
  }*/

  /**
   * 3. Routers with actors created elsewhere
   * Group Router
   */
  // in another part of my application

  val slaveList = ( 1 to 5 ).map( i => system.actorOf(Props[Slave], s"slave$i")).toList

  val slavePaths = slaveList.map(_.path.toString)

  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
  /*for(i <- 1 to 10) {
    groupMaster ! s"$i : hello from the world"
  }*/

  //3.2 From config
  val groupMaster2 = system.actorOf(FromConfig.props(), "groupmaster2")
  for(i <- 1 to 10) {
    groupMaster2 ! s"$i : hello from the world"
  }

  /**
   * Special Message
   */
  groupMaster2 ! Broadcast("Hello Everyone")

  // PoisonPill and kill or not routed
  // AddRoutee, Remove, Get handled only by the routed actor
}
