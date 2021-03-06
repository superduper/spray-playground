package com.alexb.calculator

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import spray.testkit._
import org.scalatest._
import com.alexb.main.context.ActorSystemContext
import language.postfixOps

class CalculatorServiceSpec extends WordSpec with MustMatchers with ScalatestRouteTest
  with CalculatorService with CalculatorServiceContext with ActorSystemContext {

	val timeout = Timeout(5 seconds) // needed for `?`
	def actorSystem: ActorSystem = system
	def actorRefFactory = system

	"Calculator service" must {
		"do right additions" in {
			Get("/calculator/add/35/7.2") ~> calculatorRoute ~> check {
				entityAs[CalculatorResult] must be (SuccessResult(42.2))
			}
		}
	}
}
