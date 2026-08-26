package org.wut.items.collector.service

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.wut.items.collector.auth.JwtConfig
import org.wut.items.collector.auth.PasswordHasher
import org.wut.items.collector.db.Users
import org.wut.items.collector.model.AuthResponse
import java.util.UUID

class UserService {

    
    fun register(email: String, password: String, displayName: String): AuthResponse? = transaction {
        val emailLower = email.lowercase().trim()
        val exists = Users.selectAll().where { Users.email eq emailLower }.any()
        if (exists) return@transaction null

        val id = UUID.randomUUID().toString()
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash(password, salt)
        val name = displayName.ifBlank { emailLower.substringBefore("@") }

        Users.insert {
            it[Users.id] = id
            it[Users.email] = emailLower
            it[Users.passwordHash] = hash
            it[Users.passwordSalt] = salt
            it[Users.displayName] = name
            it[Users.createdAt] = System.currentTimeMillis()
        }
        AuthResponse(token = JwtConfig.generate(id, emailLower), userId = id, email = emailLower, displayName = name)
    }

    
    fun login(email: String, password: String): AuthResponse? = transaction {
        val emailLower = email.lowercase().trim()
        val row = Users.selectAll().where { Users.email eq emailLower }.firstOrNull()
            ?: return@transaction null
        val ok = PasswordHasher.verify(password, row[Users.passwordSalt], row[Users.passwordHash])
        if (!ok) return@transaction null
        AuthResponse(
            token = JwtConfig.generate(row[Users.id], emailLower),
            userId = row[Users.id],
            email = emailLower,
            displayName = row[Users.displayName]
        )
    }

    



    fun changePassword(userId: String, currentPassword: String, newPassword: String): Boolean = transaction {
        val row = Users.selectAll().where { Users.id eq userId }.firstOrNull()
            ?: return@transaction false
        val ok = PasswordHasher.verify(currentPassword, row[Users.passwordSalt], row[Users.passwordHash])
        if (!ok) return@transaction false

        val newSalt = PasswordHasher.newSalt()
        val newHash = PasswordHasher.hash(newPassword, newSalt)
        Users.update({ Users.id eq userId }) {
            it[Users.passwordHash] = newHash
            it[Users.passwordSalt] = newSalt
        }
        true
    }
}
