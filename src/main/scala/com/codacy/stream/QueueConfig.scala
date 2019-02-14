// ORIGINAL LICENCE
/*
 *  Copyright 2017 PayPal
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/* The original file contents has been modified
 * to better fit chronichle-queue-stream needs and structure.
 */
package com.codacy.stream

import java.io.File

import scala.util.Try

import com.typesafe.config.ConfigException.BadValue
import com.typesafe.config.Config
import net.openhft.chronicle.queue.{RollCycle, RollCycles}
import net.openhft.chronicle.wire.WireType

object QueueConfig {

  val defaultCycle: RollCycle = RollCycles.DAILY
  val defaultWireType: WireType = WireType.BINARY
  val defaultBlockSize: Long = 64L << 20
  val defaultOutputPort: Int = 1
  val defaultCommitOrderPolicy = Lenient

  def from(config: Config): QueueConfig = {
    val persistDir = new File(config.getString("persist-dir"))
    val cycle = Try(config.getString("roll-cycle")).toOption.map{ s =>
      RollCycles.valueOf(s.toUpperCase)
    } getOrElse defaultCycle
    val wireType = Try(config.getString("wire-type")).toOption.map{ s =>
      WireType.valueOf(s.toUpperCase)
    } getOrElse defaultWireType
    val blockSize = Try(config.getMemorySize("block-size")).toOption map (_.toBytes) getOrElse defaultBlockSize
    val indexSpacing =
      Try(config.getMemorySize("index-spacing")).toOption.map(_.toBytes.toInt).getOrElse(cycle.defaultIndexSpacing)
    val indexCount = Try(config.getInt("index-count")).toOption.getOrElse(cycle.defaultIndexCount)
    val outputPorts = Try(config.getInt("output-ports")).toOption.getOrElse(defaultOutputPort)
    val commitOrder = Try(config.getString("commit-order-policy")).toOption.map{ s =>
      if (s == "strict") Strict
      else if (s == "lenient") Lenient
      else throw new BadValue("commit-order-policy", "Allowed values: strict or lenient")
    } getOrElse defaultCommitOrderPolicy
    QueueConfig(
      persistDir,
      cycle,
      wireType,
      blockSize,
      indexSpacing,
      indexCount,
      outputPorts = outputPorts,
      commitOrderPolicy = commitOrder
    )
  }
}

case class QueueConfig(
    persistDir: File,
    rollCycle: RollCycle = QueueConfig.defaultCycle,
    wireType: WireType = QueueConfig.defaultWireType,
    blockSize: Long = QueueConfig.defaultBlockSize,
    indexSpacing: Int = QueueConfig.defaultCycle.defaultIndexSpacing,
    indexCount: Int = QueueConfig.defaultCycle.defaultIndexCount,
    isBuffered: Boolean = false,
    epoch: Long = 0L,
    outputPorts: Int = QueueConfig.defaultOutputPort,
    commitOrderPolicy: CommitOrderPolicy = QueueConfig.defaultCommitOrderPolicy
)

sealed trait CommitOrderPolicy
object Strict extends CommitOrderPolicy
object Lenient extends CommitOrderPolicy
