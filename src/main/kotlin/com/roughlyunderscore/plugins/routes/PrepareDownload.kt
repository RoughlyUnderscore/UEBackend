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

import com.roughlyunderscore.cachedEnchantments
import com.roughlyunderscore.cachedLocales
import com.roughlyunderscore.cachedPacks
import com.roughlyunderscore.data.StoredContentType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.coroutineScope

suspend fun PipelineContext<Unit, ApplicationCall>.validateDownload(
  type: StoredContentType,
  action: suspend DownloadContext.() -> Pair<HttpStatusCode, String>
): Pair<HttpStatusCode, String> = coroutineScope {
  val id = call.parameters["id"]?.toLongOrNull() ?: return@coroutineScope HttpStatusCode.BadRequest to "Missing ID"

  when (type) {
    StoredContentType.PACK -> if (cachedPacks[id] == null) return@coroutineScope HttpStatusCode.NotFound to "Pack not found"
    StoredContentType.ENCHANTMENT -> if (cachedEnchantments[id] == null) return@coroutineScope HttpStatusCode.NotFound to "Enchantment not found"
    StoredContentType.LOCALE -> if (cachedLocales[id] == null) return@coroutineScope HttpStatusCode.NotFound to "Locale not found"
  }

  call.response.header(
    HttpHeaders.ContentDisposition,
    ContentDisposition.File.withParameter(ContentDisposition.Parameters.FileName, "$id.${when (type) {
      StoredContentType.PACK -> "tar"
      StoredContentType.ENCHANTMENT, StoredContentType.LOCALE -> "json"
    }}").toString()
  )

  return@coroutineScope DownloadContext(id).action()
}

data class DownloadContext(
  val id: Long,
)