package br.com.zup.edu.chaves.endpoints

import br.com.zup.edu.chaves.CarregaChavePixResponseConverter
import br.com.zup.edu.chaves.NovaChavePixService
import br.com.zup.edu.chaves.extension.toModel
import br.com.zup.edu.grpc.CarregaChavePixRequest
import br.com.zup.edu.grpc.CarregaChavePixResponse
import br.com.zup.edu.grpc.KeymanagerReadOnlyGrpcServiceGrpc
import br.com.zup.edu.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class CarregaChaveEndpoint(@Inject private val service: NovaChavePixService,) : KeymanagerReadOnlyGrpcServiceGrpc.KeymanagerReadOnlyGrpcServiceImplBase() {

    override fun carrega(
        request: CarregaChavePixRequest, // 1
        responseObserver: StreamObserver<CarregaChavePixResponse>, // 1
    ) {

        val filtro = request.toModel() // 2
        var chaveInfo = service.carregaPor(filtro) // 1

        responseObserver.onNext(CarregaChavePixResponseConverter().convert(chaveInfo)) // 1
        responseObserver.onCompleted()
    }
}