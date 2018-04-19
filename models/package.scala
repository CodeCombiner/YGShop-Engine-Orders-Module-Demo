package orders

import java.util.UUID

import products.models._

package object models {

  def resToOrderedRes(res : YGReservation, order_id : UUID) ={
    YGOrderedReservation(res.id,res.user_id, res.p_id, order_id,res.version,res.created_at,
      res.valid_till,res.quantity)
  }


  def requesttoOrderEntity (user_id : UUID, body : JsonRequestCreateOrder) ={
    YGOrder(id = UUID.randomUUID(),
      user_id = user_id,
      delivery_address = body.delivery_address,
      lat = body.lat,
      lon = body.lon,
      note = body.note,
      payment_method = body.payment_method,
    )
  }

  def requestCreateReservationToYGOrderedReservation(jsRequest: JsonRequestCreateReservation, u_id: UUID, order_id: UUID) = {
    YGOrderedReservation(id = UUID.randomUUID(), u_id, jsRequest.p_id, order_id, jsRequest.version, System.currentTimeMillis(), Long.MaxValue, jsRequest.quantity)
  }

  def toReservationWithProduct(res: YGOrderedReservation, product: YGProduct) =
    JsonProductOrderedReservationResponse(res, product)

  def updateOrdertoOrderEntity (user_id : UUID, order_id: UUID, body : JsonRequestUpdateOrder) ={
    YGOrder(order_id,
      user_id,
      delivery_address = body.delivery_address,
      lat = body.lat,
      lon = body.lon,
      note = body.note,
      payment_method = body.payment_method,
    )
  }

}
