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
import com.mongodb.client.model.Updates
import com.roughlyunderscore.*
import com.roughlyunderscore.data.UELocale
import com.roughlyunderscore.data.server.BackendLocale
import com.roughlyunderscore.data.server.ServerMeta
import com.roughlyunderscore.utils.verifyCodeId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import org.bson.types.Binary

fun Route.updateLocaleRoute() {
  post("/update_locale") {
    coroutineScope {
      val result = validateUpdate(setOf("json")) {
        val newLocale = gson.fromJson(stringified, UELocale::class.java)
          ?: return@validateUpdate HttpStatusCode.BadRequest to "Invalid locale syntax"

        val name = newLocale.localeIdentifier

        val oldLocaleData = localeCollection.find(Filters.eq("id", id)).first()
          ?: return@validateUpdate HttpStatusCode.BadRequest to "No locale with ID $id found"
        val oldLocale = gson.fromJson(oldLocaleData.get("content", Binary::class.java)?.data?.decodeToString(), UELocale::class.java)
          ?: return@validateUpdate HttpStatusCode.BadRequest to "Could not find locale content"

        if (oldLocale.localeIdentifier != name) {
          return@validateUpdate HttpStatusCode.BadRequest to "The new locale name \"$name\" does not match the old locale name \"${oldLocale.localeIdentifier}\""
        }

        if (!localeCodesCollection.verifyCodeId(id, code)) {
          return@validateUpdate HttpStatusCode.BadRequest to "The code $code does not match the hash"
        }

        cachedLocales[id] = BackendLocale.Builder().locale(newLocale)
          .meta(ServerMeta.LOCALE_PROVIDER(id).apply {
            this.downloadedTimes = oldLocaleData.getInteger("downloadedTimes")
          })
          .build()

        localeCollection.updateOne(Filters.eq("id", id), Updates.set("content", Binary(this.result)))
        call.respond(HttpStatusCode.OK, "Updated")
        return@validateUpdate HttpStatusCode.OK to "Updated locale"
      }

      call.respond(result.first, result.second)
    }
  }
}