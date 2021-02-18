package br.com.zup.edu.chaves

import br.com.zup.edu.chaves.integration.ChavePixInfo
import br.com.zup.edu.grpc.*
import br.com.zup.edu.grpc.CarregaChavePixRequest.FiltroCase.*
import br.com.zup.pix.chaves.ChavePix
import br.com.zup.pix.chaves.TipoDeChave
import br.com.zup.pix.chaves.TipoDeConta
import com.google.protobuf.Any
import com.google.protobuf.Timestamp
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.persistence.PersistenceException
import javax.validation.ConstraintViolationException
import org.hibernate.exception.ConstraintViolationException as DatabaseConstraintError

@Singleton
class KeyManagerEndpoint(
    @Inject val service: NovaChavePixService, // 1
) : KeymanagerGrpcServiceGrpc.KeymanagerGrpcServiceImplBase() { // 1

    // 12
    override fun registra(
        request: RegistraChavePixRequest, // 1
        responseObserver: StreamObserver<RegistraChavePixResponse> // 1
    ) {

        val novaChave = NovaChavePix( // 1
            clienteId = request.clienteId,
            tipo = TipoDeChave.valueOf(request.tipoDeChave.name), // 1
            chave = request.chave,
            tipoDeConta = TipoDeConta.valueOf(request.tipoDeConta.name) // 1
        )

        try { // 1
            val chaveCriada: ChavePix = service.registra(novaChave) // 1

            responseObserver.onNext(RegistraChavePixResponse.newBuilder()
                                                .setClienteId(chaveCriada.clienteId.toString())
                                                .setPixId(chaveCriada.id.toString())
                                                .build())

            responseObserver.onCompleted()

        } catch (e: ConstraintViolationException) { // 1

            e.printStackTrace()
            responseObserver.onError(handleInvalidArguments(e))

        } catch (e: PersistenceException) { // 1

            e.printStackTrace()
            responseObserver.onError(handlePersistenceException(e))

        } catch (e: Throwable) { // 1
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

    override fun carrega(
        request: CarregaChavePixRequest,
        responseObserver: StreamObserver<CarregaChavePixResponse>,
    ) {

        val filtro: Filtro = when(request.filtroCase) {
            PIXID -> request.pixId.let {
                Filtro.PorPixId(clienteId = it.clienteId, pixId = it.pixId)
            }
            CHAVE -> Filtro.PorChave(request.chave)
            FILTRO_NOT_SET -> Filtro.Invalido() // IMPROVEMENT: poderia finalizar fluxo com INVALID_ARGUMENT
        }

        var chaveInfo = try {
            service.carregaPor(filtro)
        } catch (e: ConstraintViolationException) {
            e.printStackTrace()
            responseObserver.onError(handleInvalidArguments(e))
            return;
        }

        if (chaveInfo == null ) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("Chave Pix não encontrada")
                .asRuntimeException());
            return
        }

        responseObserver.onNext(CarregaChavePixResponse.newBuilder()
            .setClienteId(chaveInfo.clienteId?.toString() ?: "") // Protobuf usa "" como default value para String
            .setPixId(chaveInfo.pixId?.toString() ?: "") // Protobuf usa "" como default value para String
            .setChave(CarregaChavePixResponse.ChavePix
                .newBuilder()
                .setTipo(br.com.zup.edu.grpc.TipoDeChave.valueOf(chaveInfo.tipo.name))
                .setChave(chaveInfo.chave)
                .setConta(CarregaChavePixResponse.ChavePix.ContaInfo.newBuilder()
                    .setTipo(br.com.zup.edu.grpc.TipoDeConta.valueOf(chaveInfo.tipoDeConta.name))
                    .setInstituicao(chaveInfo.conta.instituicao)
                    .setNomeDoTitular(chaveInfo.conta.nomeDoTitular)
                    .setCpfDoTitular(chaveInfo.conta.cpfDoTitular)
                    .setAgencia(chaveInfo.conta.agencia)
                    .setNumeroDaConta(chaveInfo.conta.numeroDaConta)
                    .build()
                )
                .setCriadaEm(chaveInfo.registradaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
            )
            .build()
        ).also {
            responseObserver.onCompleted()
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

