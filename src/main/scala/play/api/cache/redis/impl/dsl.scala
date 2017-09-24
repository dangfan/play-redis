package play.api.cache.redis.impl

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

import play.api.cache.redis._

/** Implicit helpers used within the redis cache implementation. These
  * handful tools simplifies code readability but has no major function.
  *
  * @author Karel Cemus
  */
private[ impl ] object dsl {

  @inline implicit def runtime2context( implicit runtime: RedisRuntime ): ExecutionContext = runtime.context

  /** enriches any ref by toFuture converting a value to Future.successful */
  implicit class RichFuture[ T ]( val any: T ) extends AnyVal {
    @inline def toFuture: Future[ T ] = Future.successful( any )
  }

  /** helper function enabling us to recover from command execution */
  implicit class RecoveryFuture[ T ]( val future: Future[ T ] ) extends AnyVal {

    /** Transforms the promise into desired builder results, possibly recovers with provided default value */
    @inline def recoverWithDefault[ Result[ X ] ]( default: => T )( implicit builder: Builders.ResultBuilder[ Result ], runtime: RedisRuntime ): Result[ T ] =
      builder.toResult( future, Future.successful( default ) )

    /** recovers from the execution but returns future, not Result */
    @inline def recoverWithFuture( default: => Future[ T ] )( implicit runtime: RedisRuntime ): Future[ T ] =
      future recoverWith {
        // recover from known exceptions
        case failure: RedisException => runtime.policy.recoverFrom( future, default, failure )
      }
  }

  /** helper function enabling us to recover from command execution */
  implicit class RecoveryUnitFuture( val future: Future[ Unit ] ) extends AnyVal {
    /** Transforms the promise into desired builder results, possibly recovers with provided default value */
    @inline def recoverWithDone[ Result[ X ] ]( implicit builder: Builders.ResultBuilder[ Result ], runtime: RedisRuntime ): Result[ Done ] =
      builder.toResult( future.map( unitAsDone ), Future.successful( Done ) )
  }

  /** maps units into akka.Done */
  @inline private def unitAsDone( unit: Unit ) = Done
}
