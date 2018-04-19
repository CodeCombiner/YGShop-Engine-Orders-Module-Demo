package orders.models.daos

import java.util.UUID

import auth.models.daos.UserDAOTrait
import auth.models.users.User
import orders.models.YGOrder
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.sql.SqlProfile.ColumnOption.{ NotNull, Nullable }

trait OrderDAOTrait extends UserDAOTrait {

  import dbConfig._
  import profile.api._

  protected class OrdersTable(tag: Tag) extends Table[YGOrder](tag, "orders") {

    def id = column[UUID]("id", O.PrimaryKey)

    def user_id = column[UUID]("user_id", NotNull)

    def created_at = column[Long]("created_at", NotNull)

    def delivery_address = column[String]("delivery_address", NotNull)

    def lat = column[Double]("lat", NotNull)

    def lon = column[Double]("lon", NotNull)

    def note = column[String]("note")

    def status = column[String]("status", NotNull)

    def payment_method = column[String]("payment_method", NotNull)

    def history = column[String]("history", NotNull)

    def valid_till = column[Long]("valid_till", NotNull)

    //TODO think can we remove users or not?
    def owner =
      foreignKey("USER_FK", user_id, users)(
        _.id,
        onDelete = ForeignKeyAction.Cascade
      )

    def * =
      (
        id,
        user_id,
        created_at,
        delivery_address,
        lat,
        lon,
        note,
        status,
        payment_method,
        history,
        valid_till
      ) <> ((YGOrder.apply _).tupled, YGOrder.unapply)
  }

  /**
   * The starting point for all queries on the people table.
   */
  protected val orders = TableQuery[OrdersTable]

}
