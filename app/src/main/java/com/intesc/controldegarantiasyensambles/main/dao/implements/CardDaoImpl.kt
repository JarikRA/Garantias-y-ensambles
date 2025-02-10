package com.intesc.controldegarantiasyensambles.main.dao.implements

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.CardDao
import com.intesc.controldegarantiasyensambles.main.model.Card
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import java.sql.SQLException
import timber.log.Timber
import java.sql.Statement
import java.sql.Types

class CardDaoImpl : CardDao {

    override suspend fun getLastSpecificSerialNumber(
        abbreviateCardModelName: String,
        month: Int,
        year: Int
    ): String? {
        val sql = """
        SELECT num_serie 
        FROM tarjetas
        WHERE num_serie LIKE ? 
          AND MONTH(fecha_creacion) = ? 
          AND YEAR(fecha_creacion) = ? 
        ORDER BY num_serie DESC 
        LIMIT 1
    """

        val connection = DataBaseConnection.getConnection() ?: return null

        return try {
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, "$abbreviateCardModelName%")
                statement.setInt(2, month)
                statement.setInt(3, year)

                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getString("num_serie")
                    } else {
                        null
                    }
                }
            }
        } catch (e: SQLException) {
            Timber.e(
                e,
                "Error retrieving last serial number: model=$abbreviateCardModelName, month=$month, year=$year"
            )
            null
        } finally {
            DataBaseConnection.closeConnection()
        }
    }

    override suspend fun getCardsByAdvanceSearch(
        idModel: Int?,
        month: String,
        year: String,
        category: String
    ): List<Card> {
        val sqlBuilder = StringBuilder("SELECT * FROM tarjetas WHERE 1=1")

        if (idModel != null) {
            sqlBuilder.append(" AND id_modelo_tarjeta = $idModel")
        }

        if (month != "Todos") {
            val monthNumber = when (month) {
                "Enero" -> 1
                "Febrero" -> 2
                "Marzo" -> 3
                "Abril" -> 4
                "Mayo" -> 5
                "Junio" -> 6
                "Julio" -> 7
                "Agosto" -> 8
                "Septiembre" -> 9
                "Octubre" -> 10
                "Noviembre" -> 11
                "Diciembre" -> 12
                else -> null
            }

            if (monthNumber != null) {
                sqlBuilder.append(" AND MONTH(fecha_creacion) = $monthNumber")
            }
        }

        if (year != "Todos") {
            sqlBuilder.append(" AND YEAR(fecha_creacion) = $year")
        }

        if (category != "Todos" && category.isNotEmpty()) {
            sqlBuilder.append(" AND categoria = '$category'")
        }

        val cards = mutableListOf<Card>()

        Timber.i("Starting advanced search for cards with idModel: $idModel, month: $month, year: $year, category: $category")

        try {
            DataBaseConnection.getConnection()?.use { connection ->
                connection.prepareStatement(sqlBuilder.toString()).use { statement ->
                    Timber.i("Executing SQL query: $sqlBuilder")

                    statement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            val card = Card(
                                id = resultSet.getInt("id"),
                                idModel = resultSet.getObject("id_modelo_tarjeta") as Int?,
                                serialNumber = resultSet.getString("num_serie"),
                                creationDate = resultSet.getDate("fecha_creacion"),
                                updateDate = resultSet.getDate("fecha_actualizacion"),
                                status = resultSet.getString("estado"),
                                subStatus = resultSet.getObject("sub_estado") as String?,
                                category = resultSet.getString("categoria"),
                                userId = resultSet.getObject("id_cliente") as Int?
                            )
                            cards.add(card)
                        }
                    }

                    Timber.i("Retrieved ${cards.size} cards.")
                }
            }
        } catch (e: SQLException) {
            Timber.e("SQL error while performing advanced search for cards", e)
        } catch (e: Exception) {
            Timber.e("Unexpected error while performing advanced search for cards", e)
        } finally {
            DataBaseConnection.closeConnection()
        }

        return cards
    }

    override suspend fun insertCard(card: Card): Int {
        val sql = """
        INSERT INTO tarjetas 
        (id_modelo_tarjeta, num_serie, fecha_creacion, fecha_actualizacion, estado, sub_estado, categoria, id_cliente) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """
        val connection = DataBaseConnection.getConnection() ?: return -1

        return try {
            connection.autoCommit = false
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->

                if (card.idModel != null) {
                    statement.setInt(1, card.idModel)
                } else {
                    statement.setNull(1, Types.INTEGER)
                }

                statement.setString(2, card.serialNumber)
                statement.setDate(3, java.sql.Date(card.creationDate.time))
                statement.setDate(4, java.sql.Date(card.updateDate.time))
                statement.setString(5, card.status)

                if (card.subStatus != null) {
                    statement.setString(6, card.subStatus)
                } else {
                    statement.setNull(6, Types.VARCHAR)
                }

                statement.setString(7, card.category)

                if (card.userId != null) {
                    statement.setInt(8, card.userId)
                } else {
                    statement.setNull(8, Types.INTEGER)
                }

                statement.executeUpdate()
                connection.commit()

                val generatedKeys = statement.generatedKeys
                if (generatedKeys.next()) {
                    generatedKeys.getInt(1)
                } else {
                    -1
                }
            }
        } catch (e: SQLException) {
            connection.rollback()
            Timber.e(e, "Error inserting card.")
            -1
        } finally {
            connection.autoCommit = true
            DataBaseConnection.closeConnection()
        }
    }

    override suspend fun insertCards(cards: List<Card>): List<Int> {
        val sql = """
        INSERT INTO tarjetas 
        (id, id_modelo_tarjeta, num_serie, fecha_creacion, fecha_actualizacion, estado, sub_estado, categoria, id_cliente) 
        VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?)
    """
        val connection = DataBaseConnection.getConnection() ?: return emptyList()

        return try {
            connection.autoCommit = false
            val ids = mutableListOf<Int>()

            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                for (card in cards) {

                    if (card.idModel != null) {
                        statement.setInt(1, card.idModel)
                    } else {
                        statement.setNull(1, Types.INTEGER)
                    }

                    statement.setString(2, card.serialNumber)
                    statement.setDate(3, java.sql.Date(card.creationDate.time))
                    statement.setDate(4, java.sql.Date(card.updateDate.time))
                    statement.setString(5, card.status)

                    if (card.subStatus != null) {
                        statement.setString(6, card.subStatus)
                    } else {
                        statement.setNull(6, Types.VARCHAR)
                    }

                    statement.setString(7, card.category)

                    if (card.userId != null) {
                        statement.setInt(8, card.userId)
                    } else {
                        statement.setNull(8, Types.INTEGER)
                    }

                    statement.addBatch()
                }

                Timber.d("Running insert batch")
                statement.executeBatch()
                Timber.d("Batch executed, retrieving generated keys")

                val generatedKeys = statement.generatedKeys
                while (generatedKeys.next()) {
                    ids.add(generatedKeys.getInt(1))
                }
            }

            connection.commit()
            ids
        } catch (e: SQLException) {
            connection.rollback()
            Timber.e(e, "Error inserting batch of cards.")
            emptyList()
        } finally {
            try {
                connection.autoCommit = true
                DataBaseConnection.closeConnection()
            } catch (e: SQLException) {
                Timber.e(e, "Failed to reset autoCommit or close connection.")
            }
        }
    }

    override suspend fun updateCard(card: Card): Boolean {
        val sql = """
        UPDATE tarjetas 
        SET 
            id_modelo_tarjeta = ?, 
            num_serie = ?, 
            fecha_actualizacion = ?, 
            estado = ?, 
            sub_estado = ?, 
            categoria = ?, 
            id_cliente = ? 
        WHERE id = ?
    """
        val connection = DataBaseConnection.getConnection() ?: return false

        return try {
            connection.autoCommit = false
            connection.prepareStatement(sql).use { statement ->

                if (card.idModel != null) {
                    statement.setInt(1, card.idModel)
                } else {
                    statement.setNull(1, Types.INTEGER)
                }

                statement.setString(2, card.serialNumber)

                statement.setDate(3, java.sql.Date(card.updateDate.time))

                statement.setString(4, card.status)

                if (card.subStatus != null) {
                    statement.setString(5, card.subStatus)
                } else {
                    statement.setNull(5, Types.VARCHAR)
                }

                statement.setString(6, card.category)

                if (card.userId != null) {
                    statement.setInt(7, card.userId)
                } else {
                    statement.setNull(7, Types.INTEGER)
                }

                statement.setInt(8, card.id)

                val rowsUpdated = statement.executeUpdate()
                connection.commit()

                Timber.i("Card with id=${card.id} updated successfully. Rows affected: $rowsUpdated")
                rowsUpdated > 0
            }
        } catch (e: SQLException) {
            connection.rollback()
            Timber.e(e, "Error updating card with id=${card.id}")
            false
        } finally {
            connection.autoCommit = true
            DataBaseConnection.closeConnection()
        }
    }

    override suspend fun updateMultipleCards(cards: List<Card>): Boolean {
        val sql = """
        UPDATE tarjetas 
        SET 
            id_modelo_tarjeta = ?, 
            num_serie = ?, 
            fecha_creacion = ?, 
            fecha_actualizacion = ?, 
            estado = ?, 
            sub_estado = ?, 
            categoria = ?, 
            id_cliente = ? 
        WHERE id = ?
    """
        val connection = DataBaseConnection.getConnection() ?: return false

        return try {
            connection.autoCommit = false
            connection.prepareStatement(sql).use { statement ->
                for (card in cards) {

                    if (card.idModel != null) {
                        statement.setInt(1, card.idModel)
                    } else {
                        statement.setNull(1, Types.INTEGER)
                    }

                    statement.setString(2, card.serialNumber)

                    statement.setDate(3, java.sql.Date(card.creationDate.time))

                    statement.setDate(4, java.sql.Date(card.updateDate.time))

                    statement.setString(5, card.status)

                    if (card.subStatus != null) {
                        statement.setString(6, card.subStatus)
                    } else {
                        statement.setNull(6, Types.VARCHAR)
                    }

                    statement.setString(7, card.category)

                    if (card.userId != null) {
                        statement.setInt(8, card.userId)
                    } else {
                        statement.setNull(8, Types.INTEGER)
                    }

                    statement.setInt(9, card.id)

                    statement.addBatch()
                }

                Timber.i("Executing batch update for ${cards.size} cards.")
                statement.executeBatch()
            }
            connection.commit()
            Timber.i("Successfully updated ${cards.size} cards in batch.")
            true
        } catch (e: SQLException) {
            connection.rollback()
            Timber.e(e, "Error while updating multiple cards.")
            false
        } finally {
            try {
                connection.autoCommit = true
                DataBaseConnection.closeConnection()
            } catch (e: SQLException) {
                Timber.e(e, "Error while resetting autoCommit or closing the connection.")
            }
        }
    }
}

