package com.intesc.controldegarantiasyensambles.main.database

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.sql.Connection
import java.sql.DriverManager

object DataBaseConnection {

    //SSH connection constants
    private const val SSH_USERNAME = "default_value"
    private const val SSH_PASSWORD = "default_value"
    private const val SSH_HOST = "default_value"
    private const val SSH_PORT = 0

    //MySQL connection constants
    private const val MYSQL_HOST = "default_value"
    private const val MYSQL_PORT = 0
    private const val DATABASE_NAME = "default_value"
    private const val MYSQL_USERNAME = "default_value"
    private const val MYSQL_PASSWORD = "default_value"

    private var session: Session? = null
    private var connection: Connection? = null

    fun getConnection(): Connection? {
        if (connection != null && !connection!!.isClosed) {
            return connection
        }

        return try {
            // Connexion SSH
            val jsch = JSch()
            session = jsch.getSession(SSH_USERNAME, SSH_HOST, SSH_PORT)
            session?.setPassword(SSH_PASSWORD)
            session?.setConfig("StrictHostKeyChecking", "no")
            session?.connect()

            if (session?.isConnected == true) {
                val assignedPort = session?.setPortForwardingL(0, MYSQL_HOST, MYSQL_PORT)

                val connectionUrl =
                    "jdbc:mysql://localhost:$assignedPort/$DATABASE_NAME?useTimeZone=true&serverTimeZone=UTC"
                connection =
                    DriverManager.getConnection(connectionUrl, MYSQL_USERNAME, MYSQL_PASSWORD)

                connection
            } else {
                println("SSH session not connected.")
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun closeConnection() {
        try {
            connection?.close()
            session?.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection = null
            session = null
        }
    }
}