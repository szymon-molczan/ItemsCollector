package org.wut.items.collector.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.wut.items.collector.model.ChangePasswordRequest
import org.wut.items.collector.model.ErrorResponse
import org.wut.items.collector.model.LoginRequest
import org.wut.items.collector.model.RegisterRequest
import org.wut.items.collector.service.UserService

fun Route.authRoutes(users: UserService) {
    route("/api/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            if (req.email.isBlank() || req.password.length < 4) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Adres e-mail i hasło (co najmniej 4 znaki) są wymagane"))
                return@post
            }
            val resp = users.register(req.email, req.password, req.displayName)
            if (resp == null) call.respond(HttpStatusCode.Conflict, ErrorResponse("Ten adres e-mail jest już zajęty"))
            else call.respond(resp)
        }
        post("/login") {
            val req = call.receive<LoginRequest>()
            val resp = users.login(req.email, req.password)
            if (resp == null) call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Nieprawidłowy adres e-mail lub hasło"))
            else call.respond(resp)
        }
        authenticate("auth-jwt") {
            post("/change-password") {
                val uid = call.principal<JWTPrincipal>()?.payload?.getClaim("uid")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val req = call.receive<ChangePasswordRequest>()
                if (req.newPassword.length < 4) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Nowe hasło musi mieć co najmniej 4 znaki"))
                    return@post
                }
                val ok = users.changePassword(uid, req.currentPassword, req.newPassword)
                if (!ok) call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Aktualne hasło jest nieprawidłowe"))
                else call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }
        }
    }
}
