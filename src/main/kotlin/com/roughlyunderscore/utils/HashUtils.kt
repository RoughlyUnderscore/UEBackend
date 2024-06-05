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

package com.roughlyunderscore.utils

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.roughlyunderscore.argon2
import org.bson.Document

/**
 * Hashes a [code] and saves it with the [id] of an enchantment/pack.
 */
fun MongoCollection<Document>.saveCodeWithId(id: Long, code: String) = this.insertOne(Document().apply {
  set("id", id)
  set("code", argon2.hash(15, 65536, 1, code.toByteArray(Charsets.UTF_8)))
}).wasAcknowledged()

/**
 * Verifies that the [code] matches the hash for the item of [id].
 */
fun MongoCollection<Document>.verifyCodeId(id: Long, code: String): Boolean {
  val hash = this.find(Filters.eq("id", id)).first()?.getString("code") ?: return false
  return argon2.verify(hash, code.toByteArray())
}