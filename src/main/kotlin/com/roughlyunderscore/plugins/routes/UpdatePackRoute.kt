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
import com.roughlyunderscore.utils.asPack
import com.roughlyunderscore.utils.verifyCodeId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import org.bson.types.Binary

fun Route.updatePackRoute() {
  post("/update_pack") {
    coroutineScope {
      val result = validateUpdate(setOf("tar")) {
        val newPack = result.asPack(id = id) ?: return@validateUpdate HttpStatusCode.BadRequest to "Invalid pack syntax"

        val name = newPack.metadata.name

        val oldPackData = packCollection.find(Filters.eq("id", id)).first() ?: return@validateUpdate HttpStatusCode.BadRequest to "No pack with ID $id found"
        val oldPack = oldPackData.get("content", Binary::class.java)?.data?.asPack(id = id) ?: return@validateUpdate HttpStatusCode.BadRequest to "Could not find pack content"

        if (oldPack.metadata.name != name) {
          return@validateUpdate HttpStatusCode.BadRequest to "The new pack name \"$name\" does not match the old pack name \"${oldPack.metadata.name}\""
        }

        if (!packCodesCollection.verifyCodeId(id, code)) return@validateUpdate HttpStatusCode.BadRequest to "The code $code does not match the hash"

        cachedPacks[id] = newPack
        packCollection.updateOne(Filters.eq("id", id), Updates.set("content", Binary(this.result)))
        return@validateUpdate HttpStatusCode.OK to "Updated"

        /*val meta = ServerMeta.Builder().id(id).type("pack").build()
        datastore.save(meta)

        val resultingFile = File("uploads").resolve("packs").resolve("$id+$originalName")
        FileUtils.writeStringToFile(resultingFile, stringified, Charsets.UTF_8)*/
      }

      call.respond(result.first, result.second)
    }
  }
}