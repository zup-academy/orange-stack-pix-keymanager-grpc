package br.com.zup.edu.chaves.endpoints

import br.com.zup.edu.chaves.NovaChavePixService
import br.com.zup.edu.chaves.extension.toModel
import br.com.zup.edu.grpc.KeymanagerRegistraGrpcServiceGrpc
import br.com.zup.edu.grpc.RegistraChavePixRequest
import br.com.zup.edu.grpc.RegistraChavePixResponse
import br.com.zup.edu.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RegistraChaveEndpoint(@Inject private val service: NovaChavePixService,) : KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceImplBase() {

    override fun registra(
        request: RegistraChavePixRequest, // 1
        responseObserver: StreamObserver<RegistraChavePixResponse> // 1
    ) {

        val novaChave = request.toModel() // 2
        val chaveCriada = service.registra(novaChave) // 1

        responseObserver.onNext(RegistraChavePixResponse.newBuilder()
            .setClienteId(chaveCriada.clienteId.toString())
            .setPixId(chaveCriada.id.toString())
            .build())

        responseObserver.onCompleted()
    }

}