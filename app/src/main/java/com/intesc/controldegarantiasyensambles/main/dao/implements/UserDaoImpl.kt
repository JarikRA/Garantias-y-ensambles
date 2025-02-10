package com.intesc.controldegarantiasyensambles.main.dao.implements

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.UserDao
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.User
import java.sql.SQLException
import timber.log.Timber
import java.sql.Connection
import java.sql.Statement

class UserDaoImpl : UserDao {

    override suspend fun getAllUsers(): List<User> {
        val sql = "SELECT * FROM clientes"
        val connection = DataBaseConnection.getConnection() ?: return emptyList()
        val userList = mutableListOf<User>()

        try {
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)

            while (resultSet.next()) {
                val user = User(
                    id = resultSet.getInt("id"),
                    name = resultSet.getString("nombre_cliente") ?: "",
                    phoneNumber = resultSet.getString("telefono") ?: "",
                    email = resultSet.getString("email") ?: "",
                    address = resultSet.getString("direccion") ?: "",
                    zipCode = resultSet.getString("codigo_postal") ?: ""
                )
                userList.add(user)
            }
        } catch (e: SQLException) {
            Timber.e(e, "Error retrieving all users.")
        } finally {
            DataBaseConnection.closeConnection()
        }

        return userList
    }

    override suspend fun getUserById(id: Int): User? {
        if (id <= 0) {
            Timber.e("Invalid ID: $id")
            return null
        }

        val sql = """
    SELECT id, nombre_cliente, telefono, email, direccion, codigo_postal
    FROM clientes
    WHERE id = ?
    """.trimIndent()

        return try {
            DataBaseConnection.getConnection()?.use { connection ->
                connection.prepareStatement(sql).use { statement ->
                    statement.setInt(1, id)
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            Timber.i("User found with ID: $id")
                            User(
                                id = resultSet.getInt("id"),
                                name = resultSet.getString("nombre_cliente") ?: "",
                                phoneNumber = resultSet.getString("telefono") ?: "",
                                email = resultSet.getString("email") ?: "",
                                address = resultSet.getString("direccion") ?: "",
                                zipCode = resultSet.getString("codigo_postal") ?: ""
                            )
                        } else {
                            Timber.i("No user found with ID: $id")
                            null
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            Timber.e(e, "Error fetching user with ID: $id")
            null
        }
    }

    override suspend fun insertUser(user: User): Int? {
        val sql = """
        INSERT INTO clientes (nombre_cliente, telefono, email, direccion, codigo_postal)
        VALUES (?, ?, ?, ?, ?)
    """.trimIndent()

        var connection: Connection? = null

        return try {
            connection =
                DataBaseConnection.getConnection() ?: throw SQLException("Connection is null.")

            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                statement.setString(1, user.name)
                statement.setString(2, user.phoneNumber)
                statement.setString(3, user.email)
                statement.setString(4, user.address)
                statement.setString(5, user.zipCode)

                val rowsInserted = statement.executeUpdate()

                if (rowsInserted > 0) {
                    val generatedKeys = statement.generatedKeys
                    if (generatedKeys.next()) {
                        val generatedId = generatedKeys.getInt(1)
                        Timber.i("User inserted successfully with ID: $generatedId")
                        return generatedId
                    }
                }

                Timber.w("No rows were inserted for user: $user")
                null
            }
        } catch (e: SQLException) {
            Timber.e(e, "Error inserting user: $user")
            null
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Timber.e(e, "Error closing database connection.")
            }
        }
    }

    override suspend fun updateUser(user: User): Boolean {
        val sql = """
        UPDATE clientes SET 
        nombre_cliente = ?, 
        telefono = ?, 
        email = ?, 
        direccion = ?, 
        codigo_postal = ? 
        WHERE id = ?
    """.trimIndent()

        var connection: Connection? = null

        return try {
            connection =
                DataBaseConnection.getConnection() ?: throw SQLException("Connection is null.")

            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, user.name)
                statement.setString(2, user.phoneNumber)
                statement.setString(3, user.email)
                statement.setString(4, user.address)
                statement.setString(5, user.zipCode)
                statement.setInt(6, user.id)

                val rowsUpdated = statement.executeUpdate()
                if (rowsUpdated > 0) {
                    Timber.i("User updated successfully: $user")
                    true
                } else {
                    Timber.w("No rows were updated for user: $user")
                    false
                }
            }
        } catch (e: SQLException) {
            Timber.e(e, "Error updating user: $user")
            false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Timber.e(e, "Error closing database connection.")
            }
        }
    }

    override suspend fun getAdminPassword(): String? {
        val sql = "SELECT contrasenia FROM admin"
        var password: String? = null

        try {
            DataBaseConnection.getConnection()?.use { connection ->
                connection.prepareStatement(sql).use { statement ->
                    Timber.d("Executing query: $sql")
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            password = resultSet.getString("contrasenia")
                            Timber.i("Admin password retrieved successfully")
                        } else {
                            Timber.w("No admin password found")
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            Timber.e(e, "SQL error while fetching admin password")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while fetching admin password")
        } finally {
            DataBaseConnection.closeConnection()
        }

        return password
    }
}
