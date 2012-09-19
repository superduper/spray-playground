package com.alexb.service

import akka.actor.{ actorRef2Scala, ActorSystem, Props }
import cc.spray.can.server.HttpServer
import cc.spray.io.pipelines.MessageHandlerDispatch
import cc.spray.io.IoWorker
import cc.spray.HttpService
import cc.spray.SprayCanRootService

object Main extends App {

	// we need an ActorSystem to host our application in
	val system = ActorSystem("SprayPlayground")

	// create the service instance, supplying all required dependencies
	val mainModule = new CalculatorService {
		implicit def actorSystem = system
		// bake your module cake here

		// Creating calculator actor
		implicit val calculator = actorSystem.actorOf(
			props = Props(new CalculatorActor),
			name = "calculator-actor")
	}

	// create and start the HttpService actor running our service as well as the root actor
	val httpService = system.actorOf(
		props = Props(new HttpService(mainModule.calculatorService)),
		name = "calculator-service")
	val rootService = system.actorOf(
		props = Props(new SprayCanRootService(httpService)),
		name = "root-service")

	///////////////////////////////////////////////////////////////////////////
	// Subscribing AddCommandListener
	val addCommandListener = system.actorOf(Props[AddCommandListener])
	system.eventStream.subscribe(addCommandListener, classOf[AddCommand])
	///////////////////////////////////////////////////////////////////////////

	// every spray-can HttpServer (and HttpClient) needs an IoWorker for low-level network IO
	// (but several servers and/or clients can share one)
	val ioWorker = new IoWorker(system).start()

	// create and start the spray-can HttpServer, telling it that we want requests to be
	// handled by the root service actor
	val sprayCanServer = system.actorOf(
		Props(new HttpServer(ioWorker, MessageHandlerDispatch.SingletonHandler(rootService))),
		name = "http-server")

	// a running HttpServer can be bound, unbound and rebound
	// initially to need to tell it where to bind to
	sprayCanServer ! HttpServer.Bind("localhost", 8080)

	// finally we drop the main thread but hook the shutdown of
	// our IoWorker into the shutdown of the applications ActorSystem
	system.registerOnTermination {
		ioWorker.stop()
	}
}