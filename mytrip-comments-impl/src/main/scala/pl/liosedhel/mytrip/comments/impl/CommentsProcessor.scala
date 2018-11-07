package pl.liosedhel.mytrip.comments.impl
import akka.Done
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}
import org.slf4j.{Logger, LoggerFactory}
import pl.liosedhel.mytrip.comments.impl.PlaceCommentAggregate.{CommentCreated, PlaceCommentEvent}

class CommentsProcessor(commentRepository: CommentRepository)
  extends ReadSideProcessor[PlaceCommentEvent] {

  private final val log: Logger =
    LoggerFactory.getLogger(classOf[CommentsProcessor])

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[PlaceCommentEvent] = { () =>
    Flow
      .apply[EventStreamElement[PlaceCommentEvent]]
      .collect {
        case EventStreamElement(_, event: CommentCreated, _) =>
          log.info(s"New comment place was created $event")
          commentRepository.save(event); Done
      }
  }


  override def aggregateTags = Set(PlaceCommentAggregate.PlaceCommentEvent.Tag)
}
