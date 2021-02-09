package br.com.zup.edu.chaves

import br.com.zup.edu.grpc.*
import br.com.zup.pix.chaves.TipoDeChave
import br.com.zup.pix.chaves.TipoDeConta
import com.google.protobuf.Any
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.persistence.PersistenceException
import javax.validation.ConstraintViolationException
import org.hibernate.exception.ConstraintViolationException as DatabaseConstraintError

@Singleton
class KeyManagerEndpoint(
    @Inject val service: NovaChavePixService,
) : KeymanagerGrpcServiceGrpc.KeymanagerGrpcServiceImplBase() {

    override fun registra(
        request: RegistraChavePixRequest,
        responseObserver: StreamObserver<RegistraChavePixResponse>
    ) {

        val novaChave = NovaChavePix(
            clienteId = request.clienteId,
            tipo = TipoDeChave.valueOf(request.tipoDeChave.name),
            chave = request.chave,
            tipoDeConta = TipoDeConta.valueOf(request.tipoDeConta.name)
        )

        try {
            val chaveCriada = service.registra(novaChave)

            responseObserver.onNext(RegistraChavePixResponse.newBuilder()
                                                .setClienteId(chaveCriada.clienteId.toString())
                                                .setPixId(chaveCriada.id.toString())
                                                .build())

            responseObserver.onCompleted()

        } catch (e: ConstraintViolationException) {

            e.printStackTrace()
            responseObserver.onError(handleInvalidArguments(e))

        } catch (e: PersistenceException) {

            e.printStackTrace()
            responseObserver.onError(handlePersistenceException(e))

        } catch (e: Throwable) {
            e.printStackTrace()
            responseObserver.onError(Status.INTERNAL
                                        .withDescription(e.message)
                                        .withCause(e)
                                        .asRuntimeException())
        }
    }

    override fun remove(request: RemoveChavePixRequest, responseObserver: StreamObserver<RemoveChavePixResponse>) {
        try {
            service.remove(clienteId = request.clienteId, pixId = request.pixId)

            responseObserver.onNext(RemoveChavePixResponse.newBuilder()
                                                .setClienteId(request.clienteId)
                                                .setPixId(request.pixId)
                                                .build())
            responseObserver.onCompleted()

        } catch (e: ConstraintViolationException) {

            e.printStackTrace()
            responseObserver.onError(handleInvalidArguments(e))

        } catch (e: Throwable) {
            e.printStackTrace()
            responseObserver.onError(Status.INTERNAL
                                        .withDescription(e.message)
                                        .withCause(e)
                                        .asRuntimeException())
        }
    }

    private fun handlePersistenceException(e: PersistenceException): StatusRuntimeException {
        return when (e.cause) {
            is DatabaseConstraintError -> Status.INVALID_ARGUMENT
                                                .withDescription("chave Pix existente")
                                                .asRuntimeException()
            else -> Status.INTERNAL
                          .withDescription(e.message)
                          .withCause(e)
                          .asRuntimeException()
        }
    }

    private fun handleInvalidArguments(e: ConstraintViolationException): StatusRuntimeException {

        val details = BadRequest.newBuilder()
            .addAllFieldViolations(e.constraintViolations.map {
                BadRequest.FieldViolation.newBuilder()
                    .setField(it.propertyPath.last().name ?: "chave")
                    .setDescription(it.message)
                    .build()
            })
            .build()

        val statusProto = com.google.rpc.Status.newBuilder()
                                .setCode(Code.INVALID_ARGUMENT_VALUE)
                                .setMessage("Dados inválidos")
                                .addDetails(Any.pack(details))
                                .build()

        return StatusProto.toStatusRuntimeException(statusProto)
    }

}

