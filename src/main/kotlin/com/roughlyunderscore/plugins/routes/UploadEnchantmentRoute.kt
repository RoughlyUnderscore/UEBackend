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
import com.roughlyunderscore.cachedEnchantments
import com.roughlyunderscore.data.server.BackendMetalessEnchantment
import com.roughlyunderscore.data.server.ServerMeta
import com.roughlyunderscore.gson
import com.roughlyunderscore.latestEnchantmentId
import com.roughlyunderscore.utils.saveCodeWithId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import org.apache.commons.lang3.RandomStringUtils
import java.security.SecureRandom

fun Route.uploadEnchantmentRoute() {
  post("/upload_enchantment") {
    coroutineScope {
      val result = validateUpload(setOf("json")) {
        val id = ++latestEnchantmentId

        val enchantment = (gson.fromJson(stringified, BackendMetalessEnchantment.Builder::class.java) ?: run {
          return@validateUpload HttpStatusCode.BadRequest to "Invalid enchantment syntax"
        }).id(id).build().toBackendEnchantment(ServerMeta.ENCH_PROVIDER(id))

        if (cachedEnchantments.any { it.value.name == enchantment.name }) {
          return@validateUpload HttpStatusCode.Conflict to "Enchantment with name ${enchantment.name} already exists"
        }

        cachedEnchantments[id] = enchantment

        /*val meta = ServerMeta.Builder().id(id).type("ench").build()
        datastore.save(meta)

        val resultingFile = File("uploads").resolve("enchs").resolve("$id+$originalName")
        FileUtils.writeStringToFile(resultingFile, stringified, Charsets.UTF_8)*/

        val code = RandomStringUtils.random(24, 0, 0, true, true, null, SecureRandom())
        enchantmentCodesCollection.saveCodeWithId(id, code)
        enchantmentCollection.insertOne(contentProvider(result, id))
        return@validateUpload HttpStatusCode.OK to "Uploaded enchantment with ID $id. Your unique code is $code. Do not lose it."
      }

      call.respond(result.first, result.second)
    }
  }
}