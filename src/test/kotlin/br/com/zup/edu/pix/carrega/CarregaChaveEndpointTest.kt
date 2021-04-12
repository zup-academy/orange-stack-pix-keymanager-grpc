package br.com.zup.edu.pix.carrega

import br.com.zup.edu.grpc.CarregaChavePixRequest
import br.com.zup.edu.grpc.CarregaChavePixRequest.FiltroPorPixId
import br.com.zup.edu.grpc.KeymanagerCarregaGrpcServiceGrpc
import br.com.zup.edu.integration.bcb.*
import br.com.zup.edu.pix.*
import br.com.zup.edu.pix.TipoDeChave.*
import br.com.zup.edu.util.violations
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

/**
 * TIP: Microunaut ainda não suporta @Nested classes do jUnit5
 *  - https://github.com/micronaut-projects/micronaut-test/issues/56
 */
@MicronautTest(transactional = false)
internal class CarregaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeymanagerCarregaGrpcServiceGrpc.KeymanagerCarregaGrpcServiceBlockingStub,
) {

    @Inject
    lateinit var bcbClient: BancoCentralClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    /**
     * TIP: por padrão roda numa transação isolada
     */
    @BeforeEach
    fun setup() {
        repository.save(chave(tipo = EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = CPF, chave = "63657520325", clienteId = UUID.randomUUID()))
        repository.save(chave(tipo = ALEATORIA, chave = "randomkey-3", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = CELULAR, chave = "+551155554321", clienteId = CLIENTE_ID))
    }

    /**
     * TIP: por padrão roda numa transação isolada
     */
    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve carregar chave por pixId e clienteId`() {
        // cenário
        val chaveExistente = repository.findByChave("+551155554321").get()

        // ação
        val response = grpcClient.carrega(CarregaChavePixRequest.newBuilder()
                                                                .setPixId(FiltroPorPixId.newBuilder()
                                                                    .setPixId(chaveExistente.id.toString())
                                                                    .setClienteId(chaveExistente.clienteId.toString())
                                                                    .build()
                                                                ).build())

        // validação
        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipo.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando filtro invalido`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder()
                                            .setPixId(FiltroPorPixId.newBuilder()
                                                .setPixId("")
                                                .setClienteId("")
                                                .build()
                                            ).build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            assertThat(violations(), containsInAnyOrder(
                Pair("pixId", "must not be blank"),
                Pair("clienteId", "must not be blank"),
                Pair("pixId", "não é um formato válido de UUID"),
                Pair("clienteId", "não é um formato válido de UUID"),
            ))
        }
    }

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando registro nao existir`() {
        // ação
        val pixIdNaoExistente = UUID.randomUUID().toString()
        val clienteIdNaoExistente = UUID.randomUUID().toString()
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder()
                                            .setPixId(FiltroPorPixId.newBuilder()
                                                .setPixId(pixIdNaoExistente)
                                                .setClienteId(clienteIdNaoExistente)
                                                .build()
                                            ).build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro existir localmente`() {
        // cenário
        val chaveExistente = repository.findByChave("rafael.ponte@zup.com.br").get()

        // ação
        val response = grpcClient.carrega(CarregaChavePixRequest.newBuilder()
                                            .setChave("rafael.ponte@zup.com.br")
                                            .build())

        // validação
        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipo.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro nao existir localmente mas existir no BCB`() {
        // cenário
        val bcbResponse = pixKeyDetailsResponse()
        `when`(bcbClient.findByKey(key = "user.from.another.bank@santander.com.br"))
            .thenReturn(HttpResponse.ok(pixKeyDetailsResponse()))

        // ação
        val response = grpcClient.carrega(CarregaChavePixRequest.newBuilder()
                                    .setChave("user.from.another.bank@santander.com.br")
                                    .build())

        // validação
        with(response) {
            assertEquals("", this.pixId)
            assertEquals("", this.clienteId)
            assertEquals(bcbResponse.keyType.name, this.chave.tipo.name)
            assertEquals(bcbResponse.key, this.chave.chave)
        }
    }

    @Test
    fun `nao deve carregar chave por valor da chave quando registro nao existir localmente nem no BCB`() {
        // cenário
        `when`(bcbClient.findByKey(key = "not.existing.user@santander.com.br"))
            .thenReturn(HttpResponse.notFound())

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder()
                                    .setChave("not.existing.user@santander.com.br")
                                    .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `nao deve carregar chave por valor da chave quando filtro invalido`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder().setChave("").build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            assertThat(violations(), containsInAnyOrder(
                Pair("chave", "must not be blank"),
            ))
        }
    }

    @Test
    fun `nao deve carregar chave quando filtro invalido`() {

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder().build())
        }

        // validação
        with(thrown) {
            Assertions.assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            Assertions.assertEquals("Chave Pix inválida ou não informada", status.description)
        }
    }


    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return mock(BancoCentralClient::class.java)
    }

    @Factory
    class Clients  {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeymanagerCarregaGrpcServiceGrpc.KeymanagerCarregaGrpcServiceBlockingStub? {
            return KeymanagerCarregaGrpcServiceGrpc.newBlockingStub(channel)
        }
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

    private fun pixKeyDetailsResponse(): PixKeyDetailsResponse {
        return PixKeyDetailsResponse(
            keyType = PixKeyType.EMAIL,
            key = "user.from.another.bank@santander.com.br",
            bankAccount = bankAccount(),
            owner = owner(),
            createdAt = LocalDateTime.now()
        )
    }

    private fun bankAccount(): BankAccount {
        return BankAccount(
            participant = "90400888",
            branch = "9871",
            accountNumber = "987654",
            accountType = BankAccount.AccountType.SVGS
        )
    }

    private fun owner(): Owner {
        return Owner(
            type = Owner.OwnerType.NATURAL_PERSON,
            name = "Another User",
            taxIdNumber = "12345678901"
        )
    }

}