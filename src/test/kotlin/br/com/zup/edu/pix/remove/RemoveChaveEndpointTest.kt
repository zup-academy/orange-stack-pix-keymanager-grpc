package br.com.zup.edu.pix.remove

import br.com.zup.edu.grpc.KeymanagerRemoveGrpcServiceGrpc
import br.com.zup.edu.grpc.RemoveChavePixRequest
import br.com.zup.edu.pix.ChavePixNaoEncontradaException
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.util.*
import javax.inject.Inject

@MicronautTest
internal class RemoveChaveEndpointTest {

    @Inject
    lateinit var grpcClient: KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceBlockingStub

    @Inject
    lateinit var service: RemoveChaveService

    @Test
    fun `deve remover chave Pix`() {
        // cenário
        val clienteId = UUID.randomUUID().toString()
        val pixId = UUID.randomUUID().toString()

        doNothing().`when`(service).remove(clienteId = clienteId, pixId = pixId)

        // ação
        val response = grpcClient.remove(RemoveChavePixRequest.newBuilder()
                                                    .setPixId(pixId)
                                                    .setClienteId(clienteId)
                                                    .build())

        // validação
        assertEquals(pixId, response.pixId)
        assertEquals(clienteId, response.clienteId)
    }

    /**
     * XXX: vale a pena ter dado que não existe branches no código?
     */
    @Test
    fun `nao deve remover chave Pix quando chave nao encontrada`() {
        // cenário
        val clienteId = UUID.randomUUID().toString()
        val pixId = UUID.randomUUID().toString()

        `when`(service.remove(clienteId = clienteId, pixId = pixId))
            .thenThrow(ChavePixNaoEncontradaException("Chave Pix não encontrada"))

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            val response = grpcClient.remove(RemoveChavePixRequest.newBuilder()
                                                            .setPixId(pixId)
                                                            .setClienteId(clienteId)
                                                            .build())
        }

        // validação
        with (thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @MockBean(RemoveChaveService::class)
    fun mockedService(): RemoveChaveService {
        return mock(RemoveChaveService::class.java)
    }

    @Factory
    class Clients  {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceBlockingStub? {
            return KeymanagerRemoveGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}