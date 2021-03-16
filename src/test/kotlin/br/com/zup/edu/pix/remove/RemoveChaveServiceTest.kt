package br.com.zup.edu.pix.remove

import br.com.zup.edu.integration.bcb.BancoCentralClient
import br.com.zup.edu.integration.bcb.DeletePixKeyRequest
import br.com.zup.edu.integration.bcb.DeletePixKeyResponse
import br.com.zup.edu.pix.*
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.validation.ConstraintViolationException

@MicronautTest
internal class RemoveChaveServiceTest(
    val service: RemoveChaveService,
    val repository: ChavePixRepository,
) {

    @Inject
    lateinit var bcbClient: BancoCentralClient

    lateinit var CHAVE_EXISTENTE: ChavePix

    @BeforeEach
    fun setup() {
        CHAVE_EXISTENTE = repository.save(chave(
            tipo = TipoDeChave.EMAIL,
            chave = "rponte@gmail.com",
            clienteId = UUID.randomUUID()
        ))
    }

    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }


    @Test
    fun `deve remover chave pix existente`() {
        // cenário
        `when`(bcbClient.delete("rponte@gmail.com", DeletePixKeyRequest("rponte@gmail.com")))
            .thenReturn(HttpResponse.ok(DeletePixKeyResponse(key = "rponte@gmail.com",
                participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
                deletedAt = LocalDateTime.now()))
            )

        // ação
        service.remove(clienteId = CHAVE_EXISTENTE.clienteId.toString(), pixId = CHAVE_EXISTENTE.id.toString())

        // validação
        with(repository.findById(CHAVE_EXISTENTE.id!!)) {
            assertTrue(isEmpty)
        }
    }

    @Test
    fun `nao deve remover chave pix existente quando ocorrer algum erro no serviço do BCB`() {
        // cenário
        `when`(bcbClient.delete("rponte@gmail.com", DeletePixKeyRequest("rponte@gmail.com")))
            .thenReturn(HttpResponse.unprocessableEntity())

        // ação
        val thrown = assertThrows<IllegalStateException> {
            service.remove(clienteId = CHAVE_EXISTENTE.clienteId.toString(), pixId = CHAVE_EXISTENTE.id.toString())
        }

        // validação
        with(thrown) {
            assertEquals("Erro ao remover chave Pix no Banco Central do Brasil (BCB)", message)
        }
    }

    @Test
    fun `nao deve remover chave pix quando chave inexistente`() {
        // cenário
        val pixIdNaoExistente = UUID.randomUUID().toString()

        // ação
        val thrown = assertThrows<ChavePixNaoEncontradaException> {
            service.remove(clienteId = CHAVE_EXISTENTE.clienteId.toString(), pixId = pixIdNaoExistente)
        }

        with(thrown) {
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente", message)
        }
    }

    /**
     * XXX: será que precisamos disso dado que não existe branch explicito na query?
     */
    @Test
    fun `nao deve remover chave pix quando chave existente mas pertence a outro cliente`() {
        // cenário
        val outroClienteId = UUID.randomUUID().toString()

        // ação
        val thrown = assertThrows<ChavePixNaoEncontradaException> {
            service.remove(clienteId = outroClienteId, pixId = CHAVE_EXISTENTE.id.toString())
        }

        with(thrown) {
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente", message)
        }
    }

    /**
     * XXX: será que precisamos disso dado que não existe branch da Bean Validation?
     */
    @Test
    fun `nao deve remover chave pix quando pixId e clienteId forem invalidos`() {
        // cenário
        val pixIdVazio = ""
        val clienteIdVazio = ""

        // ação
        val thrown = assertThrows<ConstraintViolationException> {
            service.remove(clienteId = clienteIdVazio, pixId = pixIdVazio)
        }

        with(thrown) {
            val violations = this.constraintViolations
                                 .map { Pair(it.propertyPath.last().toString(), it.message) }
                                 .toList()

            assertThat(violations, hasSize(4))
            assertThat(
                violations,
                containsInAnyOrder(
                    Pair("clienteId", "must not be blank"),
                    Pair("clienteId", "cliente ID com formato inválido"),
                    Pair("pixId", "must not be blank"),
                    Pair("pixId", "pix ID com formato inválido")),
            )
        }
    }

    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return mock(BancoCentralClient::class.java)
    }

    private fun chave(
        tipo: TipoDeChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID(),
    ): ChavePix {
        return ChavePix(
            clienteId = clienteId,
            tipo = tipo,
            chave = chave,
            tipoDeConta = TipoDeConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "UNIBANCO ITAU",
                nomeDoTitular = "Rafael Ponte",
                cpfDoTitular = "12345678900",
                agencia = "1218",
                numeroDaConta = "123456"
            )
        )
    }

}