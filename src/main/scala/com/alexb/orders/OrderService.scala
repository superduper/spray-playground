package com.alexb.orders

import spray.routing.directives.PathMatchers._
import spray.routing.HttpService
import spray.http.{ StatusCodes, EmptyEntity}
import spray.httpx.SprayJsonSupport
import akka.util.Timeout
import akka.actor.ActorRef
import akka.pattern.ask
import com.alexb.utils.PageDirectives
import com.alexb.oauth.OAuth
import com.alexb.main.context.OAuthSupport

trait OrderService
  extends HttpService
  with PageDirectives
  with SprayJsonSupport
  with OrderMarshallers {

  this: OAuthSupport =>

  implicit val timeout: Timeout // needed for `?` below

  def orderActor: ActorRef
  def orderSearchActor: ActorRef

  val orderRoute = {
    pathPrefix("orders") {
      post {
        path("save-order") {
          entity(as[AddOrderCommand]) { cmd =>
            orderActor ! cmd
            respondWithStatus(StatusCodes.Accepted) {
              complete(EmptyEntity)
            }
          }
        }
      } ~
      get {
        path("orders" / PathElement) { orderId =>
          complete((orderActor ? OrderByIdQuery(orderId)).mapTo[Option[Order]])
        } ~
        path("orders-by-client" / PathElement) { clientId =>
          pageInfo { page =>
            complete((orderActor ? OrdersByClientIdQuery(clientId, page)).mapTo[List[Order]])
          }
        } ~
        path("search-by-notes") {
          (parameter('query.as[String]) & pageInfo) { (query, page) =>
            complete((orderSearchActor ? SearchOrdersByNotesQuery(query, page)).mapTo[Seq[Order]])
          }
        }
      } ~
      path("test") {
        authenticate(OAuth(tokenValidator)) { user =>
          complete(Order("1", "1", List(OrderItem("Trololo", 2)), "Some notes"))
        }
      }
    }
  }
}
