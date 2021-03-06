package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken.{INVALID_TOKEN, READ_TOKEN, WRITE_TOKEN}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{deleteRoute, insertRoute, recreateSchema}
import org.zalando.spearheads.innkeeper.api.{Error, RouteOut}
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._

class GetRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("get /routes") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should return the correct routes") {
        insertRoute("R1")
        insertRoute("R2")

        val response = getSlashRoutes(token)
        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[RouteOut]]
        routes.size should be(2)
      }

      describe("when filtering the routes by name") {

        it("should return the correct routes") {
          insertRoute("R1")
          insertRoute("R2")
          insertRoute("R3")

          val response = getSlashRoutesByName(List("R1", "R2"), token)
          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val routes = entity.parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(2)
          routes.map(_.id).toSet should be(Set(1L, 2L))
        }

        it("should not return the deleted routes") {
          insertRoute("R1")
          insertRoute("R2")
          deleteRoute(1)

          val response = getSlashRoutesByName("R1", token)
          response.status should be(StatusCodes.OK)

          val routes = entityString(response).parseJson.convertTo[Seq[RouteOut]]
          routes should be('empty)
        }

        it("should return the disabled routes") {
          insertRoute("R1", disableAt = Some(LocalDateTime.now().minusMinutes(3)))
          insertRoute("R2")

          val response = getSlashRoutesByName("R1", token)
          response.status should be(StatusCodes.OK)
          val routes = entityString(response).parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(1)
          routes.map(_.id).toSet should be(Set(1))
        }
      }

      describe("when filtering the routes by team") {
        it("should return the correct routes") {
          insertRoute("R1", ownedByTeam = "team-1")
          insertRoute("R2", ownedByTeam = "team-2")
          insertRoute("R3", ownedByTeam = "team-3")

          val response = getSlashRoutesByTeam(List("team-1", "team-2"), token)
          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val routes = entity.parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(2)
          routes.map(_.id).toSet should be(Set(1L, 2L))
        }

        it("should not return the deleted routes") {
          insertRoute("R1", ownedByTeam = "team-1")
          insertRoute("R2", ownedByTeam = "team-2")
          deleteRoute(1)

          val response = getSlashRoutesByTeam("team-1", token)
          response.status should be(StatusCodes.OK)

          val routes = entityString(response).parseJson.convertTo[Seq[RouteOut]]
          routes should be('empty)
        }

        it("should return the disabled routes") {
          insertRoute("R1", ownedByTeam = "team-1", disableAt = Some(LocalDateTime.now().minusMinutes(3)))
          insertRoute("R2", ownedByTeam = "team-2")

          val response = getSlashRoutesByTeam("team-1", token)
          response.status should be(StatusCodes.OK)
          val routes = entityString(response).parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(1)
          routes.map(_.id).toSet should be(Set(1))
        }

        describe("when filtering the routes by team") {
          it("should return the correct routes") {
            insertRoute("R1", ownedByTeam = "team-1")
            insertRoute("R2", ownedByTeam = "team-2")
            insertRoute("R3", ownedByTeam = "team-3")

            val response = getSlashRoutesByPathId(List(1L, 2L), token)
            response.status should be(StatusCodes.OK)
            val entity = entityString(response)
            val routes = entity.parseJson.convertTo[Seq[RouteOut]]
            routes.size should be(2)
            routes.map(_.id).toSet should be(Set(1L, 2L))
          }
        }
      }

      describe("when filtering the routes by uri") {
        it("should return the correct routes") {
          insertRoute("R1")
          insertRoute("R2")
          insertRoute("R3")

          val response = getSlashRoutesByUri(List("/path-for-R1", "/path-for-R2"), token)
          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val routes = entity.parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(2)
          routes.map(_.id).toSet should be(Set(1L, 2L))
        }

        it("should not return the deleted routes") {
          insertRoute("R1")
          insertRoute("R2")
          deleteRoute(1)

          val response = getSlashRoutesByUri("/path-for-R1", token)
          response.status should be(StatusCodes.OK)

          val routes = entityString(response).parseJson.convertTo[Seq[RouteOut]]
          routes should be('empty)
        }

        it("should return the disabled routes") {
          insertRoute("R1", disableAt = Some(LocalDateTime.now().minusMinutes(3)))
          insertRoute("R2")

          val response = getSlashRoutesByUri("/path-for-R1", token)
          response.status should be(StatusCodes.OK)
          val routes = entityString(response).parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(1)
          routes.map(_.id).toSet should be(Set(1))
        }
      }
    }

    describe("failure") {
      describe("all routes") {
        describe("when no token is provided") {

          it("should return the 401 Unauthorized status") {
            val response = getSlashRoutes()
            response.status should be(StatusCodes.Unauthorized)
          }
        }

        describe("when an invalid token is provided") {
          val token = INVALID_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashRoutes(token)
            response.status should be(StatusCodes.Forbidden)
            entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
          }
        }

        describe("when a token without the read scope is provided") {
          val token = WRITE_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashRoutes(token)
            response.status should be(StatusCodes.Forbidden)
            entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
          }
        }
      }
    }
  }
}
