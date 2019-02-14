package com.codacy.stream

import java.io.File

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.math.Ordering
import scala.collection.mutable.PriorityQueue

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.stage._

import akka.codacy.AkkaInternal

import com.typesafe.config.Config

class ChronicleQueueSource[T](val queue: PersistentQueue[T])(
    implicit serializer: QueueSerializer[T],
    system: ActorSystem
) extends GraphStage[SourceShape[T]] {

  def this(config: Config)(implicit serializer: QueueSerializer[T], system: ActorSystem) =
    this(new PersistentQueue[T](config))

  def this(persistDir: File)(implicit serializer: QueueSerializer[T], system: ActorSystem) =
    this(new PersistentQueue[T](persistDir))

  private val out = Outlet[T]("PersistentBuffer.out")
  override val shape: SourceShape[T] = SourceShape.of(out)

  val defaultOutputPort = 0
  protected def elementOut(e: Event[T]): T = e.entry

  protected def autoCommit(index: Long) = queue.commit(defaultOutputPort, index, false)

  protected def verifyCommit(index: Long) = queue.verifyCommitOrder(defaultOutputPort, index)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {

      private val successCallback = getAsyncCallback[Event[T]](handleSuccess)

      private val failureCallback = getAsyncCallback[Exception](handleFailure)

      val pendingOffers = new PriorityQueue[Event[T]]()(new Ordering[Event[T]] {
        def compare(x: Event[T], y: Event[T]) = // inverse order
          Ordering.Long.compare(y.index, x.index)
      })

      var terminating = false

      override def postStop(): Unit = {
        terminating = true
      }

      def receiveMessages(): Unit = {
        // this future has to stay on the calling thread ... the package is so forced...
        Future { queue.dequeue() }(AkkaInternal.sameThreadExecutionContext).onComplete {
          case Success(el) =>
            el match {
              case Some(element) =>
                successCallback.invoke(element)
              case None =>
                successCallback.invoke(null.asInstanceOf[Event[T]])
            }
          case Failure(e) =>
            e match {
              case ex: Exception =>
                failureCallback.invoke(ex)
              case th: Throwable => throw th
            }
        }(AkkaInternal.sameThreadExecutionContext)
        //(system.dispatcher) this will shuffle the order of insertion

        // another possible implementation that could be benchmarked
        // scala.util.Try { queue.dequeue() } match {
        //   case Success(el) =>
        //     el match {
        //       case Some(element) =>
        //         successCallback.invoke(element)
        //       case None =>
        //         // pauser.pause()
        //         successCallback.invoke(null.asInstanceOf[Event[T]])
        //     }
        //   case Failure(e) =>
        //     e match {
        //       case ex: Exception =>
        //         failureCallback.invoke(ex)
        //       case th: Throwable => throw th
        //     }
        // }
      }

      def handleFailure(ex: Exception): Unit =
        failStage(ex)

      def commitOrEnqueue(elem: Event[T]): Boolean = {
        try {
          verifyCommit(elem.index)
          push(out, elementOut(elem))
          autoCommit(elem.index)
          true
        } catch {
          case _: Throwable =>
            pendingOffers.enqueue(elem)
            false
        }
      }

      def handleSuccess(result: Event[T]): Unit = {
        if (!pendingOffers.isEmpty && isAvailable(out)) {
          if (result != null)
            pendingOffers.enqueue(result)

          commitOrEnqueue(pendingOffers.dequeue)
        } else if (pendingOffers.isEmpty && isAvailable(out) && result != null) {
          commitOrEnqueue(result)
        } else if (pendingOffers.isEmpty && !isAvailable(out) && result != null) {
          pendingOffers.enqueue(result)
        }

        receiveMoreOrComplete()
      }

      private def receiveMoreOrComplete(): Unit =
        if (!terminating) {
          receiveMessages()
        } else if (terminating) {
          completeStage()
        }

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            if (!pendingOffers.isEmpty) {
              var commited = commitOrEnqueue(pendingOffers.dequeue)
              while (!pendingOffers.isEmpty && isAvailable(out) && commited) {
                commited = commitOrEnqueue(pendingOffers.dequeue)
              }
              receiveMoreOrComplete()
            } else {
              receiveMessages()
            }
          }
        }
      )
    }
  }
}
