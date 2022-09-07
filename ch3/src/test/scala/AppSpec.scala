import App.Todo
import sttp.client3.{HttpClientSyncBackend, Identity, Response, UriContext, basicRequest}
import sttp.model.StatusCode
import zhttp.http.{Request, Status}
import zhttp.service.{EventLoopGroup, Server, ServerChannelFactory}
import zhttp.service.server.ServerChannelFactory
import zio.test._
import zio._
import zio.json.DecoderOps

// 서버를 테스트해보세요
// https://zio.dev/reference/test/
// https://sttp.softwaremill.com/en/latest/quickstart.html
// https://sttp.softwaremill.com/en/latest/backends/zio.html

object AppSpec extends ZIOSpecDefault {

  val appLayer: ZLayer[ServerChannelFactory with EventLoopGroup, Throwable, Server.Start] = ZLayer.scoped {
    zhttp.service.Server.app(App.app)
      .withPort(0)
      .make
  }
  override def spec = suite("App")(
    test("get one todo item test") {
      val backend = HttpClientSyncBackend()
      val result = basicRequest.get(uri"http://localhost:8091/todo/1").send(backend)

      val responseCodeAssertion = assertTrue(result.code == StatusCode.Ok)
      val responseBodyAssertion = (for {
        body <- result.body
        actualTodoItem <- body.fromJson[Todo]
        expectedTodoItem <- App.todoList.get(1L).toRight("EMPTY")
      } yield {
        assertTrue(actualTodoItem == expectedTodoItem)
      }).getOrElse(assertTrue(false))

      responseCodeAssertion && responseBodyAssertion
    },
    test("get todo item list test") {
      val backend = HttpClientSyncBackend()
      val result = basicRequest.get(uri"http://localhost:8091/todo").send(backend)

      val responseCodeAssertion = assertTrue(result.code == StatusCode.Ok)
      val responseBodyAssertion = (for {
        body <- result.body
        actualTodoList <- body.fromJson[Map[Long, Todo]]
      } yield {
        assertTrue(actualTodoList == App.todoList)
      }).getOrElse(assertTrue(false))

      responseCodeAssertion && responseBodyAssertion
    }
  ).provideShared(
    appLayer,
    ServerChannelFactory.auto,
    EventLoopGroup.auto(1)
  )
}
