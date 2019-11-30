import scala.concurrent.Future
import scala.util.{Failure, Success}

val aThread = new Thread( new Runnable() {
  override def run() = println( "Im running in parallel")
})

aThread.start
aThread.join

val sugarThread = new Thread( ()=> {
  println("Syntax sugar")
})

sugarThread.start
sugarThread.join

class BankAccount(private var amount: Int) {
  override def toString: String = "" + amount

  //Not Thread safe
  def withdraw( money: Int) = this.amount -= money

  //Thread safe
  def safeWithdray( money: Int) = this.synchronized{
    this.amount -= money
  }
}

// Inter thread communication
// Done using wait and notify
import scala.concurrent.ExecutionContext.Implicits.global
// Scala Futures
val future = Future {
  42
}

future.onComplete{
  case Success(42) => println("Found meaning")
  case Failure(_) => println("No meaning")
}

val aProcessedFuture = future.map( _ + 1)
val aFlatFuture = future.flatMap{ value => {
  Future( value + 2)
}}

val filterFuture = future.filter( _ % 2 == 0)

val aNonSenseFuture = for {
  meaningOfLife <- future
  filteredFuture <- filterFuture
} yield meaningOfLife + filteredFuture

aNonSenseFuture.onComplete{
  case Success(_) => {
    println("Complete")
  }
}

//andThen, recover/recoverWith