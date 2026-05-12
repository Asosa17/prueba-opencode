package org.example.project.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.example.project.config.SupabaseConfig
import javax.sql.DataSource

object DatabaseFactory {
    private var dataSource: DataSource? = null

    fun createDataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = SupabaseConfig.dbUrl
            username = SupabaseConfig.dbUser
            password = SupabaseConfig.dbPassword
            maximumPoolSize = SupabaseConfig.dbPoolSize
            maxLifetime = SupabaseConfig.dbMaxLifetimeMs
            driverClassName = "org.postgresql.Driver"
            validate()
        }
        return HikariDataSource(config)
    }

    fun getDataSource(): DataSource {
        if (dataSource == null) {
            dataSource = createDataSource()
        }
        return dataSource!!
    }
}
