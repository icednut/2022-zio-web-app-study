import zhttp.http._
import zhttp.service.Server
import zio._
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonCodec, JsonDecoder, JsonEncoder}

import java.time.LocalDateTime

// web server를 만들어 보세요
// https://github.com/dream11/zio-http/blob/main/example/src/main/scala/example/HelloWorld.scala
object App extends ZIOAppDefault {
  implicit val todoEncoder: JsonEncoder[Todo] = DeriveJsonEncoder.gen[Todo]
  implicit val todoDecoder: JsonDecoder[Todo] = DeriveJsonDecoder.gen[Todo]
  def run = Server.start(8091, app)

  val todoList = Map(
    1L -> Todo(id = 1L, message = "Hello TODO"),
    2L -> Todo(id = 2L, message = "Hello TODO2"),
    3L -> Todo(id = 3L, message = "Hello TODO3")
  )

  val app = Http.collect[Request] {
    case Method.GET -> !! / "todo" =>
      Response.json(todoList.toJson)
    case Method.GET -> !! / "todo" / id =>
      (for {
        actualId <- id.toLongOption
        todo <- todoList.get(actualId)
      } yield {
        todo
      }) match {
        case Some(todo) => Response.json(todo.toJson)
        case None => Response.status(Status.NotFound)
      }
  }

  case class Todo(id: Long, message: String)
}
