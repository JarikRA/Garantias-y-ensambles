package com.intesc.controldegarantiasyensambles.main.dao.implements

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.CardModelDao
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.CardModel
import timber.log.Timber
import java.sql.Connection
import java.sql.SQLException

class CardModelDaoImpl : CardModelDao {

    override suspend fun getAllCardModels(): List<CardModel> {
        val sql = "SELECT * FROM modelos_tarjetas"
        val connection = DataBaseConnection.getConnection() ?: return emptyList()
        val cardModels = mutableListOf<CardModel>()

        try {
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)

            while (resultSet.next()) {
                val id = resultSet.getInt("id")
                val modelName = resultSet.getString("nombre_modelo") ?: ""
                val abbreviatedModel = resultSet.getString("modelo_abreviado") ?: ""

                cardModels.add(CardModel(id, modelName, abbreviatedModel))
            }
        } catch (e: SQLException) {
            Timber.e(e, "Error retrieving all card models.")
        } finally {
            DataBaseConnection.closeConnection()
        }

        return cardModels
    }

    override suspend fun getCarModelById(id: Int): CardModel? {
        val connection = DataBaseConnection.getConnection() ?: return null
        var cardModel: CardModel? = null

        try {
            val statement =
                connection.prepareStatement("SELECT * FROM modelos_tarjetas WHERE id = ?")
            statement.setInt(1, id)

            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                cardModel = CardModel(
                    id = resultSet.getInt("id"),
                    modelName = resultSet.getString("nombre_modelo") ?: "",
                    abbreviatedModel = resultSet.getString("modelo_abreviado") ?: ""
                )
            }
        } catch (e: SQLException) {
            Timber.e(e, "Error retrieving card model with ID: $id")
        } finally {
            DataBaseConnection.closeConnection()
        }

        return cardModel
    }

    override suspend fun insertCardModel(cardModel: CardModel): Boolean {
        val sql = """
        INSERT INTO modelos_tarjetas (nombre_modelo, modelo_abreviado)
        VALUES (?, ?)
    """.trimIndent()

        var connection: Connection? = null

        return try {
            connection =
                DataBaseConnection.getConnection() ?: throw SQLException("Connection is null.")

            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, cardModel.modelName)
                statement.setString(2, cardModel.abbreviatedModel)

                val rowsInserted = statement.executeUpdate()
                if (rowsInserted > 0) {
                    Timber.i("Card model inserted successfully: $cardModel")
                    true
                } else {
                    Timber.w("No rows were inserted for card model: $cardModel")
                    false
                }
            }
        } catch (e: SQLException) {

            Timber.e(e, "Error inserting card model: $cardModel")
            false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Timber.e(e, "Error closing database connection.")
            }
        }
    }


    override suspend fun updateCardModel(cardModel: CardModel): Boolean {
        if (cardModel.modelName.isBlank() || cardModel.abbreviatedModel.isBlank()) {
            Timber.e("Model name or abbreviated model cannot be empty.")
            return false
        }

        val sql = "UPDATE modelos_tarjetas SET nombre_modelo = ?, modelo_abreviado = ? WHERE id = ?"
        return try {
            DataBaseConnection.getConnection()?.use { connection ->
                connection.prepareStatement(sql).use { statement ->
                    statement.setString(1, cardModel.modelName)
                    statement.setString(2, cardModel.abbreviatedModel)
                    statement.setInt(3, cardModel.id!!)
                    statement.executeUpdate() > 0
                }
            }.also { success ->
                if (success == true) {
                    Timber.i("Card model updated successfully: $cardModel")
                } else {
                    Timber.w("Failed to update card model: $cardModel")
                }
            } ?: false
        } catch (e: SQLException) {
            Timber.e(e, "Error updating card model: $cardModel")
            false
        }
    }

    override suspend fun deleteCardModel(id: Int): Boolean {
        val sql = "DELETE FROM modelos_tarjetas WHERE id = ?"
        return try {
            DataBaseConnection.getConnection()?.use { connection ->
                connection.prepareStatement(sql).use { statement ->
                    statement.setInt(1, id)
                    statement.executeUpdate() > 0
                }
            }.also { success ->
                if (success == true) {
                    Timber.i("Card model deleted successfully with ID: $id")
                } else {
                    Timber.w("Failed to delete card model with ID: $id")
                }
            } ?: false
        } catch (e: SQLException) {
            Timber.e(e, "Error deleting card model with ID: $id")
            false
        }
    }
}
