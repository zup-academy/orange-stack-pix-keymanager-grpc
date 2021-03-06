package br.com.zup.edu.pix.remove

import br.com.zup.edu.grpc.KeymanagerRemoveGrpcServiceGrpc
import br.com.zup.edu.grpc.RemoveChavePixRequest
import br.com.zup.edu.grpc.RemoveChavePixResponse
import br.com.zup.edu.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler // 1
@Singleton
class RemoveChaveEndpoint(@Inject private val service: RemoveChaveService,) // 1
    : KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceImplBase() { // 1

    // 5
    override fun remove(
        request: RemoveChavePixRequest, // 1
        responseObserver: StreamObserver<RemoveChavePixResponse>, // 1
    ) {

        service.remove(clienteId = request.clienteId, pixId = request.pixId)

        responseObserver.onNext(RemoveChavePixResponse.newBuilder() // 1
                                    .setClienteId(request.clienteId)
                                    .setPixId(request.pixId)
                                    .build())
        responseObserver.onCompleted()
    }

}