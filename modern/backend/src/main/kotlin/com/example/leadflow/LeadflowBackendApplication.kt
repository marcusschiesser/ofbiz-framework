package com.example.leadflow

import com.example.leadflow.config.OfbizProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(OfbizProperties::class)
class LeadflowBackendApplication

fun main(args: Array<String>) {
    runApplication<LeadflowBackendApplication>(*args)
}
