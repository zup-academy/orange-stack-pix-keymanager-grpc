package br.com.zup.edu.chaves.endpoints

import br.com.zup.edu.chaves.CarregaChavePixResponseConverter
import br.com.zup.edu.chaves.ChavePixRepository
import br.com.zup.edu.chaves.extension.toModel
import br.com.zup.edu.chaves.integration.bcb.BancoCentralClient
import br.com.zup.edu.grpc.CarregaChavePixRequest
import br.com.zup.edu.grpc.CarregaChavePixResponse
import br.com.zup.edu.grpc.KeymanagerCarregaGrpcServiceGrpc
import br.com.zup.edu.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@ErrorHandler // 1
@Singleton
class CarregaChaveEndpoint(
    @Inject private val repository: ChavePixRepository, // 1
    @Inject private val bcbClient: BancoCentralClient, // 1
    @Inject private val validator: Validator,
) : KeymanagerCarregaGrpcServiceGrpc.KeymanagerCarregaGrpcServiceImplBase() { // 1

    // 9
    override fun carrega(
        request: CarregaChavePixRequest, // 1
        responseObserver: StreamObserver<CarregaChavePixResponse>, // 1
    ) {

        val filtro = request.toModel(validator) // 2
        val chaveInfo = filtro.filtra(repository = repository, bcbClient = bcbClient)

        responseObserver.onNext(CarregaChavePixResponseConverter().convert(chaveInfo)) // 1
        responseObserver.onCompleted()
    }
}