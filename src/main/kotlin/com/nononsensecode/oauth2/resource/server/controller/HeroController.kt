package com.nononsensecode.oauth2.resource.server.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1.0/heroes")
class HeroController {

    private val heroes = listOf(
        Hero(1, "Superman"),
        Hero(2, "Batman"),
        Hero(3, "Aquaman")
    )

    @GetMapping
    fun getHeroes(): ResponseEntity<List<Hero>> = ResponseEntity(heroes, HttpStatus.OK)
}


data class Hero(
    val id: Int,
    val name: String
)