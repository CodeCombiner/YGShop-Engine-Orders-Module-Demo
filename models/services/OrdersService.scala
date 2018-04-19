package orders.models.services

import java.util.UUID

import models.YGEvent
import orders.models.{ JsonRequestCreateOrder, YGOrder, YGOrderedReservation }
import play.api.mvc.Result
import products.models.{ JsonRequestUpdateReservation, JsonRequestUpdateReservationForBulkUpdate, YGReservation }

import scala.concurrent.Future

trait OrdersService {

  def changeOrderStatusByAdmin(
    orderId: UUID,
    userId: UUID,
    status: String
  ): Future[Option[YGOrder]]
  def changeOrderStatus(
    orderId: UUID,
    userId: UUID,
    status: String
  ): Future[Option[YGOrder]]
  def findOrderByIdForAdmin(orderId: UUID): Future[Option[YGOrder]]
  def findUserOrders(userId: UUID, offset: Int, limit: Int): Future[Seq[YGOrder]]
  def findUserOrder(userId: UUID, orderId: UUID): Future[Option[YGOrder]]
  def placeOrder(userId: UUID, body: JsonRequestCreateOrder): Future[Result]

  def adminGetOrders(limit: Int, offset: Int): Future[Seq[YGOrder]]

  def updateOrderByAdmin(
    order: YGOrder,
    ress: Seq[JsonRequestUpdateReservationForBulkUpdate]
  ): Future[Option[YGOrder]]

}
