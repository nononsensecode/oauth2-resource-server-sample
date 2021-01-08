package com.nononsensecode.oauth2.resource.server.filter

import java.lang.RuntimeException

class AuthorizationFailureException(
    override val message: String
): RuntimeException(message)