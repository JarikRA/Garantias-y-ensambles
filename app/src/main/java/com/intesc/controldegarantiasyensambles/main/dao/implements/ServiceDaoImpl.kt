package com.intesc.controldegarantiasyensambles.main.dao.implements

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.ServiceDao
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.Service
import timber.log.Timber
import java.sql.Connection
import java.sql.Date
import java.sql.SQLException

class ServiceDaoImpl : ServiceDao {

    override suspend fun getServicesByCardId(cardId: Int): List<Service> {
        val sql = "SELECT * FROM servicios WHERE id_tarjeta = ?"
        var connection: Connection? = null
        val services = mutableListOf<Service>()

        return try {
            connection = DataBaseConnection.getConnection()
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setInt(1, cardId)
                val resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    val service = Service(
                        id = resultSet.getInt("id"),
                        idCard = resultSet.getInt("id_tarjeta"),
                        date = resultSet.getDate("fecha"),
                        faultDescription = resultSet.getString("descripcion_falla"),
                        cardFailure = resultSet.getString("falla_tarjeta"),
                        reasonForFailure = resultSet.getString("motivo_falla"),
                        repair = resultSet.getString("reparacion")
                    )
                    services.add(service)
                }
            }
            services
        } catch (e: SQLException) {
            Timber.e(e, "Error retrieving services for cardId=$cardId")
            emptyList()
        } finally {
            try {
                connection?.close()
                Timber.i("Database connection closed after getServicesByCardId")
            } catch (closeException: SQLException) {
                Timber.e(closeException, "Failed to close database connection")
            }
        }
    }

    override suspend fun insertService(service: Service): Boolean {
        val sql = """
        INSERT INTO servicios (id_tarjeta, fecha, descripcion_falla, falla_tarjeta, motivo_falla, reparacion)
        VALUES (?, ?, ?, ?, ?, ?)
    """
        var connection: Connection? = null

        return try {
            connection = DataBaseConnection.getConnection()
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setInt(1, service.idCard)
                statement.setDate(2, service.date as Date?)
                statement.setString(3, service.faultDescription)
                statement.setString(4, service.cardFailure)
                statement.setString(5, service.reasonForFailure)
                statement.setString(6, service.repair)

                val rowsInserted = statement.executeUpdate()
                rowsInserted > 0
            } ?: false
        } catch (e: SQLException) {
            Timber.e(e, "Error inserting service: $service")
            false
        } finally {
            try {
                connection?.close()
                Timber.i("Database connection closed after insertService")
            } catch (closeException: SQLException) {
                Timber.e(closeException, "Failed to close database connection")
            }
        }
    }

    override suspend fun insertRepairInLastServiceOfCard(idCard: Int, repair: String): Boolean {
        val sqlSelect = """
        SELECT * FROM servicios 
        WHERE id_tarjeta = ? 
        ORDER BY fecha DESC, id DESC LIMIT 1
    """
        val sqlUpdate = """
        UPDATE servicios 
        SET reparacion = ? 
        WHERE id = ?
    """
        var connection: Connection? = null

        return try {
            connection = DataBaseConnection.getConnection()

            val resultSet = connection?.prepareStatement(sqlSelect)?.apply {
                setInt(1, idCard)
            }?.executeQuery()

            if (resultSet?.next() == true) {
                val serviceId = resultSet.getInt("id")

                val rowsUpdated = connection?.prepareStatement(sqlUpdate)?.apply {
                    setString(1, repair)
                    setInt(2, serviceId)
                }?.executeUpdate() ?: 0

                rowsUpdated > 0
            } else {
                false
            }
        } catch (e: SQLException) {
            Timber.e(e, "Error updating repair in last service for cardId=$idCard")
            false
        } finally {
            try {
                connection?.close()
                Timber.i("Database connection closed after insertRepairInLastServiceOfCard")
            } catch (closeException: SQLException) {
                Timber.e(closeException, "Failed to close database connection")
            }
        }
    }
}