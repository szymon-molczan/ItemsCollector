package org.wut.items.collector

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue











class ApplicationTest {

    @Test
    fun health_endpoint_returns_ok() = testApplication {
        application { module() }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"status\""), "Response should contain status field, was: $body")
        assertTrue(body.contains("\"ok\""), "Status should be ok, was: $body")
    }

    @Test
    fun root_endpoint_returns_api_info() = testApplication {
        application { module() }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(
            response.bodyAsText().contains("Items Collector"),
            "Root should mention API name"
        )
    }
}
