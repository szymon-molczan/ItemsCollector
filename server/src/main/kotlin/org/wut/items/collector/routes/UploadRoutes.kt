package org.wut.items.collector.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.wut.items.collector.model.ErrorResponse
import org.wut.items.collector.model.UploadResponse
import org.wut.items.collector.service.FileStorage

fun Route.uploadRoutes(storage: FileStorage) {
    authenticate("auth-jwt") {
        route("/api/upload") {
            post {
                val multipart: MultiPartData = call.receiveMultipart()
                var savedUrl: String? = null
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val bytes = part.provider().readRemaining().readByteArray()
                            savedUrl = storage.save(bytes, part.originalFileName)
                        }
                        else -> {}
                    }
                    part.dispose()
                }
                if (savedUrl == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Nie przesłano pliku"))
                } else {
                    call.respond(UploadResponse(url = savedUrl!!))
                }
            }
        }
    }
}
