package com.roughlyunderscore

import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.roughlyunderscore.data.UELocale
import com.roughlyunderscore.data.server.*
import com.roughlyunderscore.json.LocaleDeserializer
import com.roughlyunderscore.json.MetalessEnchantmentBuilderDeserializer
import com.roughlyunderscore.json.PackMetadataDeserializer
import com.roughlyunderscore.plugins.*
import com.roughlyunderscore.ulib.json.GsonAnnotationExcludeStrategy
import com.roughlyunderscore.utils.asPack
import de.mkammerer.argon2.Argon2Factory
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.bson.Document
import org.bson.types.Binary

// Global scope variables instead of using a singleton/DI
internal val cachedEnchantments = mutableMapOf<Long, BackendEnchantment>()
internal val cachedPacks = mutableMapOf<Long, BackendEnchantmentPack>()
internal val cachedLocales = mutableMapOf<Long, BackendLocale>()

internal var latestEnchantmentId = 0L
internal var latestPackId = 0L
internal var latestLocaleId = 0L

internal val gson = GsonBuilder()
  .setStrictness(Strictness.LENIENT)
  .setPrettyPrinting()
  .registerTypeAdapter(BackendMetalessEnchantment.Builder::class.java, MetalessEnchantmentBuilderDeserializer)
  .registerTypeAdapter(BackendPackMetadata::class.java, PackMetadataDeserializer)
  .registerTypeAdapter(UELocale::class.java, LocaleDeserializer)
  .setExclusionStrategies(GsonAnnotationExcludeStrategy)
  .create()

internal lateinit var mongoClient: MongoClient

internal lateinit var database: MongoDatabase

internal lateinit var enchantmentCollection: MongoCollection<Document>
internal lateinit var packCollection: MongoCollection<Document>
internal lateinit var enchantmentCodesCollection: MongoCollection<Document>
internal lateinit var packCodesCollection: MongoCollection<Document>
internal lateinit var localeCollection: MongoCollection<Document>
internal lateinit var localeCodesCollection: MongoCollection<Document>

internal val contentProvider = { data: ByteArray, id: Long ->
  Document().apply {
    set("content", Binary(data))
    set("uploadedAt", System.currentTimeMillis())
    set("downloadedTimes", 0)
    set("id", id)
  }
}

internal val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 24, 48)

fun main(args: Array<String>) {
  val dbName = System.getenv("DB")

  val url = System.getenv("DB_URL")
  val username = System.getenv("DB_LOGIN")
  val password = System.getenv("DB_PWD")

  val connectionString = ConnectionString("mongodb://$username:$password@$url")

  val settings = MongoClientSettings.builder()
    .applyConnectionString(connectionString)
    .serverApi(ServerApi.builder().version(ServerApiVersion.V1).build())
    .build()

  mongoClient = MongoClients.create(settings)

  database = mongoClient.getDatabase(dbName)

  enchantmentCollection = database.getCollection("enchantments")
  packCollection = database.getCollection("packs")
  enchantmentCodesCollection = database.getCollection("enchantmentCodesCollection")
  packCodesCollection = database.getCollection("packCodesCollection")
  localeCollection = database.getCollection("locales")
  localeCodesCollection = database.getCollection("localeCodesCollection")

  // Will abstract this next time that I clean up the code after 2.2
  for (enchantmentData in enchantmentCollection.find()) {
    val id = enchantmentData.getLong("id") ?: continue
    val downloadedTimes = enchantmentData.getInteger("downloadedTimes")
    val content = enchantmentData.get("content", Binary::class.java).data

    val enchantment = String(content).let { gson.fromJson(it, BackendMetalessEnchantment.Builder::class.java) }
      ?.id(id)
      ?.build()
      ?.toBackendEnchantment(ServerMeta.ENCH_PROVIDER(id).apply {
        this.downloadedTimes = downloadedTimes
      })

    if (enchantment != null) cachedEnchantments[enchantment.identifier] = enchantment
  }

  for (packData in packCollection.find()) {
    val id = packData.getLong("id") ?: continue
    val content = packData.get("content", Binary::class.java).data

    val pack = content.asPack(id = id)
    if (pack != null) cachedPacks[pack.identifier] = pack
  }

  for (localeData in localeCollection.find()) {
    val id = localeData.getLong("id") ?: continue
    val downloadedCount = localeData.getInteger("downloadedTimes")
    val content = localeData.get("content", Binary::class.java).data

    val locale = String(content).let { gson.fromJson(it, UELocale::class.java) }
      ?.let { BackendLocale.Builder().locale(it) }
      ?.meta(ServerMeta.LOCALE_PROVIDER(id).apply {
        this.downloadedTimes = downloadedCount
      })?.build()

    if (locale != null) cachedLocales[id] = locale
  }

  latestEnchantmentId = cachedEnchantments.keys.maxOrNull() ?: 0
  latestPackId = cachedPacks.keys.maxOrNull() ?: 0
  latestLocaleId = cachedLocales.keys.maxOrNull() ?: 0

  EngineMain.main(args)
}

fun Application.module() {
  configureSerialization()
  configureRouting()
}
