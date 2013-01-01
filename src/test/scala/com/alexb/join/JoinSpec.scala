package com.alexb.join

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class JoinSpec extends WordSpec with MustMatchers {
  "Join class" must {
    "do inner joins" in {
      val left = List(1, 2, 3)
      val right = List(2, 3)
      Join.join(left, right, identity[Int], identity[Int]) must be (Iterable((2, 2), (3, 3)))
    }
    "do left joins" in {
      val left = List(1, 2, 3)
      val right = List(2, 3)
      Join.leftJoin(left, right, identity[Int], identity[Int]) must be (Iterable((1, None), (2, Some(2)), (3, Some(3))))
    }
  }
}