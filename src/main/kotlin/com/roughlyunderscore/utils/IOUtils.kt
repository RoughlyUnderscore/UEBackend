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

import com.roughlyunderscore.data.server.*
import com.roughlyunderscore.gson
import com.roughlyunderscore.latestPackId
import com.roughlyunderscore.ulib.io.tempUntarOperate
import java.io.InputStream

fun ByteArray.asPack(then: () -> Unit? = {}, id: Long = ++latestPackId): BackendEnchantmentPack? {
  return (this.inputStream().use { it.asPack(id) }).apply { then() }
}

private fun InputStream.asPack(id: Long = ++latestPackId): BackendEnchantmentPack? = this.tempUntarOperate(listOf("json")) { files ->
  val enchantments = mutableListOf<BackendMetalessEnchantment>()

  var metadata: BackendPackMetadata? = null
  for (file in files) {
    if (file.isDirectory) continue
    if (file.name.equals("pack_metadata.json", true)) {
      metadata = file.reader().use { gson.fromJson(it, BackendPackMetadata::class.java) }
      continue
    } else if (file.name.endsWith(".json")) {
      val enchantment = file.reader().use { gson.fromJson(it, BackendMetalessEnchantment.Builder::class.java) }
      if (enchantment != null) enchantments.add(enchantment.id(0).build()) // IDs for pack enchantments are not important
    }
  }

  if (metadata == null) return@tempUntarOperate null

  metadata.meta = ServerMeta.PACK_PROVIDER(id)

  BackendEnchantmentPack.Builder()
    .id(id)
    .metadata(metadata)
    .enchantments(enchantments)
    .build()
}