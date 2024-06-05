package com.roughlyunderscore.plugins

import com.roughlyunderscore.cachedEnchantments
import com.roughlyunderscore.cachedLocales
import com.roughlyunderscore.cachedPacks
import com.roughlyunderscore.plugins.routes.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
  install(StatusPages) {
    // statusFile(HttpStatusCode.TooManyRequests, HttpStatusCode.PayloadTooLarge, HttpStatusCode.InternalServerError, filePattern = "#.html")
    exception<Throwable> { call, cause ->
      call.respondText(status = HttpStatusCode.InternalServerError, text = "Internal Server Error: ${cause.localizedMessage}")
      cause.printStackTrace()
    }
  }

  install(RateLimit) {
    register {
      rateLimiter(limit = 30, refillPeriod = 75.seconds)
    }
  }

  routing {
    rateLimit {
      route("api/v1") {
        uploadPackRoute()
        updatePackRoute()
        downloadPackRoute()

        uploadEnchantmentRoute()
        updateEnchantmentRoute()
        downloadEnchantmentRoute()

        uploadLocaleRoute()
        updateLocaleRoute()
        downloadLocaleRoute()

        get("/packs") { call.respond(cachedPacks.values) }
        get("/enchs") { call.respond(cachedEnchantments.values) }
        get("/locales") { call.respond(cachedLocales.values) }
      }
    }
  }
}