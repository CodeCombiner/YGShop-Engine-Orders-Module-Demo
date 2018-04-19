package orders.models

import java.util.UUID

import products.models.JsonRequestUpdateReservationForBulkUpdate

case class JsonRequestUpdateOrder(
  delivery_address: String,
  lat: Double,
  lon: Double,
  note: String,
  payment_method: String,
  reservations: Seq[JsonRequestUpdateReservationForBulkUpdate]
)
