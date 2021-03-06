package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._

import scala.concurrent.Future

/**
 * @author dpersa
 */
class GetStatusSpec extends FunSpec with Matchers with ScalaFutures {

  override implicit val patienceConfig = PatienceConfig(timeout = Span(60, Seconds), interval = Span(1, Second))

  describe("get /status") {

    it("should return the OK status code") {
      val futureResponse: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = s"${AcceptanceSpecsHelper.baseUri}/status"))
      val response = futureResponse.futureValue
      response.status should be(StatusCodes.OK)
      val entity = response.entity.dataBytes.map(bs => bs.utf8String).runFold("")((a, b) => a + b).futureValue
      entity should be("Ok")
    }
  }
}
