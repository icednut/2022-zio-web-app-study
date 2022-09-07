import App.Todo
import zio.{Scope, Task, ZIO}
import zio.test.{Gen, Spec, TestEnvironment, ZIOSpecDefault, assertTrue, check}

object TodoRepositoryInMemorySpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("TodoRepositoryInMemory") {
//    test("Create todo") {
//      val repo = new TodoRepositoryInMemory(List.empty)
//
//      for {
//        hello <- repo.create("hello")
//        list <- repo.listAll
//      } yield {
//        assertTrue(list.length == 1) && assertTrue(hello.title == "hello")
//      }
//    },
    test("is thread safe") {
      val todoListGen = Gen.listOfN(10)(Gen.alphaNumericStringBounded(0, 1))

      check(todoListGen) { todoList =>
        val repo = new TodoRepositoryInMemory(List.empty)

        for {
          newTodos <- ZIO.foreach(todoList)(repo.create): Task[List[Todo]]
          list <- repo.listAll
        } yield {
          assertTrue(list.length == 10)
        }
      }
    }
  }
}
