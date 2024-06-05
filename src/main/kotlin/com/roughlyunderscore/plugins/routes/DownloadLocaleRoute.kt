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

import com.mongodb.client.model.Filters
import com.roughlyunderscore.cachedLocales
import com.roughlyunderscore.data.StoredContentType
import com.roughlyunderscore.localeCollection
import com.roughlyunderscore.utils.updateLocaleDownloadCount
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import org.bson.types.Binary

fun Route.downloadLocaleRoute() {
  get("/download_locale") {
    coroutineScope {
      val result = validateDownload(StoredContentType.LOCALE) {
        val localeData = localeCollection.find(Filters.eq("id", id)).first()
          ?: return@validateDownload HttpStatusCode.BadRequest to "No locale with ID $id found"
        val locale = localeData.get("content", Binary::class.java).data

        // Modify "downloadedTimes" property of the enchantment
        if (!updateLocaleDownloadCount(id)) {
          return@validateDownload HttpStatusCode.InternalServerError to "Failed to update download count"
        }

        cachedLocales[id] = cachedLocales[id]!!.apply { meta.downloadedTimes++ }

        call.respondBytes(locale)
        return@validateDownload HttpStatusCode.OK to "Downloaded"
      }

      call.respond(result.first, result.second)
    }
  }
}