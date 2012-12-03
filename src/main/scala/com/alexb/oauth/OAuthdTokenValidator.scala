package com.alexb.oauth

import spray.http.{OAuth2BearerToken, HttpRequest}
import spray.http.HttpHeaders.Authorization
import spray.httpx.UnsuccessfulResponseException
import spray.httpx.SprayJsonSupport._
import spray.client.HttpConduit._
import spray.json.DefaultJsonProtocol._
import scala.concurrent.{ Future, ExecutionContext }
import akka.actor.ActorRef

class OAuthdTokenValidator(conduit: ActorRef)(implicit executor: ExecutionContext) extends OAuthTokenValidator[User] {

  implicit val userFormat = jsonFormat2(User)

  val pipeline: Token => HttpRequest => Future[User] = { token =>
    addHeader(Authorization(OAuth2BearerToken(token))) ~>
    sendReceive(conduit) ~>
    unmarshal[User]
  }

  def apply(token: Option[Token]) = token match {
    case Some(token) => pipeline(token)(Get("/user")).map(Some(_)).recover({ case e: UnsuccessfulResponseException => None })
    case None        => Future { None }
  }
}
