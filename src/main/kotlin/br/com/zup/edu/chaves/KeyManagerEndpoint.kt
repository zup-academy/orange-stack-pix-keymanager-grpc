package br.com.zup.edu.chaves

import br.com.zup.edu.chaves.extension.toModel
import br.com.zup.edu.grpc.*
import br.com.zup.edu.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

// 16
@ErrorHandler // 1
@Singleton
class KeyManagerEndpoint(
    @Inject private val service: NovaChavePixService, // 1
) : KeymanagerGrpcServiceGrpc.KeymanagerGrpcServiceImplBase() { // 1

    // 12 -> 5
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

    // 5 -> 2
    override fun remove(
        request: RemoveChavePixRequest, // 1
        responseObserver: StreamObserver<RemoveChavePixResponse>, // 1
    ) {

        service.remove(clienteId = request.clienteId, pixId = request.pixId)

        responseObserver.onNext(RemoveChavePixResponse.newBuilder()
                                            .setClienteId(request.clienteId)
                                            .setPixId(request.pixId)
                                            .build())
        responseObserver.onCompleted()
    }

    // 19 -> 12 -> 6
    override fun carrega(
        request: CarregaChavePixRequest, // 1
        responseObserver: StreamObserver<CarregaChavePixResponse>, // 1
    ) {

        val filtro = request.toModel() // 2
        var chaveInfo = service.carregaPor(filtro) // 1

        /**
         * Abordagens:
         *
         *  1. Delegate: Command Pattern (sub-controllers, services etc)
         *      - new CarregaChavePixController().carrega(request, responseObserver)
         *  2. Delegate: eventos pub/sub
         *      - publisher.publish(CarregaChaveEvent(request, responseObserver)) // sincrono
         *  3. Aumentar pontos para classes gRPC
         *  4. Ignorar contagem de pontos de classes gRPC
         *  5. Granularizar serviços no Protobuf
         */
        responseObserver.onNext(CarregaChavePixResponseConverter().convert(chaveInfo)) // 1
        responseObserver.onCompleted()
    }

}

