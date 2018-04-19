package orders.models

import java.util.UUID

object YGStatus extends Enumeration {
  val placed, cancelled, accepted, packed, delivered, paid = Value
}

object PaymentMethod extends Enumeration {
  val Cash, Card = Value
}

case class YGOrder(
    id: UUID,
    user_id: UUID,
    created_at: Long = System.currentTimeMillis(),
    delivery_address: String,
    lat: Double,
    lon: Double,
    note: String,
    status: String = YGStatus.placed.toString,
    payment_method: String,
    history: String = "",
    var valid_till: Long = 0
) {
  valid_till = created_at + (1000 * 60 * 60 * 24)
}

