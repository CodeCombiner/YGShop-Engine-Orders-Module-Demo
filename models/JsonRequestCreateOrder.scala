package orders.models

import java.util.UUID

case class JsonRequestCreateOrder(
  // id = generate
  // GET FROM request of admin id path  user_id: UUID,
  //  created_at: Long = System.currentTimeMillis(),
  delivery_address: String,
  lat: Double,
  lon: Double,
  note: String,
  // status: String = YGStatus.Placed.toString,
  payment_method: String
// history: String,
// var valid_till: Long

)

