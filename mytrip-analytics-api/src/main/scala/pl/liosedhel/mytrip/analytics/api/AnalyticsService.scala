package pl.liosedhel.mytrip.analytics.api
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import play.api.libs.json._

trait AnalyticsService extends Service {

  import AnalyticsFormatters._

  def getStats(): ServiceCall[NotUsed, Stats]

  override def descriptor: Descriptor = {
    import Service._
    named("worldmapanalytics")
      .withCalls(
        pathCall("/api/world-map/analytics/stats", getStats _)
      )
      .withAutoAcl(true)
      .withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "/api.*")
      )
  }
}

case class Stats(mapsNumber: Int, placesNumber: Int)

object AnalyticsFormatters {
  implicit val statsFormat: Format[Stats] = Json.format[Stats]
}
