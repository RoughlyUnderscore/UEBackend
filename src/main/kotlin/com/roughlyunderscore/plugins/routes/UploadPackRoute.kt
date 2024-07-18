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
import com.roughlyunderscore.utils.asPack
import com.roughlyunderscore.utils.saveCodeWithId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import org.apache.commons.lang3.RandomStringUtils
import java.security.SecureRandom

fun Route.uploadPackRoute() {
  post("/upload_pack") {
    coroutineScope {
      val result = validateUpload(setOf("tar")) {
        val pack = result.asPack() ?: run {
          return@validateUpload HttpStatusCode.BadRequest to "Invalid pack syntax"
        }

        val name = pack.metadata.name

        if (pack.enchantments.any { it.name.lowercase().invalidName() }) {
          return@validateUpload HttpStatusCode.BadRequest to "Invalid key in ${pack.enchantments.map {it.name}}. Must be [A-Za-z0-9/._-]."
        }

        if (cachedPacks.any { it.value.metadata.name == name }) {
          return@validateUpload HttpStatusCode.Conflict to "Pack with name ${pack.metadata.name} already exists"
        }

        val id = pack.identifier
        cachedPacks[id] = pack

        val code = RandomStringUtils.random(24, 0, 0, true, true, null, SecureRandom())
        packCodesCollection.saveCodeWithId(id, code)
        packCollection.insertOne(contentProvider(result, id))
        return@validateUpload HttpStatusCode.OK to "Uploaded pack with ID $id. Your unique code is $code. Do not lose it."
      }

      call.respond(result.first, result.second)
    }
  }
}

private val VALID_REGEX = """[a-z0-9. _-]+""".toRegex()
private fun String.invalidName() = !VALID_REGEX.matches(this)