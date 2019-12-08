/*
1. OOP encapsulation is only valid single thread environment
 */
class BankAccount(private var amount: Int) {
  override def toString: String = "" + amount
  def getAmount = this.amount
  //Not Thread safe
  def withdraw( money: Int) =
    this.amount -= money

  def deposit( money: Int) =
    this.amount += money

}

val account = new BankAccount(2000)

/*for( _ <- 1 to 1000){
  new Thread(() => account.withdraw(1)).start()
}

for( _ <- 1 to 1000){
  new Thread(() => account.deposit(1)).start()
}*/

println( account.getAmount )

// OOP encapsulation is broken in a multiThreaded env
// Synchronization to rescue, but adds complexity, deadlocks as problem statement complicates

// We would need a data structure fully encapsulated, with no locks


/*
2. delegating something to a thread is a PAIN

 */
var task: Runnable = null
val runningThread: Thread = new Thread( () => {
  while(true){
    while( task == null){
      runningThread.synchronized {
        println("BG: Waiting for a task")
        runningThread.wait()
      }
    }

    task.synchronized{
      println("BG: I have a task")
      task.run()
      task = null
    }
  }
})

def delegateToBackGround( r: Runnable ) = {
  if( task == null )
    task = r
  runningThread.synchronized{
    runningThread.notify
  }
}

runningThread.start
Thread.sleep(500)

delegateToBackGround( ()=> println(42))
Thread.sleep(1000)
delegateToBackGround( ()=> println("This should run in background"))

/**
 * This can get very complicated with more complex scenarios
 *
 * We need a datastructure
 *  can safely recieve messages
 *  can identify the sender
 *  is easily identifyable
 *  can gaurd against errors
 *
 */

/*
3. Tracing errors in multi threaded env is hard
 */

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
val future = (0 to 9).map( i => 100000 *i until 100000 * ( i+1))
.map( range => Future {
  if( range.contains(23456) ) throw new RuntimeException("Invalid number")
  range.sum
})

val sumFuture = Future.reduceLeft( future )( _ + _ )
sumFuture.onComplete( println )