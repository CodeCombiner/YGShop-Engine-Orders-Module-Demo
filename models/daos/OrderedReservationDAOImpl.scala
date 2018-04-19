package orders.models.daos

import java.util.UUID
import javax.inject.Inject

import auth.models.daos.{ UserDAO, UserDAOImpl, UserDAOTrait }
import models.YGEvent
import orders.models.YGOrderedReservation
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.sql.SqlProfile.ColumnOption.NotNull

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Give access to the user object.
 */
//Todo add singleton annotation - test
class OrderedReservationDAOImpl @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  usersDao: UserDAOImpl,
  ordersDao: OrderDAOImpl
)(implicit ex: ExecutionContext)
    extends OrderedReservationDAO
    with UserDAOTrait
    with OrderDAOTrait {

  override val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.

  protected class ReservationsTable(tag: Tag)
      extends Table[YGOrderedReservation](tag, "ordered_reservations") {

    def id = column[UUID]("id", O.PrimaryKey)

    def user_id = column[UUID]("user_id", NotNull)

    def p_id = column[UUID]("p_id", NotNull)

    def order_id = column[UUID]("order_id", NotNull)

    def version = column[Long]("version", NotNull)

    def created_at = column[Long]("created_at", NotNull)

    def valid_till = column[Long]("valid_till", NotNull)

    def quantity = column[Int]("quantity", NotNull)

    def owner =
      foreignKey("USER_FK", user_id, users)(
        _.id,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Restrict
      )

    def order =
      foreignKey("ORDER_FK", order_id, orders)(
        _.id
      )

    /*foreignKey("USER_FK", user_id, usersDao.getTable)(
      _.id,
         onUpdate = ForeignKeyAction.Restrict,
         onDelete = ForeignKeyAction.Cascade)*/

    def * =
      (
        id,
        user_id,
        p_id,
        order_id,
        version,
        created_at,
        valid_till,
        quantity
      ) <> ((YGOrderedReservation.apply _).tupled, YGOrderedReservation.unapply)
  }

  /**
   * The starting point for all queries on the people table.
   */
  private val orderedReservations = TableQuery[ReservationsTable]

  override def createReservation(reservation: YGOrderedReservation) =
    db.run(
      orderedReservations += reservation
    )
      .map(result =>
        result match {
          case 1 => Some(reservation)
          case _ => None
        })

  override def updateReservation(id: UUID, quantity: Int) = {
    val action = orderedReservations
      .filter(_.id === id)
      .map(
        reservationToUpdate =>
          (
            reservationToUpdate.quantity
          )
      )
      .update(
        (
          quantity
        )
      )
    val future = db.run(action)
    future.map { result =>
      result match {
        case 1 => findReservationById(id)
        case _ => Future.successful(None)
      }
    }.flatten
  }

  override def removeReservation(reservationId: UUID) = {
    val action = orderedReservations.filter(_.id === reservationId).delete
    val future = db.run(action)
    future.map { result =>
      result match {
        case 1 =>
          Some(
            YGEvent(
              processed_at = System.currentTimeMillis(),
              result = "success",
              initiated_by = "ReservationDAO",
              appointed_to = "ReservationsController"
            )
          )
        case _ =>
          Some(
            YGEvent(
              processed_at = System.currentTimeMillis(),
              result = "error",
              initiated_by = "ReservationDAO",
              appointed_to = "ReservationsController"
            )
          )
      }
    }
  }

  override def removeUsersReservations(userId: UUID) = {
    val action = orderedReservations.filter(_.user_id === userId).delete
    val future = db.run(action)
    future.map { result =>
      result match {
        case 1 =>
          Some(
            YGEvent(
              processed_at = System.currentTimeMillis(),
              result = "success",
              initiated_by = "ReservationDAO",
              appointed_to = "ReservationsController"
            )
          )
        case _ =>
          Some(
            YGEvent(
              processed_at = System.currentTimeMillis(),
              result = "error",
              initiated_by = "ReservationDAO",
              appointed_to = "ReservationsController"
            )
          )
      }
    }
  }

  override def findReservationById(reservationId: UUID) = {
    db.run(orderedReservations.filter(_.id === reservationId).result)
      .map(seq =>
        seq.size match {
          case 0 => Option.empty[YGOrderedReservation]
          case 1 => seq.headOption
        })
  }

  override def findUserReservations(userId: UUID) = {

    val q = orderedReservations.filter(reservation => reservation.user_id === userId)
    val action = q.result
    val result: Future[Seq[YGOrderedReservation]] = db.run(action)
    result
  }

  override def findUserReservationByProductId(userId: UUID, productId: UUID) =
    db.run(orderedReservations.filter(_.p_id === productId).result)
      .map(seq =>
        seq.size match {
          case 0 => Option.empty[YGOrderedReservation]
          case 1 => seq.headOption
        })
}
