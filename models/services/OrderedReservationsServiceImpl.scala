package orders.models.services

import java.util.UUID
import javax.inject.Inject

import models.YGEvent
import orders.models.YGOrderedReservation
import orders.models.daos.OrderedReservationDAO
import play.api.libs.json.Json
import products.models.daos.ReservationDAO
import products.models.services.ProductService
import products.models.{ JsonRequestUpdateProduct, YGProduct, _ }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

class OrderedReservationsServiceImpl @Inject() (productsService: ProductService, orderedReservationDao: OrderedReservationDAO)(implicit ex: ExecutionContext) extends OrderedReservationsService {

  implicit val productFormat = Json.format[YGProduct]
  implicit val jsonRequestProductFormat = Json.format[JsonRequestUpdateProduct]
  implicit val reservationFormat = Json.format[YGOrderedReservation]
  // implicit val jsonRequestReservationFormat = Json.format[JsonRequestUpdateReservation]

  override def createReservation(reservation: YGOrderedReservation) = {
    (for {
      //1) reserve quantity
      decreaseQuantity <- productsService.decreaseQuantity(reservation.p_id, reservation.quantity)
      //2) create reservation Dao
      newReservation <- orderedReservationDao.createReservation(reservation)
    } yield newReservation).map(response => response match {
      //3a) if created respond reservation,
      case Some(reservation) => Future.successful(Some(reservation))
      //3b) if not respond false, increase quantity back in products
      case None => productsService.increaseQuantity(reservation.p_id, reservation.quantity).map(product => Option.empty[YGOrderedReservation])
    }).flatten
  }

  override def updateReservation(reservationJson: JsonRequestUpdateReservationForBulkUpdate, id: UUID): Future[Option[YGOrderedReservation]] = {

    findReservationById(id).map(_.map(reservationToUpdate => {
      productsService.retrieve(reservationToUpdate.p_id).map { product =>
        var x: Future[Option[YGOrderedReservation]] = null
        //1) check quantity in products if different , respond same
        if (reservationToUpdate.quantity == reservationJson.quantity) { x = Future.successful(Some(reservationToUpdate)) }
        //2) update  products no and respond reservation
        else {
          var newReservationQ = 0
          val y =
            if (reservationToUpdate.quantity > reservationJson.quantity) {
              val newQuantityDifference = reservationToUpdate.quantity - reservationJson.quantity
              newReservationQ = reservationJson.quantity
              productsService.increaseQuantity(reservationToUpdate.p_id, newQuantityDifference)
            } else {
              var newQuantityDifference = reservationJson.quantity - reservationToUpdate.quantity

              //decrease of what possible
              if (product.get.available_positions > newQuantityDifference) {
                newReservationQ = reservationJson.quantity
              } else {
                newQuantityDifference = product.get.available_positions
                newReservationQ = product.get.available_positions + reservationToUpdate.quantity
              }
              productsService.decreaseQuantity(reservationToUpdate.p_id, newQuantityDifference)
            }

          x = y.map(product => orderedReservationDao.updateReservation(id, newReservationQ)).flatten
        }
        x
      }
    }).get).flatten.flatten

  }

  override def removeReservation(reservationId: UUID): Future[Option[YGEvent]] = {

    for {
      reservationToRemove <- findReservationById(reservationId).map(optres => optres.get)
      eventRemove <- orderedReservationDao.removeReservation(reservationId).map(optevent => optevent.get)
      eventIncreaseFut <- {
        val x: Future[Option[YGEvent]] = eventRemove.result match {
          case "success" => productsService.increaseQuantity(reservationToRemove.p_id, reservationToRemove.quantity)
            .map(product =>
              Some(YGEvent(id = product.p_id, processed_at = System.currentTimeMillis(), result = "success",
                initiated_by = "ReservationsService", appointed_to = "ReservationsController")))
          case _ => orderedReservationDao.removeReservation(reservationToRemove.p_id)
        }
        x
      }

    } yield eventIncreaseFut

  }

  override def removeUsersReservations(userId: UUID): Future[Option[YGEvent]] = {
    def thisCollRsrvtnById(reserv_id: UUID, reservations: Seq[YGOrderedReservation]): YGOrderedReservation = {
      reservations.filter(ygres => ygres.id == reserv_id).head
    }
    // 1st plan of processing
    val futEvents: Future[Seq[YGEvent]] = for {
      allReservations <- findUserReservations(userId)
      eventsRemove <- Future.sequence(allReservations.map(YGOrderedReservation => removeReservation(YGOrderedReservation.id).map(optEv => optEv.get)))
      /* eventsChecked <- eventsRemove.map(optEvent=> optEvent.map(event => { event.result match{
        case "success" => Future.successful(Some(event))
        case _ => removeReservation(event.id) //thisCollRsrvtnById(event.id,allReservations).quantity
      }}))*/
    } yield eventsRemove //eventsChecked

    futEvents.map(events => if (events.filterNot(event => event.result == "success").isEmpty)
      Some(YGEvent(processed_at = System.currentTimeMillis(), result = "success",
      initiated_by = "ReservationsService", appointed_to = "ReservationsController"))
    else Some(YGEvent(processed_at = System.currentTimeMillis(), result = "error",
      initiated_by = "ReservationsService", appointed_to = "ReservationsController")))

  }

  override def findReservationById(reservationId: UUID): Future[Option[YGOrderedReservation]] =
    orderedReservationDao.findReservationById(reservationId)

  override def findUserReservations(userId: UUID): Future[Seq[YGOrderedReservation]] =
    orderedReservationDao.findUserReservations(userId)

  override def findUserReservationByProductId(userId: UUID, productId: UUID) =
    orderedReservationDao.findUserReservationByProductId(userId: UUID, productId)
}

