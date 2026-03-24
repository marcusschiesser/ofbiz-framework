package com.example.leadflow

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class LeadflowBackendApplication

fun main(args: Array<String>) {
    runApplication<LeadflowBackendApplication>(*args)
}
