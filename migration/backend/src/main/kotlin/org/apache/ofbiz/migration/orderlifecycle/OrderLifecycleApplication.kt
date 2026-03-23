package org.apache.ofbiz.migration.orderlifecycle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.flywaydb.core.Flyway
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.sql.DataSource

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
@OpenAPIDefinition(
    info = Info(
        title = "Order Lifecycle API",
        version = "v1",
        description = "Cloud-native order lifecycle service extracted from the OFBiz order management bounded context.",
    ),
)
class OrderLifecycleApplication {

    @Bean
    fun objectMapper(): ObjectMapper =
        jacksonObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Bean(initMethod = "migrate")
    fun flyway(dataSource: DataSource): Flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()

    @Bean
    fun corsConfigurer(appProperties: AppProperties): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(*appProperties.cors.allowedOrigins.toTypedArray())
                    .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
            }
        }
    }

    @Bean
    fun seedRunner(
        context: ConfigurableApplicationContext,
        appProperties: AppProperties,
        demoDataSeeder: DemoDataSeeder,
    ): ApplicationRunner {
        return ApplicationRunner { _: ApplicationArguments ->
            if (!appProperties.seed.enabled) {
                return@ApplicationRunner
            }

            demoDataSeeder.seedDemoData()

            if (appProperties.seed.only) {
                SpringApplication.exit(context, { 0 })
            }
        }
    }
}

@ConfigurationProperties("app")
data class AppProperties(
    val cors: CorsProperties = CorsProperties(),
    val seed: SeedProperties = SeedProperties(),
)

data class CorsProperties(
    val allowedOrigins: List<String> = listOf("http://localhost:3000"),
)

data class SeedProperties(
    val enabled: Boolean = false,
    val only: Boolean = false,
)

fun main(args: Array<String>) {
    runApplication<OrderLifecycleApplication>(*args)
}
