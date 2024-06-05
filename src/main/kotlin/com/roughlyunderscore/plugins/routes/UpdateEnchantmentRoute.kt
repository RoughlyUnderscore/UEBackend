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
import com.roughlyunderscore.data.server.BackendMetalessEnchantment
import com.roughlyunderscore.data.server.ServerMeta
import com.roughlyunderscore.utils.verifyCodeId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import org.bson.types.Binary

fun Route.updateEnchantmentRoute() {
  post("/update_enchantment") {
    coroutineScope {
      val result = validateUpdate(setOf("json")) {
        val newEnch = gson.fromJson(stringified, BackendMetalessEnchantment.Builder::class.java)?.build()
          ?: return@validateUpdate HttpStatusCode.BadRequest to "Invalid enchantment syntax"

        val name = newEnch.name

        val oldEnchData = enchantmentCollection.find(Filters.eq("id", id)).first()
          ?: return@validateUpdate HttpStatusCode.BadRequest to "No enchantment with ID $id found"
        val oldEnch = gson.fromJson(oldEnchData.get("content", Binary::class.java)?.data?.decodeToString(), BackendMetalessEnchantment.Builder::class.java)?.build()
          ?: return@validateUpdate HttpStatusCode.BadRequest to "Could not find enchantment content"

        if (oldEnch.name != name) {
          return@validateUpdate HttpStatusCode.BadRequest to "The new enchantment name \"$name\" does not match the old enchantment name \"${oldEnch.name}\""
        }

        if (!enchantmentCodesCollection.verifyCodeId(id, code)) return@validateUpdate HttpStatusCode.BadRequest to "The code $code does not match the hash"

        cachedEnchantments[id] = newEnch.toBackendEnchantment(
          ServerMeta.Builder().id(id).downloadedTimes(oldEnchData.getInteger("downloadedTimes")).type("ench").build()
        )
        enchantmentCollection.updateOne(Filters.eq("id", id), Updates.set("content", Binary(this.result)))
        return@validateUpdate HttpStatusCode.OK to "Updated"
      }

      call.respond(result.first, result.second)
    }
  }
}