package cmwell.util.cache

import java.util.concurrent.atomic.AtomicBoolean
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Proj: server
  * User: gilad
  * Date: 2/4/18
  * Time: 11:38 AM
  */
class TimeBasedAccumulatedCache[T,K,V1,V2] private(/*@volatile*/ private[this] var mainCache: Map[K,(V1,V2)],
                                                   /*@volatile*/ private[this] var v1Cache: Map[V1,K],
                                                   /*@volatile*/ private[this] var v2Cache: Map[V2,K],
                                                   seedTimestamp: Long,
                                                   coolDownMillis: Long,
                                                   accumulateSince: Long => Future[T],
                                                   timestampAndEntriesFromResults: T => (Long,Map[K,(V1,V2)]),
                                                   getSingle: K => Future[(V1,V2)]) { self: LazyLogging =>

  private[this] val isBeingUpdated = new AtomicBoolean(false)
  /*@volatile*/ private[this] var timestamp: Long = seedTimestamp
  /*@volatile*/ private[this] var checkTime: Long = System.currentTimeMillis()

//  My original thought was to have a black list of keys that has been asked for
//  to prevent abuse of asking the same non existing key repeatedly.
//  This may be redundant, since `updatedRecently` guards us from doing it too frequently...
//
//  @volatile private[this] var blacklist: Map[K,Long] = Map.empty

  @inline def get(key: K): Option[(V1,V2)] = mainCache.get(key)
  @inline def getByV1(v1: V1): Option[K] = v1Cache.get(v1)
  @inline def getByV2(v2: V2): Option[K] = v2Cache.get(v2)

  def getOrElseUpdate(key: K)(implicit ec: ExecutionContext): Future[Option[(V1,V2)]] = get(key).fold {
    if(updatedRecently || !isBeingUpdated.compareAndSet(false,true)) Future.successful(None)
    else Try(accumulateSince(timestamp)).fold(Future.failed,identity).flatMap { results =>
      val (newTimestamp, newVals) = /*Try(*/timestampAndEntriesFromResults(results)/*).recover {
        case err: Throwable =>
          logger.error(s"fetchTimeStampAndEntriesFromResults threw an exception", err)
          (timestamp, Map.empty[K,V])
      }.get*/
      checkTime = System.currentTimeMillis()
      if(newVals.isEmpty) Future.successful(None)
      else {
        mainCache ++= newVals
        val (v1Additions,v2Additions) = TimeBasedAccumulatedCache.getInvertedCaches(newVals)
        v1Cache ++= v1Additions
        v2Cache ++= v2Additions
        timestamp = newTimestamp
        newVals.get(key).fold[Future[Option[(V1,V2)]]] {
          Try(getSingle(key)).fold(Future.failed,identity).map { case v@(v1,v2) =>
            mainCache += key -> v
            v1Cache += v1 -> key
            v2Cache += v2 -> key
            Some(v)
          }
        }(Future.successful[Option[(V1,V2)]] _ compose Some.apply)
      }
    }.andThen { case _ => isBeingUpdated.set(false) }
  }(Future.successful[Option[(V1,V2)]] _ compose Some.apply)

  def invalidate(k: K)(implicit ec: ExecutionContext): Future[Unit] = {
    mainCache.get(k).fold(Future.successful(())) { case (v1, v2) =>
      acquireLock().map { _ =>
        if (mainCache.contains(k)) {
          mainCache -= k
          v1Cache -= v1
          v2Cache -= v2
        }
        isBeingUpdated.set(false)
      }
    }
  }

  private[this] def updatedRecently: Boolean =
    System.currentTimeMillis() - checkTime <= coolDownMillis

  private[this] def acquireLock(maxDepth: Int = 16)(implicit ec: ExecutionContext): Future[Unit] = {
    if(maxDepth == 0) Future.failed(new IllegalStateException(s"too many tries to acquire TimeBasedAccumulatedCache lock failed"))
    else if(isBeingUpdated.compareAndSet(false,true)) Future.successful(())
    else cmwell.util.concurrent.SimpleScheduler.scheduleFuture(40.millis)(acquireLock(maxDepth - 1))
  }
}


object TimeBasedAccumulatedCache {


  private def getInvertedCaches[K,V1,V2](m: Map[K,(V1,V2)]): (Map[V1,K],Map[V2,K]) = {
    m.foldLeft(Map.empty[V1,K] -> Map.empty[V2,K]) {
      case ((accv1,accv2),(k,(v1,v2))) =>
        accv1.updated(v1, k) -> accv2.updated(v2, k)
    }
  }

  def apply[T,K,V1,V2](seed: Map[K,(V1,V2)], seedTimestamp: Long, coolDown: FiniteDuration)
                  (accumulateSince: Long => Future[T])
                  (timestampAndEntriesFromResults: T => (Long,Map[K,(V1,V2)]))
                  (getSingle: K => Future[(V1,V2)]): TimeBasedAccumulatedCache[T,K,V1,V2] = {
    val (v1,v2) = getInvertedCaches(seed)
    new TimeBasedAccumulatedCache(seed,v1,v2,
      seedTimestamp,
      coolDown.toMillis,
      accumulateSince,
      timestampAndEntriesFromResults,
      getSingle) with LazyLogging
  }
}