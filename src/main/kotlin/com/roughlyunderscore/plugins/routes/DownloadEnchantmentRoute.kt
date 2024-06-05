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
import com.roughlyunderscore.cachedEnchantments
import com.roughlyunderscore.data.StoredContentType
import com.roughlyunderscore.enchantmentCollection
import com.roughlyunderscore.utils.updateEnchantmentDownloadCount
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import org.bson.types.Binary

fun Route.downloadEnchantmentRoute() {
  get("/download_enchantment") {
    coroutineScope {
      val result = validateDownload(StoredContentType.ENCHANTMENT) {
        val enchantmentData = enchantmentCollection.find(Filters.eq("id", id)).first()
          ?: return@validateDownload HttpStatusCode.BadRequest to "No enchantment with ID $id found"
        val enchantment = enchantmentData.get("content", Binary::class.java).data

        // Modify "downloadedTimes" property of the enchantment
        if (!updateEnchantmentDownloadCount(id)) {
          return@validateDownload HttpStatusCode.InternalServerError to "Failed to update download count"
        }

        cachedEnchantments[id] = cachedEnchantments[id]!!.apply { meta.downloadedTimes++ }

        call.respondBytes(enchantment)
        return@validateDownload HttpStatusCode.OK to "Downloaded"
      }

      call.respond(result.first, result.second)
    }
  }
}