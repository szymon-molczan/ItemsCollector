package org.wut.items.collector

import com.auth0.jwt.JWT
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import org.wut.items.collector.auth.JwtConfig
import org.wut.items.collector.db.Db
import org.wut.items.collector.model.ErrorResponse
import org.wut.items.collector.routes.authRoutes
import org.wut.items.collector.routes.collectionRoutes
import org.wut.items.collector.routes.uploadRoutes
import org.wut.items.collector.service.CollectionService
import org.wut.items.collector.service.FileStorage
import org.wut.items.collector.service.ItemImageService
import org.wut.items.collector.service.ItemService
import org.wut.items.collector.service.UserService

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    
    Db.init()

    
    val users = UserService()
    val collections = CollectionService()
    val items = ItemService()
    val itemImages = ItemImageService()
    val storage = FileStorage()

    
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    install(CallLogging) { level = Level.INFO }
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get); allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put); allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Wewnętrzny błąd serwera"))
        }
    }
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "items-collector"
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.getClaim("uid").asString().isNullOrBlank()) null
                else JWTPrincipal(credential.payload)
            }
        }
    }

    
    routing {
        get("/") { call.respondText("Items Collector API - up") }
        get("/health") { call.respond(mapOf("status" to "ok")) }

        
        staticFiles("/uploads", storage.rootDir())

        authRoutes(users)
        collectionRoutes(collections, items, itemImages)
        uploadRoutes(storage)
    }
}
