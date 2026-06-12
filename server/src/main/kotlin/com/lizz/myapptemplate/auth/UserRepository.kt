package com.lizz.myapptemplate.auth

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class User(
    val id: String,
    val email: String,
    val passwordHash: String,
)

/**
 * The swap-point for real persistence: implement this against your database
 * and provide it to Application.module(). The in-memory default forgets
 * everything on restart — fine for the template demo.
 */
interface UserRepository {
    fun create(
        email: String,
        passwordHash: String,
    ): User?

    fun findByEmail(email: String): User?

    fun findById(id: String): User?
}

class InMemoryUserRepository : UserRepository {
    private val usersByEmail = ConcurrentHashMap<String, User>()

    override fun create(
        email: String,
        passwordHash: String,
    ): User? {
        val user = User(id = UUID.randomUUID().toString(), email = email, passwordHash = passwordHash)
        return if (usersByEmail.putIfAbsent(email, user) == null) user else null
    }

    override fun findByEmail(email: String): User? = usersByEmail[email]

    override fun findById(id: String): User? = usersByEmail.values.firstOrNull { it.id == id }
}
