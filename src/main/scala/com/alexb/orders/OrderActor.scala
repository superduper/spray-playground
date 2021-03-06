package com.alexb.orders

import akka.actor.Actor
import com.mongodb.casbah.Imports._
import java.util.UUID
import com.alexb.utils.{ FutureUtils, PageInfo }

class OrderActor(collection: MongoCollection) extends Actor with FutureUtils {

  def receive = {
    case cmd: AddOrderCommand       => wrapInFuture { saveOrder(Order(UUID.randomUUID.toString, cmd.clientId, List(), cmd.notes)) }
    case cmd: DeleteOrderCommand    => wrapInFuture { deleteOrder(cmd.orderId) }
    case cmd: OrderByIdQuery        => answerWithFutureResult { findOrder(cmd.orderId) }
    case cmd: OrdersByClientIdQuery => answerWithFutureResult { findOrdersByClient(cmd.clientId, cmd.page) }
  }

  private def saveOrder(order: Order) = {
    collection += MongoDBObject("_id" -> order.orderId,
      "clientId" -> order.clientId,
      "items" -> order.items,
      "notes" -> order.notes)
  }

  private def deleteOrder(orderId: String) = {
    collection -= MongoDBObject("_id" -> orderId)
  }

  private def findOrder(orderId: String) =
    collection
      .findOne(MongoDBObject("_id" -> orderId))
      .map(toOrder)

  private def findOrdersByClient(clientId: String, page: PageInfo) =
    collection
      .find(MongoDBObject("clientId" -> clientId))
      .skip(page.skip)
      .limit(page.size)
      .map(toOrder)
      .to[List]

  private def toOrder(d: DBObject): Order =
    Order(d.getAs[String]("_id").get,
      d.getAs[String]("clientId").get,
      d.getAs[Iterable[DBObject]]("items").get.to[List].map(toOrderItem),
      d.getAs[String]("notes").getOrElse(""))

  private def toOrderItem(d: DBObject): OrderItem =
    OrderItem(d.getAs[String]("itemId").get, d.getAs[Int]("quantity").get)
}
