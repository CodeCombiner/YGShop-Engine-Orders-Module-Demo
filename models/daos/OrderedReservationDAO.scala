package orders.models.daos

import java.util.UUID

import models.YGEvent
import orders.models.YGOrderedReservation

import scala.concurrent.Future

/**
 * Give access to the YGProduct object.
 */
trait OrderedReservationDAO {

  def createReservation(reservation: YGOrderedReservation): Future[Option[YGOrderedReservation]]

  def updateReservation(id: UUID, quantity: Int): Future[Option[YGOrderedReservation]]

  def removeReservation(reservationId: UUID): Future[Option[YGEvent]]

  def removeUsersReservations(userId: UUID): Future[Option[YGEvent]]

  def findReservationById(reservationId: UUID): Future[Option[YGOrderedReservation]]

  def findUserReservationByProductId(userId: UUID, productId: UUID): Future[Option[YGOrderedReservation]]

  def findUserReservations(userId: UUID): Future[Seq[YGOrderedReservation]]

}
