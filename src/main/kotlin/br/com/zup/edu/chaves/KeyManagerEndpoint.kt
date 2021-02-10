package br.com.zup.edu.chaves

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
    @Inject val service: NovaChavePixService,
    @Inject val repository: ChavePixRepository,
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

    override fun carrega(
        request: CarregaChavePixRequest,
        responseObserver: StreamObserver<CarregaChavePixResponse>,
    ) {

        if (request.clienteId.isNullOrBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                                        .withDescription("Identificador do cliente deve ser informado")
                                        .asRuntimeException());
            return
        }

        if (request.pixId.isNullOrBlank() && request.chave.isNullOrBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                                        .withDescription("Pix ID ou Chave deve ser informado")
                                        .asRuntimeException());
            return
        }

        val chave = when (request.filtroCase) {
            PIXID -> repository.findById(UUID.fromString(request.pixId))
            CHAVE -> repository.findByChave(request.chave)
            FILTRO_NOT_SET -> Optional.empty()
        }

        if (chave.isEmpty) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                                        .withDescription("Chave Pix não encontrada")
                                        .asRuntimeException());
            return
        }

        val titular = UUID.fromString(request.clienteId)
        if (!chave.get().pertenceAo(titular)) {
            responseObserver.onError(Status.PERMISSION_DENIED
                                        .withDescription("Cliente não tem acesso a esta chave Pix")
                                        .asRuntimeException());
            return
        }

        val k = chave.get()
        responseObserver.onNext(CarregaChavePixResponse.newBuilder()
            .setClienteId(k.clienteId.toString())
            .setChave(CarregaChavePixResponse.ChavePix
                .newBuilder()
                .setPixId(k.id.toString())
                .setTipo(br.com.zup.edu.grpc.TipoDeChave.valueOf(k.tipo.name))
                .setChave(k.chave)
                .setConta(CarregaChavePixResponse.ChavePix.ContaInfo.newBuilder()
                    .setTipo(br.com.zup.edu.grpc.TipoDeConta.valueOf(k.tipoDeConta.name))
                    .setInstituicao(k.conta.instituicao)
                    .setNomeDoTitular(k.conta.nomeDoTitular)
                    .setCpfDoTitular(k.conta.cpfDoTitular)
                    .setAgencia(k.conta.agencia)
                    .setNumeroDaConta(k.conta.numeroDaConta)
                    .build()
                )
                .setCriadaEm(k.criadaEm.let {
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

