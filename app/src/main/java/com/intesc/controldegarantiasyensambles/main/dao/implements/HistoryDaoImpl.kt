package com.intesc.controldegarantiasyensambles.main.dao.implements

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.HistoryDao
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.History
import timber.log.Timber
import java.sql.Connection
import java.sql.SQLException

class HistoryDaoImpl : HistoryDao {

    override suspend fun getHistoriesByCardId(cardId: Int): List<History> {
        val sql = "SELECT * FROM historiales WHERE id_tarjeta = ?"
        var connection: Connection? = null
        val histories = mutableListOf<History>()

        return try {
            connection = DataBaseConnection.getConnection()
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setInt(1, cardId)
                val resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    val history = History(
                        id = resultSet.getInt("id"),
                        idCard = resultSet.getInt("id_tarjeta"),
                        date = resultSet.getDate("fecha"),
                        status = resultSet.getString("estado"),
                        assemblerName = resultSet.getString("nombre_ensamblador"),
                        record = resultSet.getString("historial"),
                        category = resultSet.getString("categoria") ?: "Sin categoría"
                    )
                    histories.add(history)
                }
            }
            histories
        } catch (e: SQLException) {
            Timber.e(e, "Error retrieving histories for cardId=$cardId")
            emptyList()
        } finally {
            try {
                connection?.close()
                Timber.i("Database connection closed after getHistoriesByCardId")
            } catch (closeException: SQLException) {
                Timber.e(closeException, "Failed to close database connection")
            }
        }
    }


    override suspend fun insertHistory(history: History): Boolean {
        val sql = """
    INSERT INTO historiales 
    (id, id_tarjeta, fecha, estado, nombre_ensamblador, historial, categoria) 
    VALUES (?, ?, ?, ?, ?, ?, ?)
    """
        var connection: Connection? = null

        return try {
            connection = DataBaseConnection.getConnection()
            connection?.prepareStatement(sql)?.use { statement ->

                statement.setInt(1, history.id)
                statement.setInt(2, history.idCard)
                statement.setDate(3, java.sql.Date(history.date.time))
                statement.setString(4, history.status)
                statement.setString(5, history.assemblerName)
                statement.setString(6, history.record)
                statement.setString(7, history.category)

                val rowsInserted = statement.executeUpdate()
                Timber.i("Inserted $rowsInserted row(s) into historiales with id: ${history.id}")
                rowsInserted > 0
            } ?: false
        } catch (e: SQLException) {
            Timber.e(
                e,
                "Error inserting history with id=${history.id}, idCard=${history.idCard}, date=${history.date}, status=${history.status}, " +
                        "assemblerName=${history.assemblerName}, record=${history.record}, category=${history.category}."
            )
            Timber.e("SQL Error Code: ${e.errorCode}, SQL State: ${e.sqlState}")
            false
        } finally {
            try {
                connection?.close()
                Timber.i("Database connection closed after insertHistory")
            } catch (closeException: SQLException) {
                Timber.e(closeException, "Failed to close database connection")
            }
        }
    }

    override suspend fun insertHistories(histories: List<History>): Boolean {
        val sql = """
    INSERT INTO historiales 
    (id, id_tarjeta, fecha, estado, nombre_ensamblador, historial, categoria) 
    VALUES (?, ?, ?, ?, ?, ?, ?)
    """
        var connection: Connection? = null

        return try {
            connection = DataBaseConnection.getConnection()
            connection?.prepareStatement(sql)?.use { statement ->
                for (history in histories) {
                    statement.setInt(1, history.id)
                    statement.setInt(2, history.idCard)
                    statement.setDate(3, java.sql.Date(history.date.time))
                    statement.setString(4, history.status)
                    statement.setString(5, history.assemblerName)
                    statement.setString(6, history.record)
                    statement.setString(7, history.category)
                    statement.addBatch()
                }

                val results = statement.executeBatch()
                Timber.i("Inserted ${results.size} rows into historiales")
                results.all { it > 0 }
            } ?: false
        } catch (e: SQLException) {
            Timber.e(e, "Error inserting batch of histories")
            false
        } finally {
            connection?.close()
            Timber.i("Database connection closed after insertHistories")
        }
    }
}