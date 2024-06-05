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
import com.mongodb.client.model.Updates
import com.roughlyunderscore.enchantmentCollection
import com.roughlyunderscore.localeCollection
import com.roughlyunderscore.packCollection
import org.bson.Document

val idFilter = { id: Long -> Filters.eq("id", id) }
val downloadCountUpdate = { Updates.inc("downloadedTimes", 1) }

fun updateEnchantmentDownloadCount(id: Long) = enchantmentCollection.updateDownloadCount(id)
fun updatePackDownloadCount(id: Long) = packCollection.updateDownloadCount(id)
fun updateLocaleDownloadCount(id: Long) = localeCollection.updateDownloadCount(id)

fun MongoCollection<Document>.updateDownloadCount(id: Long) = updateOne(idFilter(id), downloadCountUpdate()).wasAcknowledged()