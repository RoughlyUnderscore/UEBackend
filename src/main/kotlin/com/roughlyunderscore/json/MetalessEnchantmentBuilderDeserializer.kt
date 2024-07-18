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
import com.roughlyunderscore.data.server.*
import com.roughlyunderscore.enums.EnchantmentPlayer
import com.roughlyunderscore.ulib.data.Time
import com.roughlyunderscore.ulib.data.TimeMeasurementUnit
import com.roughlyunderscore.ulib.data.safeValueOr
import com.roughlyunderscore.ulib.json.*
import com.roughlyunderscore.ulib.text.normalize
import org.bukkit.Material
import java.lang.reflect.Type

object MetalessEnchantmentBuilderDeserializer : JsonDeserializer<BackendMetalessEnchantment.Builder?> {
  override fun deserialize(src: JsonElement?, type: Type?, ctx: JsonDeserializationContext?): BackendMetalessEnchantment.Builder? {
    val json = src?.asJsonObject ?: return null

    val name = json.anyString(DeserializationNames.Enchantment.NAME) ?: return null
    val material = json.onAnyString(DeserializationNames.Enchantment.ITEM) { Material.matchMaterial(this) } ?: return null
    val author = json.anyStringStrict(DeserializationNames.Enchantment.AUTHOR) { "Unknown" }
    val trigger = json.anyString(DeserializationNames.Enchantment.TRIGGER_NAME) ?: return null

    val chance = json.anyDoubleStrict(DeserializationNames.Enchantment.CHANCE) { 100.0 }
    val cooldown = Time(json.anyLongStrict(DeserializationNames.Enchantment.COOLDOWN) { 0 }, TimeMeasurementUnit.TICKS)
    val activationIndicator = json.anyStringStrict(DeserializationNames.Enchantment.ACTIVATION) { "bossbar" }

    val description = json.anyArrayOfStringsStrict(DeserializationNames.Enchantment.DESCRIPTION) { emptyList() }
    val applicables = json.anyArrayOfStrings(DeserializationNames.Enchantment.APPLICABLES) ?: return null
    val forbidden = json.anyArrayOfStringsStrict(DeserializationNames.Enchantment.FORBIDDEN) { emptyList() }

    val seekers = json.anyArrayOfStringsStrict(DeserializationNames.Enchantment.SEEKERS) { emptyList() }
    val targetPlayer = json.onAnyString(DeserializationNames.Enchantment.TARGET_PLAYER, { EnchantmentPlayer.FIRST } ) {
      safeValueOr(this.normalize().uppercase(), EnchantmentPlayer.FIRST)
    }!!

    val conflicts = json.onAnyArrayOfStrings(DeserializationNames.Enchantment.CONFLICTS) { mapNotNull { it } } ?: emptyList()

    val unique = json.anyBoolean(DeserializationNames.Enchantment.UNIQUE) ?: false

    val key = NamedKey.Builder().plugin("UnderscoreEnchants").key(name.lowercase().replace(" ", "_")).build()

    val conditions = json.onAnyArray(DeserializationNames.Enchantment.CONDITIONS) { mapNotNull { loadCondition(it) } } ?: emptyList()
    val levels = json.onAnyArray(DeserializationNames.Enchantment.LEVELS) { mapNotNull { loadLevel(it) } } ?: emptyList()
    val obtainment = json.onAnyArray(DeserializationNames.Enchantment.OBTAINMENT) { mapNotNull { loadObtainmentRestriction(it) } } ?: emptyList()
    val requiredEnchantments = json.onAnyArray(DeserializationNames.Enchantment.REQUIRED_ENCHANTMENTS) { mapNotNull { loadRequiredEnchantment(it) } } ?: emptyList()
    val requiredPlugins = json.onAnyArray(DeserializationNames.Enchantment.REQUIRED_PLUGINS) { mapNotNull { loadRequiredPlugin(it) } } ?: emptyList()

    val worldBlacklist = json.anyArrayOfStrings(DeserializationNames.Enchantment.WORLD_BLACKLIST) ?: emptyList()
    val worldWhitelist = json.anyArrayOfStrings(DeserializationNames.Enchantment.WORLD_WHITELIST) ?: emptyList()

    return BackendMetalessEnchantment.Builder()
      .name(name)
      .material(material)
      .author(author)
      .description(description)
      .key(key)
      .activationChance(chance)
      .cooldown(cooldown)
      .trigger(trigger)
      .indicator(activationIndicator)
      .applicables(applicables)
      .forbiddenMaterials(forbidden)
      .conditions(conditions)
      .conflicts(conflicts)
      .levels(levels)
      .restrictions(obtainment)
      .seekers(seekers)
      .targetPlayer(targetPlayer)
      .unique(unique)
      .requiredEnchantments(requiredEnchantments)
      .worldBlacklist(worldBlacklist)
      .worldWhitelist(worldWhitelist)
      .requiredPlugins(requiredPlugins)
  }

  private fun loadCondition(element: JsonElement): BackendEnchantmentCondition? {
    val json = element.asJsonObject ?: return null

    val fullConditionString = json.anyString(DeserializationNames.Condition.CONDITION) ?: return null
    val negate = json.anyBoolean(DeserializationNames.Condition.NEGATE) ?: false
    val targetPlayer = json.anyStringStrict(DeserializationNames.Condition.TARGET) { "first" }

    val conditionSplit = fullConditionString.split(" ").toMutableList()
    val condition = conditionSplit[0]
    conditionSplit.removeFirst()

    return BackendEnchantmentCondition.Builder()
      .name(condition)
      .negate(negate)
      .target(targetPlayer)
      .arguments(conditionSplit)
      .build()
  }

  private fun loadLevel(element: JsonElement): BackendEnchantmentLevel? {
    val json = element.asJsonObject ?: return null

    val levelIndex = json.anyInt(DeserializationNames.Level.INDEX) ?: return null
    val levelChance = json.anyDoubleStrict(DeserializationNames.Level.CHANCE) { 100.0 }
    val levelCooldown = json.anyLongStrict(DeserializationNames.Level.COOLDOWN) { 0 }

    val actions = json.onAnyArray(DeserializationNames.Level.ACTIONS) { mapNotNull { loadActions(it) } }?.flatten() ?: return null
    val conditions = json.onAnyArray(DeserializationNames.Level.CONDITIONS) { mapNotNull { loadCondition(it) } } ?: emptyList()

    return BackendEnchantmentLevel.Builder()
      .index(levelIndex)
      .conditions(conditions)
      .chance(levelChance)
      .cooldown(Time(levelCooldown, TimeMeasurementUnit.MILLISECONDS))
      .actions(actions)
      .build()
  }

  private fun loadActions(element: JsonElement): List<BackendEnchantmentAction>? {
    val json = element.asJsonObject ?: return null

    val singleAction = json.anyString(DeserializationNames.Action.SINGLE_ACTION)
    val multipleActions = json.anyArray(DeserializationNames.Action.MULTIPLE_ACTIONS)

    // In an action object, there can only be either the "action" field or the "actions" field.
    if (singleAction == null && multipleActions == null) return null
    if (singleAction != null && multipleActions != null) return null

    val actionDelayTicks = json.anyIntStrict(DeserializationNames.Action.DELAY) { 0 }
    val actionChance = json.anyDoubleStrict(DeserializationNames.Action.CHANCE) { 100.0 }
    val targetPlayer = json.anyStringStrict(DeserializationNames.Action.TARGET) { "first" }

    val conditions = json.onAnyArray(DeserializationNames.Action.CONDITIONS) { mapNotNull { loadCondition(it) } } ?: emptyList()

    if (singleAction != null) {
      val fullAction = json.anyString(DeserializationNames.Action.SINGLE_ACTION) ?: return null
      val actionSplit = fullAction.split(" ").toMutableList()
      val action = actionSplit[0]
      actionSplit.removeFirst()

      return listOf(BackendEnchantmentAction.Builder()
        .name(action)
        .chance(actionChance)
        .delay(Time(actionDelayTicks, TimeMeasurementUnit.TICKS))
        .target(targetPlayer)
        .arguments(actionSplit)
        .conditions(conditions)
        .build()
      )
    } else if (multipleActions != null) {
      val actionList = mutableListOf<BackendEnchantmentAction>()

      multipleActions.forEach {
        val fullAction = it.asString ?: return null
        val actionSplit = fullAction.split(" ").toMutableList()
        val action = actionSplit[0]
        actionSplit.removeFirst()

        actionList.add(BackendEnchantmentAction.Builder()
          .name(action)
          .chance(actionChance)
          .delay(Time(actionDelayTicks, TimeMeasurementUnit.TICKS))
          .target(targetPlayer)
          .arguments(actionSplit)
          .conditions(conditions)
          .build())
      }

      return actionList
    }

    return null
  }

  private fun loadObtainmentRestriction(element: JsonElement): BackendObtainmentRestriction? {
    val json = element.asJsonObject ?: return null

    val type = json.anyString(DeserializationNames.Obtainment.TYPE) ?: return null
    val levels = json.onAnyString(DeserializationNames.Obtainment.LEVELS) {
      val list = mutableListOf<Int>()
      val parts = replace(" ", "").split(",")
      for (part in parts) {
        if (part.contains("-")) {
          val range = part.split("-")
          for (i in range[0].toInt()..range[1].toInt()) list.add(i)
        } else {
          list.add(part.toInt())
        }
      }

      list
    } ?: return null

    return BackendObtainmentRestriction.Builder()
      .means(type)
      .levels(levels)
      .build()
  }

  private fun loadRequiredEnchantment(element: JsonElement): BackendRequiredEnchantment? {
    val json = element.asJsonObject ?: return null

    val keyString = json.anyString(DeserializationNames.RequiredEnchantment.KEY) ?: return null
    val seekers = json.anyArrayOfStrings(DeserializationNames.RequiredEnchantment.SEEKERS) ?: emptyList()

    val enchantmentLevels = json.anyString(DeserializationNames.RequiredEnchantment.LEVELS)?.let {
      val levels = mutableListOf<Int>()

      val parts = it.split(",")
      for (part in parts) {
        if (!part.contains("-")) {
          levels.add(part.toIntOrNull() ?: continue)
          continue
        }

        val deepPart = part.split("-")
        val start = deepPart.getOrNull(0)?.toIntOrNull() ?: continue
        val end = deepPart.getOrNull(1)?.toIntOrNull() ?: continue

        for (i in start..end) levels.add(i)
      }

      levels
    } ?: emptyList()

    return BackendRequiredEnchantment.Builder()
      .name(keyString)
      .seekers(seekers)
      .levels(enchantmentLevels)
      .build()
  }

  private fun loadRequiredPlugin(element: JsonElement): BackendRequiredPlugin? {
    val json = element.asJsonObject ?: return null

    val pluginName = json.anyString(DeserializationNames.RequiredPlugin.PLUGIN_NAME) ?: return null
    val displayName = json.anyStringStrict(DeserializationNames.RequiredPlugin.DISPLAY_NAME) { pluginName }
    val link = json.anyString(DeserializationNames.RequiredPlugin.LINK) ?: return null

    return BackendRequiredPlugin.Builder()
      .pluginName(pluginName)
      .displayName(displayName)
      .link(link)
      .build()
  }
}