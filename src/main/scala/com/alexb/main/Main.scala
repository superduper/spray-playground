package com.alexb.main

import akka.actor.{ actorRef2Scala, Actor, ActorSystem, Props }
import spray.can.server.HttpServer
import spray.io.{ IOBridge, SingletonHandler }
import spray.routing.HttpService
import akka.util.Timeout
import akka.util.duration._
import com.alexb.calculator.{ CalculatorModule, AddCommandListener, AddCommand }
import com.alexb.orders.{ OrderService, OrderActor, OrderModule }
import com.alexb.statics.StaticsModule
import com.alexb.memoize.ConcurrentHashMapCacheManager

object Main extends App {

	// we need an ActorSystem to host our application in
	val system = ActorSystem("SprayPlayground")
	
	// create the service instance, supplying all required dependencies
	// val calculatorModule = new CalculatorModule
	class SprayPlaygroundService extends Actor with CalculatorModule with OrderModule with StaticsModule with MongoContext with ElasticSearchContext with InfinispanContext {
		def actorSystem = system
		def config = system.settings.config
		
		val timeout = Timeout(5 seconds) // needed for `?`
		
		// the HttpService trait defines only one abstract member, which
		// connects the services environment to the enclosing actor or test
		def actorRefFactory = context

		// this actor only runs our route, but you could add
		// other things here, like request stream processing
		// or timeout handling
	 	def receive = runRoute(calculatorRoute ~ orderRoute ~ staticsRoute)
		def collection = mongoConn("spray_playground")("orders")
		def countryCollection = mongoConn("spray_playground")("countries")
	}

	// create and start the HttpService actor running our service as well as the root actor
	val httpService = system.actorOf(
		props = Props[SprayPlaygroundService],
		name = "service")

	///////////////////////////////////////////////////////////////////////////
	// Subscribing AddCommandListener
	val addCommandListener = system.actorOf(Props[AddCommandListener])
	system.eventStream.subscribe(addCommandListener, classOf[AddCommand])
	///////////////////////////////////////////////////////////////////////////

	// every spray-can HttpServer (and HttpClient) needs an IOBridge for low-level network IO
	// (but several servers and/or clients can share one)
	val ioBridge = new IOBridge(system).start()

	// create and start the spray-can HttpServer, telling it that we want requests to be
	// handled by the root service actor
	val sprayCanServer = system.actorOf(
		Props(new HttpServer(ioBridge, SingletonHandler(httpService))),
		name = "http-server")

	// a running HttpServer can be bound, unbound and rebound
	// initially to need to tell it where to bind to
	sprayCanServer ! HttpServer.Bind(
			system.settings.config.getString("application.host"),
			system.settings.config.getInt("application.port"))

	// finally we drop the main thread but hook the shutdown of
	// our IOBridge into the shutdown of the applications ActorSystem
	system.registerOnTermination {
		ioBridge.stop()
	}
}