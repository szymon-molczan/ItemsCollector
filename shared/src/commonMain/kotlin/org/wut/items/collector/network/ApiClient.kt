package org.wut.items.collector.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.wut.items.collector.model.AuthResponse
import org.wut.items.collector.model.CollectionDto
import org.wut.items.collector.model.CreateCollectionRequest
import org.wut.items.collector.model.CreateItemRequest
import org.wut.items.collector.model.CreateItemImageRequest
import org.wut.items.collector.model.ErrorResponse
import org.wut.items.collector.model.ItemDto
import org.wut.items.collector.model.ItemImageDto
import org.wut.items.collector.model.LoginRequest
import org.wut.items.collector.model.RegisterRequest
import org.wut.items.collector.model.UpdateCollectionRequest
import org.wut.items.collector.model.UpdateItemRequest
import org.wut.items.collector.model.UploadResponse




class ApiException(val status: Int, message: String) : Exception(message)




class ApiClient(
    httpClientFactory: (HttpClientConfig<*>.() -> Unit) -> HttpClient,
    private val baseUrlProvider: () -> String,
    private val tokenProvider: () -> String?
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client: HttpClient = httpClientFactory {
        install(ContentNegotiation) { json(json) }
        install(Logging) { level = LogLevel.INFO }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
        HttpResponseValidator {
            validateResponse { resp -> validate(resp) }
        }
    }

    private suspend fun validate(resp: HttpResponse) {
        if (resp.status.value < 400) return
        val msg = runCatching {
            json.decodeFromString(ErrorResponse.serializer(), resp.body<String>())
        }.getOrNull()?.error ?: "HTTP ${resp.status.value}"
        throw ApiException(resp.status.value, msg)
    }

    private fun base(): String = baseUrlProvider().trimEnd('/')

    




    fun baseUrl(): String = base()

    private suspend fun authHeader(): Pair<String, String>? =
        tokenProvider()?.let { HttpHeaders.Authorization to "Bearer $it" }

    
    suspend fun register(req: RegisterRequest): AuthResponse =
        client.post("${base()}/api/auth/register") { setBody(req) }.body()

    suspend fun login(req: LoginRequest): AuthResponse =
        client.post("${base()}/api/auth/login") { setBody(req) }.body()

    suspend fun changePassword(req: org.wut.items.collector.model.ChangePasswordRequest) {
        client.post("${base()}/api/auth/change-password") {
            authHeader()?.let { (k, v) -> header(k, v) }
            setBody(req)
        }
    }

    
    suspend fun listCollections(): List<CollectionDto> =
        client.get("${base()}/api/collections") {
            authHeader()?.let { (k, v) -> header(k, v) }
        }.body()

    suspend fun createCollection(req: CreateCollectionRequest): CollectionDto =
        client.post("${base()}/api/collections") {
            authHeader()?.let { (k, v) -> header(k, v) }
            setBody(req)
        }.body()

    suspend fun updateCollection(id: String, req: UpdateCollectionRequest): CollectionDto =
        client.put("${base()}/api/collections/$id") {
            authHeader()?.let { (k, v) -> header(k, v) }
            setBody(req)
        }.body()

    suspend fun deleteCollection(id: String) {
        client.delete("${base()}/api/collections/$id") {
            authHeader()?.let { (k, v) -> header(k, v) }
        }
    }

    
    suspend fun listItems(collectionId: String): List<ItemDto> =
        client.get("${base()}/api/collections/$collectionId/items") {
            authHeader()?.let { (k, v) -> header(k, v) }
        }.body()

    suspend fun createItem(collectionId: String, req: CreateItemRequest): ItemDto =
        client.post("${base()}/api/collections/$collectionId/items") {
            authHeader()?.let { (k, v) -> header(k, v) }
            setBody(req)
        }.body()

    suspend fun updateItem(collectionId: String, id: String, req: UpdateItemRequest): ItemDto =
        client.put("${base()}/api/collections/$collectionId/items/$id") {
            authHeader()?.let { (k, v) -> header(k, v) }
            setBody(req)
        }.body()

    suspend fun deleteItem(collectionId: String, id: String) {
        client.delete("${base()}/api/collections/$collectionId/items/$id") {
            authHeader()?.let { (k, v) -> header(k, v) }
        }
    }

    

    
    suspend fun listItemImages(collectionId: String, itemId: String): List<ItemImageDto> =
        client.get("${base()}/api/collections/$collectionId/items/$itemId/images") {
            authHeader()?.let { (k, v) -> header(k, v) }
        }.body()

    




    suspend fun createItemImage(
        collectionId: String,
        itemId: String,
        req: CreateItemImageRequest
    ): ItemImageDto =
        client.post("${base()}/api/collections/$collectionId/items/$itemId/images") {
            authHeader()?.let { (k, v) -> header(k, v) }
            setBody(req)
        }.body()

    
    suspend fun deleteItemImage(collectionId: String, itemId: String, imageId: String) {
        client.delete("${base()}/api/collections/$collectionId/items/$itemId/images/$imageId") {
            authHeader()?.let { (k, v) -> header(k, v) }
        }
    }

    
    suspend fun setPrimaryItemImage(
        collectionId: String,
        itemId: String,
        imageId: String
    ): ItemImageDto =
        client.put("${base()}/api/collections/$collectionId/items/$itemId/images/$imageId/primary") {
            authHeader()?.let { (k, v) -> header(k, v) }
        }.body()

    




    suspend fun downloadBytes(absoluteUrl: String): ByteArray =
        client.get(absoluteUrl) {
            authHeader()?.let { (k, v) -> header(k, v) }
        }.body()

    
    suspend fun uploadImage(bytes: ByteArray, fileName: String): UploadResponse =
        client.post("${base()}/api/upload") {
            authHeader()?.let { (k, v) -> header(k, v) }
            setBody(MultiPartFormDataContent(formData {
                append(
                    "file",
                    bytes,
                    headers {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    }
                )
            }))
        }.body()
}
