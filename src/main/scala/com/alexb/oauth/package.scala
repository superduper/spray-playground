package com.alexb

import scala.concurrent.Future

package object oauth {
  type Token = String
  type OAuthTokenValidator[T] = Option[Token] => Future[Option[T]]
}

package oauth {
  case class User(username: String, authorities: Seq[String])
}
