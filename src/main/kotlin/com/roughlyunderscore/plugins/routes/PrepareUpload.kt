// Copyright 2024 RoughlyUnderscore
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.roughlyunderscore.plugins.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.coroutineScope
import kotlin.math.pow

suspend fun PipelineContext<Unit, ApplicationCall>.validateUpload(
  exts: Set<String>,
  action: suspend UploadContext.() -> Pair<HttpStatusCode, String>
): Pair<HttpStatusCode, String> = coroutineScope {
  val part = call.receiveMultipart().readPart() ?: return@coroutineScope HttpStatusCode.BadRequest to "No file found"

  if (part !is PartData.FileItem) {
    part.dispose()
    return@coroutineScope HttpStatusCode.BadRequest to "No file found"
  }

  val name = part.originalFileName!!
  val ext = name.substringAfterLast('.', "")

  if (ext !in exts) {
    part.dispose()
    return@coroutineScope HttpStatusCode.BadRequest to "Invalid file type"
  }

  val bytes = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
  val mb = bytes?.div(2.0.pow(20.0))

  if (mb == null || mb > 1.0) {
    part.dispose()
    return@coroutineScope HttpStatusCode.PayloadTooLarge to "Too large (limit: 1MB)"
  }

  val result = part.streamProvider().readAllBytes()
  val stringified = result.decodeToString()

  val response = UploadContext(name, ext, result, stringified).action()

  part.dispose()
  return@coroutineScope response
}

data class UploadContext(
  val originalName: String,
  val ext: String,
  val result: ByteArray,
  val stringified: String
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UploadContext

    if (originalName != other.originalName) return false
    if (ext != other.ext) return false
    if (!result.contentEquals(other.result)) return false
    if (stringified != other.stringified) return false

    return true
  }

  override fun hashCode(): Int {
    var result1 = originalName.hashCode()
    result1 = 31 * result1 + ext.hashCode()
    result1 = 31 * result1 + result.contentHashCode()
    result1 = 31 * result1 + stringified.hashCode()
    return result1
  }
}