package br.com.zup.edu.shared.grpc

import br.com.zup.edu.shared.grpc.handlers.DefaultExceptionHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExceptionHandlerResolver(
    @Inject private val handlers: List<ExceptionHandler<Exception>>,
    val defaultHandler: ExceptionHandler<Exception> = DefaultExceptionHandler(),
) {

    fun resolve(e: Exception): ExceptionHandler<Exception> {
        val foundHandlers = handlers.filter { h -> h.supports(e) }
        if (foundHandlers.size > 1)
            throw IllegalStateException("Too many handlers supporting the same exception '$e': $foundHandlers")

        return foundHandlers.firstOrNull() ?: defaultHandler
    }

}