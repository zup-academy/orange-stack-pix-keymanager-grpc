package br.com.zup.edu.shared.grpc

import br.com.zup.edu.shared.grpc.handlers.DefaultExceptionHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExceptionHandlerResolver(
    @Inject private val handlers: List<ExceptionHandler<Exception>>,
) {

    private var defaultHandler: ExceptionHandler<Exception> = DefaultExceptionHandler()

    /**
     * We can replace the default exception handler through this constructor
     * https://docs.micronaut.io/latest/guide/index.html#replaces
     */
    constructor(handlers: List<ExceptionHandler<Exception>>, defaultHandler: ExceptionHandler<Exception>) : this(handlers) {
        this.defaultHandler = defaultHandler
    }

    fun resolve(e: Exception): ExceptionHandler<Exception> {
        val foundHandlers = handlers.filter { h -> h.supports(e) }
        if (foundHandlers.size > 1)
            throw IllegalStateException("Too many handlers supporting the same exception '${e.javaClass.name}': $foundHandlers")

        return foundHandlers.firstOrNull() ?: defaultHandler
    }

}