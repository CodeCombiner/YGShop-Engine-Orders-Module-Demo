package orders

import java.util.UUID
import javax.inject.Inject

import auth.models.authorizations.WithRole
import auth.models.services.AuthTokenService
import auth.utils.auth.DefaultEnv
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.crypto.Base64AuthenticatorEncoder
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import orders.models.{ JsonRequestCreateOrder, JsonUpdateOrderStatus, YGOrder, YGOrderedReservation }
import orders.models.services.{ OrderedReservationsService, OrdersService }
import play.api.Configuration
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ AbstractController, ControllerComponents, Result }
import products.models.{ JsonRequestUpdateReservationForBulkUpdate, _ }
import orders.models._
import products.models.services.ReservationsService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OrdersController @Inject() (
  val messagesAp2i: MessagesApi,
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  authInfoRepository: AuthInfoRepository,
  authenticatorEncoder: Base64AuthenticatorEncoder,
  credentialsProvider: CredentialsProvider,
  ordersService: OrdersService,
  reservationService: ReservationsService,
  orderedReservationService: OrderedReservationsService,
  userTokenService: AuthTokenService,
  configuration: Configuration,

  lock: Clock
)
    extends AbstractController(components) with I18nSupport {

  implicit val reservationFormat = Json.format[YGOrderedReservation]
  implicit val jsonUpdateStatusFormat = Json.format[JsonUpdateOrderStatus]
  implicit val orderFormat = Json.format[YGOrder]
  implicit val jsonRequestUpdateReservationForBulkUpdate = Json.format[JsonRequestUpdateReservationForBulkUpdate]
  implicit val jsonRequestUpdateReservationFormat = Json.format[JsonRequestUpdateReservation]
  implicit val jsonRequestUpdateOrderFormat = Json.format[JsonRequestUpdateOrder]
  implicit val jsonRequestCreateOrderFormat = Json.format[JsonRequestCreateOrder]
  implicit val jsonRequestCreateReservationFormat = Json.format[JsonRequestCreateReservation]

  def getOrders(offset: Int, limit: Int) = silhouette.SecuredAction.async { implicit request =>
    ordersService.findUserOrders(request.identity.id, offset, limit).
      map(orders => Ok(Json.toJson("result" -> Json.toJson(orders))))
  }

  def getOrderById(id_ : String) = silhouette.SecuredAction.async { implicit request =>
    ordersService.findUserOrder(request.identity.id, UUID.fromString(id_)).
      map(order => Ok(Json.toJson("result" -> Json.toJson(order))))
  }

  def createOrder = silhouette.SecuredAction.async(parse.json[JsonRequestCreateOrder]) {
    implicit request =>
      ordersService.placeOrder(request.identity.id, request.body)
  }

  def updateOrderStatus(id: String, status: String) = silhouette.SecuredAction.
    async(parse.json[JsonUpdateOrderStatus]) { implicit request =>
      ordersService.changeOrderStatus(UUID.fromString(id), request.identity.id, status).
        map(result => Ok(Json.toJson("result" -> result)))
    }

  def adminCreateOrder(id: String) = silhouette.SecuredAction(WithRole("admin")).
    async(parse.json[JsonRequestCreateOrder]) { implicit request =>
      ordersService.placeOrder(UUID.fromString(id), request.body)
    }

  def adminGetUserOrders(id: String, offset: Int, limit: Int) =
    silhouette.SecuredAction(WithRole("admin")).async { implicit request =>
      ordersService.findUserOrders(request.identity.id, offset, limit).
        map(orders => Ok(Json.toJson("result" -> Json.toJson(orders))))
    }

  def adminGetOrders(limit: Int, offset: Int) = silhouette.SecuredAction(WithRole("admin")).
    async { implicit request =>
      ordersService.adminGetOrders(limit, offset).
        map(orders => Ok(Json.toJson("result" -> Json.toJson(orders))))
    }

  def adminGetById(id: String) = silhouette.SecuredAction(WithRole("admin")).
    async { implicit request =>
      ordersService.findOrderByIdForAdmin(UUID.fromString(id)).
        map(order => Ok(Json.toJson("result" -> Json.toJson(order))))
    }

  def adminUpdateOrderStatus(user_id: String, id: String, status: String) =
    silhouette.SecuredAction(WithRole("admin")).
      async(parse.json[JsonUpdateOrderStatus]) { implicit request =>
        ordersService.changeOrderStatusByAdmin(
          UUID.fromString(id),
          UUID.fromString(user_id), status
        ).
          map(result => Ok(Json.toJson("result" -> result)))
      }

  def adminRemoveOrderReservation(orderId: String, res_id: String) =
    silhouette.SecuredAction(WithRole("admin")).async {
      orderedReservationService.removeReservation(UUID.fromString(res_id)).
        map(result => Ok(Json.toJson("result" -> "success")))
    }

  def adminCreateOrderReservation(orderId: String) =
    silhouette.SecuredAction(WithRole("admin")).async(parse.json[JsonRequestCreateReservation]) {
      implicit request =>
        orderedReservationService
          .findUserReservationByProductId(request
            .identity.id, request.body.p_id)
          .map { optReservation =>
            {
              val z: Future[Result] = optReservation match {

                case None =>
                  orderedReservationService
                    .createReservation(
                      requestCreateReservationToYGOrderedReservation(
                        request.body,
                        request.identity.id,
                        UUID.fromString(orderId)
                      )
                    )
                    .map(reservation =>
                      Ok(Json.toJson("result" -> Json.toJson(reservation))))
                case Some(reservExistingToUpdate) => {
                  orderedReservationService
                    .updateReservation(
                      JsonRequestUpdateReservationForBulkUpdate(reservExistingToUpdate.id, request.body.quantity),
                      reservExistingToUpdate.id
                    )
                    .map(
                      reservation =>
                        Ok(Json.toJson("result" -> Json.toJson(reservation)))
                    )
                }
              }
              z
            }
          }
          .flatten
    }

  def adminUpdateOrderReservation(orderId: String, res_id: String) =
    silhouette.SecuredAction(WithRole("admin")).async(parse.json[JsonRequestUpdateReservation]) { implicit request =>
      orderedReservationService.findReservationById(UUID.fromString(res_id)).
        map(optResToUpdate => optResToUpdate.map(resToUpdate => {
          orderedReservationService.updateReservation(
            JsonRequestUpdateReservationForBulkUpdate(UUID.fromString(res_id), request.body.quantity),
            UUID.fromString(res_id)
          ).map(
              reservation => Ok(Json.toJson("result" -> Json.toJson(reservation)))
            )
        }).getOrElse(Future.successful(NotFound(JsObject(Map("not.found" ->
          Json.toJson("reservation.not.found"))))))).flatten
    }

  //TODO Probably update both reservations and order details
  def adminUpdateOrder(user_id: String, order_id: String) =
    silhouette.SecuredAction(WithRole("admin")).async(parse.json[JsonRequestUpdateOrder]) { implicit request =>
      ordersService.updateOrderByAdmin(
        updateOrdertoOrderEntity(UUID.fromString(user_id), UUID.fromString(order_id), request.body),
        request.body.reservations
      ).map(ygOrder => Ok(Json.toJson("result" -> Json.toJson(ygOrder))))
    }

}
