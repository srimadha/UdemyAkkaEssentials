import scala.concurrent.Future

val partialFunction: PartialFunction[Int, Int] = {
  case 1 => 1
  case 2 => 2
}

val lifted = partialFunction.lift // total function Int => Option[Int]

lifted(1) // Some(1)
lifted(3) // None

val pfChain = partialFunction.orElse[Int, Int]{
  case 3 => 3
}

pfChain(1)
pfChain(3)
//pfChain(4) // Match Error

//Type Aliases
type RecieveFunction = PartialFunction[Any, Unit]

def recieve: RecieveFunction = {
  case 1 => println("One")
  case _ => println("I know nothing")
}

recieve( 1 )
recieve( 2 )

//implicits
implicit val timeOut = 10

def setTimeOut(f: () => Unit)(implicit timeOut: Int) = {
  println( timeOut )
  f()
}

setTimeOut(() => println("Hello")) // timeout value is passed implicitly
setTimeOut(() => println("There"))(200)

//implicit conversions

// With implicit defs
case class Person( name: String){
  def greet = s"Hi $name"
}

Person("Sri").greet

implicit def fromStringToPerson(name: String): Person = Person(name)

"Megh".greet

// with implicit classes
implicit class Dog( name: String){
  def bark = println(s"$name -> Bark")
}

"Lassie".bark

//Implicits can be confusing without organizing

import scala.concurrent.ExecutionContext.Implicits.global
val future = Future  {
  println("Hello Future")
}
//Implicit Ordering
//1. Companion Object
//2. Local Scope
//3. Global scope
