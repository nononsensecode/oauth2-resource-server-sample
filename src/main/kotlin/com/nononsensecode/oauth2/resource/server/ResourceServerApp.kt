package com.nononsensecode.oauth2.resource.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ResourceServerApp

fun main(args: Array<String>) {
    runApplication<ResourceServerApp>(*args)
}