/*
 * Copyright 2013 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.heikoseeberger.gabbler

import akka.actor.{ ActorLogging, Props }
import akka.io.IO
import scala.concurrent.duration.DurationInt
import spray.can.Http
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing.{ HttpServiceActor, Route }
import spray.routing.authentication.BasicAuth

object GabblerService {

  def props(hostname: String, port: Int): Props =
    Props(new GabblerService(hostname, port))
}

class GabblerService(hostname: String, port: Int) extends HttpServiceActor with ActorLogging {

  import GabblerService._
  import context.dispatcher

  IO(Http)(context.system) ! Http.Bind(self, hostname, port) // For details see my blog post http://goo.gl/XwOv7P

  override def receive: Receive =
    runRoute(apiRoute ~ staticRoute)

  def apiRoute: Route =
    pathPrefix("api") {
      path("shutdown") {
        get {
          complete {
            val system = context.system
            system.scheduler.scheduleOnce(1 second)(system.shutdown())
            "Shutting down in 1 second ..."
          }
        }
      }
    }

  def staticRoute: Route =
    // format: OFF
    path("") {
      getFromResource("web/index.html")
    } ~
    getFromResourceDirectory("web") // format: ON
}
