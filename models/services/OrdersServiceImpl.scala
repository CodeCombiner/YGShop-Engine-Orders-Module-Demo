package orders.models.services

import java.util.UUID
import javax.inject.Inject

import models.YGEvent
import orders.models.daos.{ OrderDAO, OrderedReservationDAO }
import orders.models.{ JsonRequestCreateOrder, YGOrder, YGOrderedReservation }
import org.omg.CosNaming.NamingContextPackage.NotFound
import play.api.libs.json.Json
import products.models.daos.ReservationDAO
import products.models.services.{ ProductService, ReservationsService }
import products.models.{ JsonRequestUpdateProduct, YGProduct, YGReservation, _ }
import play.api.mvc.Results._

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import orders.models._
import play.api.mvc.Result

class OrdersServiceImpl @Inject() (
  productsService: ProductService,
  reservationDao: ReservationDAO,
  orderedReservationDao: OrderedReservationDAO,
  orderedReservationsService: OrderedReservationsService,
  reservationsService: ReservationsService,
  orderDAO: OrderDAO
)(implicit ex: ExecutionContext)
    extends OrdersService {

  implicit val reservationFormat = Json.format[YGOrderedReservation]
  implicit val orderFormat = Json.format[YGOrder]
  implicit val jsonRequestUpdateReservationFormat =
    Json.format[JsonRequestUpdateReservation]
  //implicit val jsonRequestUpdateOrderFormat = Json.format[JsonRequestUpdateOrder]
  implicit val jsonRequestCreateOrderFormat =
    Json.format[JsonRequestCreateOrder]
  implicit val jsonResponseOrderFormat = Json.format[JsonResponseOrderFormat]

  //Todo remove all reservations or one by one - one by one is safe but all is better for order consistency?
  override def placeOrder(userId: UUID, body: JsonRequestCreateOrder) = {
    reservationDao
      .findUserReservations(userId)
      .map(reservtns => {
        val x: Future[Result] =
          reservtns.length match {
            case 0 =>
              Future.successful(
                UnprocessableEntity(Json.toJson("result" -> "error"))
              )
            case _ =>
              orderDAO
                .createOrder(requesttoOrderEntity(userId, body))
                .map(optOrder => {
                  optOrder.map(order => {
                    val seqyYGOrderesress = Future
                      .sequence {
                        reservtns.map(reservation => {
                          val orderedR = resToOrderedRes(reservation, order.id)
                          orderedReservationsService.createReservation(orderedR)

                        })
                      }
                      .map(seq => seq.flatten)
                    seqyYGOrderesress
                  }.map(seq =>
                    reservationsService
                      .removeUsersReservations(userId)
                      .map(x =>
                        seq.isEmpty match {

                          case true => {
                            orderDAO.remove(order.id)
                            UnprocessableEntity(
                              Json.toJson("result" -> "error.cart.not.found")
                            )
                          }
                          case false =>
                            Ok(Json.toJson(
                              "response" -> JsonResponseOrderFormat(
                                order,
                                seq
                              )
                            ))
                        }))
                    .flatten)
                }.getOrElse(Future.successful(UnprocessableEntity(
                  Json.toJson("result" -> "error.cart.not.found")
                ))))
                .flatten
          }
        x
      })
      .flatten
  }

  override def findOrderByIdForAdmin(orderId: UUID) = orderDAO.findOrderByIdForAdmin(orderId)

  override def findUserOrders(userId: UUID, offset: Int, limit: Int) = orderDAO.findUserOrders(userId, offset, limit)

  override def findUserOrder(userId: UUID, orderId: UUID) = orderDAO.findUserOrder(userId, orderId)

  override def changeOrderStatusByAdmin(orderId: UUID, userId: UUID, status: String) = {
    findUserOrder(userId: UUID, orderId: UUID).map(_.map(order => {
      orderDAO.changeOrderStatus(orderId, userId, status)
    })).flatMap(_.getOrElse(Future(Option.empty[YGOrder])))
  }

  override def changeOrderStatus(orderId: UUID, userId: UUID, status: String) =
    {
      findUserOrder(userId: UUID, orderId: UUID).map(_.map(order => {
        if (YGStatus.withName(order.status).id < YGStatus.withName(status).id) {
          orderDAO.changeOrderStatus(orderId, userId, status)
        } else {
          Future.successful(Some(order))
        }
      })).flatMap(_.getOrElse(Future(Option.empty[YGOrder])))
    }

  override def adminGetOrders(limit: Int, offset: Int) = orderDAO.getOrders(offset, limit)

  override def updateOrderByAdmin(order: YGOrder, ress: Seq[JsonRequestUpdateReservationForBulkUpdate]) = {
    orderDAO.updateOrder(order).map(optUpdatedOrder =>
      ress.map(res => orderedReservationsService.updateReservation(
        JsonRequestUpdateReservationForBulkUpdate(res.id, res.quantity), res.id
      ))).
      map(x => Future.sequence(x)).map(x => Some(order))
  }

}
