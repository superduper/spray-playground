package com.alexb.swift

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random
import spray.can.server.SprayCanHttpServerApp
import spray.http.MediaTypes
import scala.io.Source
import language.postfixOps

class SwiftClientSpec extends WordSpec with MustMatchers with SprayCanHttpServerApp {
  import system.dispatcher
  val timeout = 20 seconds
  implicit val askTimeout = Timeout(timeout)

  val port = Stream.continually(Random.nextInt(65536)).find(_ > 49152).get
  val mockSwiftServer = system.actorOf(Props(new StubSwiftServer))
  val server = newHttpServer(mockSwiftServer)
  Await.ready(server ? Bind("localhost", port), timeout)

  val client = system.actorOf(Props(new SwiftClient(SwiftCredentials("some_account", "some_auth_key"),
    s"http://localhost:$port/v1.0")))

  val bytes = Source.fromFile("src/test/resources/sample.png")(scala.io.Codec.ISO8859).map(_.toByte).toArray

  "Swift client" must {
    "create containers" in {
      Await.result(client ? CreateContainer("new_container"), timeout) must be (CreateContainerResult(true, false))
    }
    "create duplicate containers" in {
      Await.result(client ? CreateContainer("new_container"), timeout) must be (CreateContainerResult(true, true))
    }
    "list containers" in {
      Await.result(client ? ListContainers, timeout) must be (Seq(Container("new_container", 0, 0)))
    }
    "list objects" in {
      Await.result(client ? ListObjects("new_container"), timeout) must be (Seq())
    }
    "put objects" in {
      Await.result(client ? PutObject("new_container", "sample.png", bytes), timeout) must be (PutObjectResult(true))
    }
    "get objects" in {
      val obj = Await.result((client ? GetObject("new_container", "sample.png")).mapTo[Option[Object]], timeout).get
      obj.name must be ("sample.png")
      obj.mediaType must be (MediaTypes.forExtension("png").get)
      obj.data.deep must be (bytes.deep)
    }
    "get empty objects" in {
      Await.result(client ? GetObject("new_container", "some_nonexistent_object"), timeout) must be (None)
    }
    "delete existent objects" in {
      Await.result(client ? DeleteObject("new_container", "sample.png"), timeout) must be (DeleteObjectResult(true, false))
    }
    "delete nonexistent objects" in {
      Await.result(client ? DeleteObject("new_container", "sample.png"), timeout) must be (DeleteObjectResult(true, true))
    }
    "handle interleaving requests" in {
      val req1 = client ? ListContainers
      val req2 = client ? ListContainers
      val f = for {
        res1 <- req1.mapTo[Seq[Container]]
        res2 <- req2.mapTo[Seq[Container]]
      } yield (res1, res2)
      val readyResult = Await.result(f, timeout)
      readyResult._1.length must be (1)
      readyResult._2.length must be (1)
    }
    "handle authentication token expiration" in {
      mockSwiftServer ! RegenerateToken
      Await.result(client ? ListContainers, timeout)
    }
  }
}
