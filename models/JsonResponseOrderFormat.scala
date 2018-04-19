package orders.models

case class JsonResponseOrderFormat(
  order_info: YGOrder,
  products: Seq[YGOrderedReservation]
)

