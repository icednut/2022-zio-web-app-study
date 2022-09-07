import zhttp.service.{EventLoopGroup, Server, ServerChannelFactory}
import zio._

class UserRepo(ref: Ref[Int])
object UserRepo {

  val layer: ZLayer[Any, Nothing, UserRepo] = ZLayer {
    for {
      ref <- Ref.make[Int](0)
    } yield new UserRepo(ref)
  }
}

class ExternalService
object ExternalService {
  val layer = ZLayer.succeed(new ExternalService)
}

class ReifiedExercise(userRepo: UserRepo, externalService: ExternalService) {
  val layer: ZIO[ExternalService with UserRepo, Nothing, ReifiedExercise] =
    for {
      userRepo <- ZIO.service[UserRepo]
      es <- ZIO.service[ExternalService]
    } yield {
      new ReifiedExercise(userRepo, es)
    }
}

object ReifiedExercise extends ZIOAppDefault {

  // 1. Horizontal Composition
  val depsLayer: ZLayer[Any, Nothing, UserRepo with ExternalService] =
    UserRepo.layer ++ ExternalService.layer

  import zhttp.http._

  val makeServer: ZIO[Any with EventLoopGroup with ServerChannelFactory with Scope, Throwable, Server.Start] = Server.make(Server.app(???).withPort(8090))

  val serverLayer = ZLayer.scoped(makeServer)
//  val run = serverLayer.build *> ZIO.never

  val run = serverLayer.launch
}
