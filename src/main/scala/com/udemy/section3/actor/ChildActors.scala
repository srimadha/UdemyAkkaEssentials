package com.udemy.section3.actor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.udemy.section3.actor.ChildActors.CreditCard.{AttachToAcct, CheckStatus}

object ChildActors extends App {
  // Actors can create other actors
  object Parent {
    case class CreateChild(name: String)
    case class TellChild( message: String )
  }
  class Parent extends Actor{
    import Parent._

    override def receive: Receive = {
      case CreateChild( name ) =>
        println( s"${self.path} Creating child $name")
        val childRef = context.actorOf(Props[Child], name)
        context become withChild( childRef )

    }
    def withChild(child: ActorRef): Receive = {
      case TellChild( message ) =>
          child forward message
          //child.tell(message, sender) Same as above
    }

  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got $message from $sender()")
    }
  }

  val actorSystem = ActorSystem("ParentChildDemo")
  val parentActor = actorSystem.actorOf(Props[Parent], "parent")
  import Parent._
  parentActor ! CreateChild("child")
  parentActor ! TellChild("Hey Kid")

  /*
  Guardian Actors ( Top Level )
  - /system = system guardian ( manages actors akka create
  - /user = user-level guardian ( manages actors we create )
  - / = root guardian
   */

  /**
   * Actor Selection
   */
  val childSelection = actorSystem.actorSelection("/user/parent/child")
  childSelection ! "I found the child Actor"

  /**
   * Danger
   *
   * Never pass mutable actor state, or the this reference to child actors
   */
  object NaiveBankAcct {
    case class Deposit(amt: Int)
    case class WithDraw(amt: Int)
    case object InitializeAcct
  }
  class NaiveBankAcct extends Actor {
    import NaiveBankAcct._
    import CreditCard._
    var amount = 0
    override def receive: Receive = {
      case InitializeAcct =>
        val creditRef = context.actorOf(Props[CreditCard], "card")
        creditRef ! AttachToAcct( this ) // !!!!! Should never ever do it
      case Deposit(funds) => deposit(funds)
      case WithDraw(funds) => withdraw(funds)

    }
    def deposit(funds: Int) = {
      println(s"${self.path} Depositing $funds on $amount")
      amount += funds
    }
    def withdraw(funds: Int) = {
      println(s"${self.path} WithDrawing $funds on $amount")
      amount -= funds
    }
  }
  object CreditCard {
    case class AttachToAcct( bankAccount: NaiveBankAcct) // !!! Very wrong should use ActorRef instead
    case object CheckStatus
  }
  class CreditCard extends Actor {
    override def receive: Receive = {
      case AttachToAcct(acct) =>
        context become attachedTo( acct )
    }

    def attachedTo(acct: ChildActors.NaiveBankAcct): Receive = {
      case CheckStatus => println(s"${self.path} Message processed")
      acct.withdraw(1) // This is the problem, calling actor methods directly// Bypassing all the actor
    }
  }
  import NaiveBankAcct._
  import CreditCard._
  val bankAcctRef = actorSystem.actorOf(Props[NaiveBankAcct], "account")
  bankAcctRef ! InitializeAcct
  bankAcctRef ! Deposit(100)
  Thread.sleep(500)
  val ccSelection = actorSystem.actorSelection("/user/account/card")
  ccSelection ! CheckStatus
  // Never close over
}
