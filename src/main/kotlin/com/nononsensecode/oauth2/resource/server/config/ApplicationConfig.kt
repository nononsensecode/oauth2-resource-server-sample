package com.nononsensecode.oauth2.resource.server.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.WebRequest
import java.time.ZoneId
import javax.servlet.RequestDispatcher

@Configuration
class ApplicationConfig(
    @Value("\${application.timezone}")
    val timezone: String
) {

    @Bean
    fun zoneId(): ZoneId {
        return ZoneId.of(timezone)
    }

    @Bean
    fun errorAttributes(): ErrorAttributes {
        return object : DefaultErrorAttributes() {
            override fun getErrorAttributes(
                webRequest: WebRequest?,
                options: ErrorAttributeOptions?
            ): MutableMap<String, Any> {
                val errorAttributes = super.getErrorAttributes(webRequest, options)
                val errorMessage = webRequest?.getAttribute(RequestDispatcher.ERROR_MESSAGE, RequestAttributes.SCOPE_REQUEST)
                if (errorMessage != null) {
                    errorAttributes["message"] = errorMessage
                }

                return errorAttributes
            }
        }
    }
}