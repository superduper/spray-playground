package com.alexb.service

import cc.spray.Directives
import cc.spray.directives.DoubleNumber
import cc.spray.typeconversion.SprayJsonSupport
import akka.pattern.{ ask }
import akka.util.Timeout
import akka.util.duration._
import akka.actor.{ ActorSystem, Props, ActorRef }
import cc.spray.json.DefaultJsonProtocol

trait CalculatorService
	extends Directives
	with SprayJsonSupport
	with DefaultJsonProtocol
	with DoubleResultProtocol {

	implicit val timeout = Timeout(5 seconds) // needed for `?` below

	implicit val calculator: ActorRef

	val calculatorService = {
		path("add" / DoubleNumber / DoubleNumber) { (a, b) =>
			get {
				val cmd = AddCommand(a, b)
				actorSystem.eventStream.publish(cmd)
				completeWith((calculator ? cmd).mapTo[Double])
			}
		} ~ 
		path("subtract" / DoubleNumber / DoubleNumber) { (a, b) =>
			get {
				completeWith((calculator ? SubtractCommand(a, b)).mapTo[Double])
			}
		} ~ 
		path("divide" / DoubleNumber / DoubleNumber) { (a, b) =>
			get {
				completeWith((calculator ? DivideCommand(a, b)).mapTo[Either[Double, ArithmeticException]])
			}
		}
	}
}
