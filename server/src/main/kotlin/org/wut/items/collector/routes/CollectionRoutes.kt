package org.wut.items.collector.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.wut.items.collector.model.CreateCollectionRequest
import org.wut.items.collector.model.CreateItemImageRequest
import org.wut.items.collector.model.CreateItemRequest
import org.wut.items.collector.model.ErrorResponse
import org.wut.items.collector.model.UpdateCollectionRequest
import org.wut.items.collector.model.UpdateItemRequest
import org.wut.items.collector.service.CollectionService
import org.wut.items.collector.service.ItemImageService
import org.wut.items.collector.service.ItemService

private fun io.ktor.server.application.ApplicationCall.userId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("uid")?.asString()

fun Route.collectionRoutes(collections: CollectionService, items: ItemService, itemImages: ItemImageService) {
    authenticate("auth-jwt") {
        route("/api/collections") {
            get {
                val uid = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(collections.list(uid))
            }
            post {
                val uid = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val req = call.receive<CreateCollectionRequest>()
                if (req.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Nazwa kolekcji jest wymagana"))
                    return@post
                }
                call.respond(collections.create(uid, req))
            }
            get("/{id}") {
                val uid = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val dto = collections.get(uid, id)
                if (dto == null) call.respond(HttpStatusCode.NotFound)
                else call.respond(dto)
            }
            put("/{id}") {
                val uid = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<UpdateCollectionRequest>()
                val dto = collections.update(uid, id, req)
                if (dto == null) call.respond(HttpStatusCode.NotFound)
                else call.respond(dto)
            }
            delete("/{id}") {
                val uid = call.userId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val ok = collections.delete(uid, id)
                if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
            }

            
            route("/{collectionId}/items") {
                get {
                    val uid = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val cid = call.parameters["collectionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val list = items.list(uid, cid)
                    if (list == null) call.respond(HttpStatusCode.NotFound)
                    else call.respond(list)
                }
                post {
                    val uid = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val cid = call.parameters["collectionId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val req = call.receive<CreateItemRequest>()
                    if (req.name.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Nazwa przedmiotu jest wymagana"))
                        return@post
                    }
                    val dto = items.create(uid, cid, req)
                    if (dto == null) call.respond(HttpStatusCode.NotFound)
                    else call.respond(dto)
                }
                get("/{id}") {
                    val uid = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val cid = call.parameters["collectionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val dto = items.get(uid, cid, id)
                    if (dto == null) call.respond(HttpStatusCode.NotFound)
                    else call.respond(dto)
                }
                put("/{id}") {
                    val uid = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                    val cid = call.parameters["collectionId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val req = call.receive<UpdateItemRequest>()
                    val dto = items.update(uid, cid, id, req)
                    if (dto == null) call.respond(HttpStatusCode.NotFound)
                    else call.respond(dto)
                }
                delete("/{id}") {
                    val uid = call.userId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                    val cid = call.parameters["collectionId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val ok = items.delete(uid, cid, id)
                    if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
                }

                
                route("/{itemId}/images") {
                    get {
                        val uid = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                        val cid = call.parameters["collectionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val iid = call.parameters["itemId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val list = itemImages.list(uid, cid, iid)
                        if (list == null) call.respond(HttpStatusCode.NotFound)
                        else call.respond(list)
                    }
                    post {
                        val uid = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                        val cid = call.parameters["collectionId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val iid = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val req = call.receive<CreateItemImageRequest>()
                        if (req.imageUrl.isBlank()) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Adres obrazu jest wymagany"))
                            return@post
                        }
                        val dto = itemImages.create(uid, cid, iid, req)
                        if (dto == null) call.respond(HttpStatusCode.NotFound)
                        else call.respond(dto)
                    }
                    delete("/{imageId}") {
                        val uid = call.userId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                        val cid = call.parameters["collectionId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val iid = call.parameters["itemId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val img = call.parameters["imageId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val ok = itemImages.delete(uid, cid, iid, img)
                        if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
                    }
                    put("/{imageId}/primary") {
                        val uid = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                        val cid = call.parameters["collectionId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                        val iid = call.parameters["itemId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                        val img = call.parameters["imageId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                        val dto = itemImages.setPrimary(uid, cid, iid, img)
                        if (dto == null) call.respond(HttpStatusCode.NotFound)
                        else call.respond(dto)
                    }
                }
            }
        }
    }
}
