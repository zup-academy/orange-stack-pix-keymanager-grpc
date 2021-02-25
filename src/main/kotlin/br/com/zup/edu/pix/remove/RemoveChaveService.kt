package br.com.zup.edu.pix.remove

import br.com.zup.edu.integration.bcb.BancoCentralClient
import br.com.zup.edu.integration.bcb.DeletePixKeyRequest
import br.com.zup.edu.pix.ChavePixNaoEncontradaException
import br.com.zup.edu.pix.ChavePixRepository
import br.com.zup.edu.shared.validation.ValidUUID
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

// 8
@Validated
@Singleton
class RemoveChaveService(@Inject val repository: ChavePixRepository, // 1
                         @Inject val bcbClient: BancoCentralClient,) { // 1

    @Transactional
    fun remove(
        @NotBlank @ValidUUID(message = "cliente ID com formato inválido") clienteId: String?, // 1
        @NotBlank @ValidUUID(message = "pix ID com formato inválido") pixId: String?,
    ) {

        val uuidPixId = UUID.fromString(pixId)
        val uuidClienteId = UUID.fromString(clienteId)

        val chave = repository.findByIdAndClienteId(uuidPixId, uuidClienteId) // 1
            .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada ou não pertence ao cliente") } // 1

        repository.deleteById(uuidPixId)

        val request = DeletePixKeyRequest(chave.chave) // 1

        val bcbResponse = bcbClient.delete(key = chave.chave, request = request) // 1
        if (bcbResponse.status != HttpStatus.OK) { // 1
            throw IllegalStateException("Erro ao remover chave Pix no Banco Central do Brasil (BCB)")
        }
    }

}