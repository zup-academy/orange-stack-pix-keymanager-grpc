package br.com.zup.edu.shared.grpc

import br.com.zup.edu.shared.grpc.handlers.DefaultExceptionHandler
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExceptionHandlerResolverTest {

    lateinit var illegalArgumentExceptionHandler: ExceptionHandler<IllegalArgumentException>

    lateinit var resolver: ExceptionHandlerResolver

    @BeforeEach
    fun setup() {
        illegalArgumentExceptionHandler = object : ExceptionHandler<IllegalArgumentException> {

            override fun handle(e: IllegalArgumentException): ExceptionHandler.StatusWithDetails {
                TODO("Not yet implemented")
            }

            override fun supports(e: Exception) = e is java.lang.IllegalArgumentException
        }

        resolver = ExceptionHandlerResolver(handlers = listOf(illegalArgumentExceptionHandler))
    }

    @Test
    fun `deve retornar o ExceptionHandler especifico para o tipo de excecao`() {
        val resolved = resolver.resolve(IllegalArgumentException())

        assertSame(illegalArgumentExceptionHandler, resolved)
    }

    @Test
    fun `deve retornar o ExceptionHandler padrao quando nenhum handler suportar o tipo da excecao`() {
        val resolved = resolver.resolve(RuntimeException())

        assertTrue(resolved is DefaultExceptionHandler)
    }

    @Test
    fun `deve lancar um erro caso encontre mais de um ExceptionHandler que suporte a mesma excecao`() {
        resolver = ExceptionHandlerResolver(listOf(illegalArgumentExceptionHandler, illegalArgumentExceptionHandler))

        assertThrows<IllegalStateException> { resolver.resolve(IllegalArgumentException()) }
    }
}