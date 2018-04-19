package orders.models.services

import java.util.UUID

import models.YGEvent
import orders.models.YGOrderedReservation
import products.models.{ JsonRequestUpdateReservation, JsonRequestUpdateReservationForBulkUpdate }

import scala.concurrent.Future

trait OrderedReservationsService {

  def createReservation(reservation: YGOrderedReservation): Future[Option[YGOrderedReservation]]

  def updateReservation(reservation: JsonRequestUpdateReservationForBulkUpdate, id: UUID): Future[Option[YGOrderedReservation]]

  def removeReservation(reservationId: UUID): Future[Option[YGEvent]]

  def removeUsersReservations(userId: UUID): Future[Option[YGEvent]]

  def findReservationById(reservationId: UUID): Future[Option[YGOrderedReservation]]

  def findUserReservationByProductId(userId: UUID, productId: UUID): Future[Option[YGOrderedReservation]]

  def findUserReservations(userId: UUID): Future[Seq[YGOrderedReservation]]

}
