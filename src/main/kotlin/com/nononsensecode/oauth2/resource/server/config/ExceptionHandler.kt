package com.nononsensecode.oauth2.resource.server.config

import com.nononsensecode.oauth2.resource.server.filter.AuthorizationFailureException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionHandler {

    @ExceptionHandler(AuthorizationFailureException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorizedException(e: AuthorizationFailureException): ApiError {
        return ApiError(HttpStatus.UNAUTHORIZED.value(), e.message)
    }

}

data class ApiError(
    val status: Int,
    val message: String
)