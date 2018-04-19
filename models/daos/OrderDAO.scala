package orders.models.daos

import java.util.UUID

import models.YGEvent
import orders.models.{ JsonRequestCreateOrder, YGOrder, YGOrderedReservation }
import products.models.YGReservation
import slick.lifted.TableQuery

import scala.concurrent.Future

/**
 * Give access to the YGProduct object.
 */
trait OrderDAO {

  def createOrder(order: YGOrder): Future[Option[YGOrder]]

  //TODO migrate them into created order
  def updateOrder(
    order: YGOrder
  ): Future[Option[YGOrder]]

  def changeOrderStatus(
    orderId: UUID,
    userId: UUID,
    status: String
  ): Future[Option[YGOrder]]
  def findOrderByIdForAdmin(orderId: UUID): Future[Option[YGOrder]]

  def findUserOrders(userId: UUID, offset: Int, limit: Int): Future[Seq[YGOrder]]

  def findUserOrder(userId: UUID, orderId: UUID): Future[Option[YGOrder]]

  def remove(orderId: UUID): Future[Option[YGEvent]]

  def getOrders(offset: Int, limit: Int): Future[Seq[YGOrder]]

}
