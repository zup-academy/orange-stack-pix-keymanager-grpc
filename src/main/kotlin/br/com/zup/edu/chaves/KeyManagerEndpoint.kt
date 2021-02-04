package br.com.zup.edu.chaves

import br.com.zup.edu.grpc.KeymanagerGrpcServiceGrpc
import br.com.zup.edu.grpc.RegistraChavePixRequest
import br.com.zup.edu.grpc.RegistraChavePixResponse
import br.com.zup.edu.chaves.integration.itau.ContasDeClientesNoItauClient
import br.com.zup.pix.chaves.TipoDeChave
import br.com.zup.pix.chaves.TipoDeConta
import br.com.zup.pix.chaves.ChavePix
import io.grpc.stub.StreamObserver
import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyManagerEndpoint(
    @Inject val itauClient: ContasDeClientesNoItauClient,
    @Inject val repository: ChavePixRepository,
) : KeymanagerGrpcServiceGrpc.KeymanagerGrpcServiceImplBase() {

    override fun registra(
        request: RegistraChavePixRequest,
        responseObserver: StreamObserver<RegistraChavePixResponse>
    ) {

        /**
         * 1. buscar dados da conta no ERP-ITAU
         * 2. gravar no banco de dados
         * 3. retornar resultado
         */

        // 1. buscar dados da conta no ERP-ITAU

        val response = itauClient.buscaContaPorTipo(request.clienteId, request.tipoDeConta.name)
        val conta = response.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        // 2. gravar no banco de dados
        val chave = ChavePix(
            clienteId = UUID.fromString(request.clienteId),
            tipo = TipoDeChave.valueOf(request.tipoDeChave.name),
            chave = if (request.tipoDeChave == br.com.zup.edu.grpc.TipoDeChave.ALEATORIA) UUID.randomUUID().toString() else request.chave,
            tipoDeConta = TipoDeConta.valueOf(request.tipoDeConta.name),
            conta = conta
        )

        // 3. retornar resultado
        repository.save(chave)

        // 4. retornar resultado com pixId
        responseObserver.onNext(RegistraChavePixResponse.newBuilder()
                                            .setClienteId(chave.clienteId.toString())
                                            .setPixId(chave.id.toString())
                                            .build())
        responseObserver.onCompleted()
    }
}