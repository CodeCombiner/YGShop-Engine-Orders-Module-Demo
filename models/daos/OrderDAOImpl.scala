package orders.models.daos

import java.util.UUID
import javax.inject.Inject

import auth.models.daos.UserDAOImpl
import models.YGEvent
import orders.models.{ YGOrder, YGOrderedReservation, YGStatus }
import play.api.db.slick.DatabaseConfigProvider
import products.models.YGReservation
import slick.jdbc.JdbcProfile
import slick.sql.SqlProfile.ColumnOption.NotNull

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Give access to the user object.
 */
//Todo add singleton annotation - test
class OrderDAOImpl @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  usersDao: UserDAOImpl
)(implicit ex: ExecutionContext)
    extends OrderDAO with OrderDAOTrait {

  override val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.

  import dbConfig._
  import profile.api._

  override def findOrderByIdForAdmin(orderId: UUID) = {
    db.run(orders.filter(_.id === orderId).result)
      .map(seq =>
        seq.size match {
          case 0 => Option.empty[YGOrder]
          case 1 => seq.headOption
        })
  }

  override def findUserOrders(userId: UUID, offset: Int, limit: Int) = {

    val q = orders.filter(order => order.user_id === userId).drop(offset).take(limit)
    val action = q.result
    val result: Future[Seq[YGOrder]] = db.run(action)
    result
  }

  override def getOrders(offset: Int, limit: Int) = {

    val q = orders.drop(offset).take(limit)
    val action = q.result
    val result: Future[Seq[YGOrder]] = db.run(action)
    result
  }

  override def findUserOrder(userId: UUID, orderId: UUID) =
    db.run(orders.filter(order => order.id === orderId && order.user_id === userId).result)
      .map(seq =>
        seq.size match {
          case 0 => Option.empty[YGOrder]
          case 1 => seq.headOption
        })

  override def createOrder(order: YGOrder) = {
    db.run(
      orders += order
    ).map(result =>
        result match {
          case 1 => Some(order)
          case _ => None
        })
  }

  override def remove(orderId: UUID) = {
    val action = orders.filter(_.id === orderId).delete
    val future = db.run(action)
    future.map { result =>
      result match {
        case 1 => Some(YGEvent(processed_at = System.currentTimeMillis(), result = "success",
          initiated_by = "OrderDAO", appointed_to = "OrdersServiceOnly"))
        case _ => Some(YGEvent(processed_at = System.currentTimeMillis(), result = "error",
          initiated_by = "OrderDAO", appointed_to = "OrdersServiceOnly"))
      }
    }
  }

  override def changeOrderStatus(orderId: UUID, userId: UUID, status: String) = {
    val action = orders
      .filter(order => order.id === orderId && order.user_id === userId)
      .map(
        orderToUpdate =>
          (
            orderToUpdate.status
          )
      )
      .update(
        (
          status
        )
      )
    val future = db.run(action)
    future.map { result =>
      result match {
        case 1 => findOrderByIdForAdmin(orderId)
        case _ => Future.successful(None)
      }
    }.flatten
  }

  override def updateOrder(order: YGOrder) = {
    val action = orders.filter(_.id === order.id).map(orderToUpdate =>
      (orderToUpdate.delivery_address, orderToUpdate.lat,
        orderToUpdate.lon, orderToUpdate.note,
        orderToUpdate.payment_method)).
      update(
        (order.delivery_address, order.lat,
          order.lon, order.note,
          order.payment_method)
      )
    val future = db.run(action)
    future.map { result =>
      result match {
        case 1 => findUserOrder(order.user_id, order.id)
        case _ => Future.successful(None)
      }
    }.flatten

  }
}
