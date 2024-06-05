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

package com.roughlyunderscore.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.roughlyunderscore.data.server.BackendPackMetadata
import com.roughlyunderscore.ulib.json.anyArrayOfStrings
import com.roughlyunderscore.ulib.json.anyString
import com.roughlyunderscore.ulib.json.onAnyString
import org.bukkit.Material
import java.lang.reflect.Type

object PackMetadataDeserializer : JsonDeserializer<BackendPackMetadata?> {
  override fun deserialize(src: JsonElement?, type: Type?, ctx: JsonDeserializationContext?): BackendPackMetadata? {
    val pack = src?.asJsonObject ?: return null

    val builder = BackendPackMetadata.Builder()

    builder.name(pack.anyString(DeserializationNames.PackMetadata.NAME) ?: return null)
    builder.version(pack.anyString(DeserializationNames.PackMetadata.VERSION) ?: return null)

    builder.authors(pack.anyArrayOfStrings(DeserializationNames.PackMetadata.AUTHORS) ?: emptyList())
    builder.description(pack.anyArrayOfStrings(DeserializationNames.PackMetadata.DESCRIPTION) ?: emptyList())
    builder.website(pack.anyString(DeserializationNames.PackMetadata.WEBSITE) ?: return null)
    builder.worldBlacklist(pack.anyArrayOfStrings(DeserializationNames.PackMetadata.WORLD_BLACKLIST) ?: emptyList())
    builder.worldWhitelist(pack.anyArrayOfStrings(DeserializationNames.PackMetadata.WORLD_WHITELIST) ?: emptyList())
    builder.material(pack.onAnyString(DeserializationNames.PackMetadata.ITEM) { Material.matchMaterial(this) } ?: return null)

    return builder.build()
  }
}