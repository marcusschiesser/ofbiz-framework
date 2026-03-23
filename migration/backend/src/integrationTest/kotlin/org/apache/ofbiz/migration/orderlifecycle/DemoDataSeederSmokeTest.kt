package org.apache.ofbiz.migration.orderlifecycle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
class DemoDataSeederSmokeTest {

    @Autowired
    lateinit var demoDataSeeder: DemoDataSeeder

    @Autowired
    lateinit var flyway: Flyway

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun setUp() {
        flyway.migrate()
        jdbcClient.sql("truncate table order_events, order_refunds, order_items, orders, products, customers cascade").update()
    }

    @Test
    fun `seed command creates the full reference and order dataset`() {
        demoDataSeeder.seedDemoData()

        val customers = jdbcClient.sql("select count(*) from customers").query(Long::class.java).single()
        val products = jdbcClient.sql("select count(*) from products").query(Long::class.java).single()
        val orders = jdbcClient.sql("select count(*) from orders").query(Long::class.java).single()

        assertEquals(3, customers)
        assertEquals(3, products)
        assertEquals(6, orders)
    }

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:18.1")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
