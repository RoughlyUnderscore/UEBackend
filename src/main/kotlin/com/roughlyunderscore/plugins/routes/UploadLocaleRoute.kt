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

import com.roughlyunderscore.*
import com.roughlyunderscore.data.UELocale
import com.roughlyunderscore.data.server.BackendLocale
import com.roughlyunderscore.data.server.ServerMeta
import com.roughlyunderscore.utils.saveCodeWithId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import org.apache.commons.lang3.RandomStringUtils
import java.security.SecureRandom

fun Route.uploadLocaleRoute() {
  post("/upload_locale") {
    coroutineScope {
      val result = validateUpload(setOf("json")) {
        val id = ++latestLocaleId

        val locale = (gson.fromJson(stringified, UELocale::class.java) ?: run {
          return@validateUpload HttpStatusCode.BadRequest to "Invalid locale syntax"
        })

        val backendLocale = BackendLocale.Builder().locale(locale).meta(ServerMeta.LOCALE_PROVIDER(id)).build()

        if (cachedLocales.any { it.value.locale.localeIdentifier == backendLocale.locale.localeIdentifier }) {
          return@validateUpload HttpStatusCode.Conflict to "Locale with name ${backendLocale.locale.localeIdentifier} already exists"
        }

        cachedLocales[id] = backendLocale

        val code = RandomStringUtils.random(24, 0, 0, true, true, null, SecureRandom())
        localeCodesCollection.saveCodeWithId(id, code)
        localeCollection.insertOne(contentProvider(result, id))
        return@validateUpload HttpStatusCode.OK to "Uploaded locale with ID $id. Your unique code is $code. Do not lose it."
      }

      call.respond(result.first, result.second)
    }
  }
}