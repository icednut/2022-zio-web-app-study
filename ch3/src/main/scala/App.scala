import zhttp.http._
import zhttp.service.{EventLoopGroup, Server, ServerChannelFactory}
import zio._
import zio.json.{
  DeriveJsonCodec,
  DeriveJsonDecoder,
  DeriveJsonEncoder,
  EncoderOps,
  JsonCodec,
  JsonDecoder,
  JsonEncoder
}

import java.time.LocalDateTime

final case class Todo(id: Long, title: String)

object Todo {
  implicit val TodoJsonCodec: JsonCodec[Todo] = DeriveJsonCodec.gen
}

final case class CreateTodoForm(title: String)

object CreateTodoForm {
  implicit val CreateTodoFormJsonCodec: JsonCodec[CreateTodoForm] =
    DeriveJsonCodec.gen
}

class TodoRepositoryInMemory(var ref: Ref[List[Todo]]) {
  def listAll: Task[List[Todo]] = ref.get

  def findById(id: Long): Task[Option[Todo]] = ref.get.map(_.find(_.id == id))

  def create(title: String): Task[Todo] = ref.modify { list =>
    val newId = list.length + 1
    val newTodo = Todo(newId, title)

    (newTodo, list :+ newTodo)
  }
}

class App(todoRepository: TodoRepositoryInMemory) {

  def app: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case Method.GET -> !! / "todo" =>
      for {
        todoList <- todoRepository.listAll
      } yield {
        Response.json(todoList.toJson)
      }
    case Method.GET -> !! / "todo" / id =>
      (for {
        todo <- ZIO.foreach(id.toLongOption)(todoRepository.findById)
      } yield {
        todo match {
          case Some(i) => Response.json(i.toJson)
          case None    => Response.status(Status.NotFound)
        }
      })
  }
}

// web server를 만들어 보세요
// https://github.com/dream11/zio-http/blob/main/example/src/main/scala/example/HelloWorld.scala
object App extends ZIOAppDefault {

  val myServerLayer: ZLayer[Any with EventLoopGroup with ServerChannelFactory with Scope with App with TodoRepositoryInMemory, Throwable, Server.Start] =
    ZLayer {
      for {
        ref <- Ref.make(
          List(
            Todo(1, "a"),
            Todo(2, "a"),
            Todo(3, "a"),
            Todo(4, "a")
          )
        )
        todoRepo <- ZIO.service[TodoRepositoryInMemory]
        app <- ZIO.service[App]
        server <- Server.make(Server.app(app.app).withPort(8090))
      } yield {
        server
      }
    }

  def run = myServerLayer.launch
}
