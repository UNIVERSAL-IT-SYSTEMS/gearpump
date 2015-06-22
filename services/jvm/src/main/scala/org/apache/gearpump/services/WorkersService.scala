/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gearpump.services

import akka.actor.{ActorSystem, ActorRef}
import akka.pattern._
import org.apache.gearpump.cluster.AppMasterToMaster.{GetAllWorkers, GetWorkerData}
import org.apache.gearpump.shared.Messages.{WorkerData, WorkerDescription, WorkerList}
import org.apache.gearpump.util.{LogUtil, Constants}
import spray.http.StatusCodes
import spray.routing.HttpService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait WorkersService extends HttpService {
  import upickle._
  def master:ActorRef
  implicit val system: ActorSystem
  private val LOG = LogUtil.getLogger(getClass)

  def workersRoute = get {
    implicit val ec: ExecutionContext = actorRefFactory.dispatcher
    implicit val timeout = Constants.FUTURE_TIMEOUT
    pathPrefix("api"/s"$REST_VERSION") {
      path("workers") {
        def workerDataFuture = (master ? GetAllWorkers).asInstanceOf[Future[WorkerList]].flatMap { workerList =>
          val workers = workerList.workers
          val workerDataList = List.empty[WorkerDescription]
          Future.fold(workers.map(master ? GetWorkerData(_)))(workerDataList) { (workerDataList, workerData) =>
            val workerDescription = workerData.asInstanceOf[WorkerData].workerDescription
            LOG.info(s"get worker description $workerDescription")
            if (workerDescription.isEmpty) {
              workerDataList
            } else {
              workerDataList :+ workerDescription.get
            }
          }
        }
        onComplete(workerDataFuture) {
          case Success(result: List[WorkerDescription]) => complete(write[List[WorkerDescription]](result))
          case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }
}