package com.nononsensecode.oauth2.resource.server.filter

import com.auth0.jwk.Jwk
import com.auth0.jwk.UrlJwkProvider
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.nononsensecode.oauth2.resource.server.dto.HeaderDTO
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.util.*
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.net.URL
import java.security.Signature
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val logger = KotlinLogging.logger {  }

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class AccessTokenFilter(
    @Value("\${oauth2.jwks.url}")
    val jwksUrl: String,

    val zoneId: ZoneId,

    @Value("\${application.neededScope}")
    val neededScope: String
): Filter {

    val mapper = ObjectMapper()

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val httpResponse = response as HttpServletResponse
        val httpRequest = request as HttpServletRequest

        if (isHeroesPathAndGetMethod(httpRequest)) {
            try {
                val token = checkAuthorization(httpRequest)
                val jwt = checkWhetherJWTWellFormed(token)
                val header = getHeader(jwt)
                val payloadAsString = getPayload(jwt)
                val payload: JsonNode = mapper.readTree(payloadAsString)

                checkSignature(jwt, header)
                checkExpiry(payload)
                checkScope(payload)
            } catch (e: RuntimeException) {
                logger.error { e }
                handleErrorResponse(httpResponse, e)
            }
        }

        chain?.doFilter(request, response)
    }

    private fun handleErrorResponse(httpResponse: HttpServletResponse, e: RuntimeException) {
        when (e) {
            is AuthorizationFailureException ->
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.message)
            else ->
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error")
        }
    }

    private fun isHeroesPathAndGetMethod(httpRequest: HttpServletRequest): Boolean {
        return httpRequest.requestURI == "/api/v1.0/heroes" && httpRequest.method.equals("GET", true)
    }

    private fun checkAuthorization(httpRequest: HttpServletRequest): String {
        val authorizationHeader = httpRequest.getHeader("authorization")
            ?: throw AuthorizationFailureException("There is no authorization header specified")

        val authorization = authorizationHeader.split(" ")
        if (authorization[0] != "Bearer") {
            throw AuthorizationFailureException("Bearer token not available")
        }

        return authorization[1]
    }

    private fun checkExpiry(jsonNode: JsonNode) {
        val expNode = jsonNode.findPath("exp")
        val expiry = convertTimestampToLocalDateTime(expNode.toString().toLong())
        val now = LocalDateTime.now(zoneId)
        if (now.isAfter(expiry)) {
            throw AuthorizationFailureException("Token expired")
        }
    }

    private fun checkScope(jsonNode: JsonNode) {
        val scope = jsonNode.get("scope").textValue().split(" ")
        if (!scope.contains(neededScope)) {
            throw AuthorizationFailureException("Scope not available")
        }
    }

    private fun convertTimestampToLocalDateTime(timestamp: Long): LocalDateTime {
        val zoneOffSet = zoneId.rules.getOffset(Instant.now())
        return LocalDateTime.ofEpochSecond(timestamp, 0, zoneOffSet)
    }

    private fun checkSignature(jwt: List<String>, header: HeaderDTO) {
        val jwk = getJwk(header)
        val headerAndPayload = jwt[0] + "." + jwt[1]
        val signature = jwt[2]

        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(jwk.publicKey)
        sig.update(headerAndPayload.toByteArray())
        if (!sig.verify(Base64.getUrlDecoder().decode(signature))) {
            throw AuthorizationFailureException("Invalid signature")
        }
    }

    fun getJwk(header: HeaderDTO): Jwk {
        val jwkProvider = UrlJwkProvider(URL(jwksUrl))
        return jwkProvider.get(header.keyId)
    }

    private fun getHeader(jwt: List<String>): HeaderDTO  {
        val part = getPart(jwt[0], "Header")
        return mapper.readValue(part, HeaderDTO::class.java)
    }

    private fun getPayload(jwt: List<String>) = getPart(jwt[1], "Payload")

    private fun getPart(base64UrlPart: String, partName: String): String {
        val part = convertFromBase64(base64UrlPart)
        if (isJsonValid(part))
            return part
        throw AuthorizationFailureException("Invalid $partName")
    }

    private fun isJsonValid(json: String): Boolean {
        return try {
            mapper.readTree(json)
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun checkWhetherJWTWellFormed(token: String): List<String> {
        val jwt = token.split(".")

        if (jwt.size != 3) {
            throw AuthorizationFailureException("Access token is not valid")
        }

        return jwt
    }

    private fun validateBase64UrlString(base64Url: String): String {
        val regex = Regex("[a-zA-Z0-9-_.]+")
        if (!regex.matches(base64Url))
            throw IllegalArgumentException("Access token is not valid")
        return base64Url
    }

    private fun convertFromBase64(base64Url: String): String {
        val validated = validateBase64UrlString(base64Url)
        return String(Base64.getUrlDecoder().decode(validated.toByteArray()))
    }
}