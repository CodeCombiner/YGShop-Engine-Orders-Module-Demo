package orders.models
import java.util.UUID
case class YGOrderedReservation(
  id: UUID,
  user_id: UUID,
  p_id: UUID,
  order_id: UUID,
  version: Long = 0,
  created_at: Long = System.currentTimeMillis(),
  valid_till: Long = Long.MaxValue,
  quantity: Int = 1
)

