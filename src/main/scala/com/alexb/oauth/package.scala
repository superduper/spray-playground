package com.alexb

import scala.concurrent.Future

package object oauth {
  type Token = String
  type OAuthTokenValidator[T] = Option[Token] => Future[Option[T]]
}

package oauth {
  case class User(uid: String, login: Option[String], authorities: Seq[String])
}
