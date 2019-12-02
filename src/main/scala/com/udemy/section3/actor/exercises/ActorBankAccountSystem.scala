package com.udemy.section3.actor.exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.udemy.section3.actor.exercises.ActorBankAccountSystem.Person.LiveTheLife

/**
 * - Deposit an amount
 * - Withdraw an amount
 * - Statement
 * Replies with
 * - Success
 * - Failure
 *
 * Interact with some other kind of actor
 */
object ActorBankAccountSystem extends App{

  val actorSystem = ActorSystem("bank-account-system")

  class AccountActor extends Actor {
    import AccountActor._
    var balance = 0
    override def receive: Receive = {
      case Deposit( amount ) => {
        if( amount > 0 ) {
          balance += amount
          sender ! Success(s"Transaction success - Deposit: $amount")
        } else {
          sender ! Failure(s"Transaction failure: Invalid amount: $amount")
        }
      }

      case WithDraw( amount ) => {
        if( amount > 0 ) {
          if (balance >= amount) {
            balance -= amount
            sender ! Success(s"Transaction success - Withdraw: $amount")
          } else {
            sender ! Failure(s"Transaction failure: Insufficient amount: $amount")
          }
        } else {
          sender ! Failure(s"Transaction failure: Invalid withdraw amount: $amount")
        }
      }
      case Success(msg) => println( msg )
      case Failure(msg) => println( msg )
      case Statement => {
        sender ! s"$self : Balance : $balance"
      }
    }
  }
  object AccountActor {
    case class Deposit( amount: Int )
    case class WithDraw( amount: Int )
    case object Statement
    case class Success( message: String)
    case class Failure( message: String)
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }
  class Person extends Actor{
    import AccountActor._
    override def receive: Receive = {
      case LiveTheLife(account) => {
        account ! Deposit( 100 )
        account ! WithDraw( 400 )
        account ! Deposit( 600 )
        account ! WithDraw( 400 )
        account ! Statement
      }
      case message => println( message.toString )
    }
  }
  val account = actorSystem.actorOf(Props[AccountActor], "account-actor")
  val person = actorSystem.actorOf(Props[Person], "person")
  person ! LiveTheLife( account ) // Causes Race Conditions
}
