package br.com.zup.edu.pix.carrega

import br.com.zup.edu.integration.bcb.BancoCentralClient
import br.com.zup.edu.pix.ChavePixRepository
import br.com.zup.edu.pix.ChavePixNaoEncontradaException
import br.com.zup.edu.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

// 10
@Introspected
sealed class Filtro {

    /**
     * Deve retornar chave encontrada ou lançar um exceção de erro de chave não encontrada
     */
    abstract fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClient): ChavePixInfo // 3

    @Introspected
    data class PorPixId(
        @field:NotBlank @field:ValidUUID val clienteId: String, // 1
        @field:NotBlank @field:ValidUUID val pixId: String,
    ) : Filtro() { // 1

        fun pixIdAsUuid() = UUID.fromString(pixId)
        fun clienteIdAsUuid() = UUID.fromString(clienteId)

        override fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClient): ChavePixInfo {
            return repository.findById(pixIdAsUuid())
                             .filter { it.pertenceAo(clienteIdAsUuid()) }
                             .map(ChavePixInfo::of)
                             .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada") }
        }
    }

    @Introspected
    data class PorChave(@field:NotBlank @Size(max = 77) val chave: String) : Filtro() { // 1

        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        override fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClient): ChavePixInfo {
            return repository.findByChave(chave)
                    .map(ChavePixInfo::of)
                    .orElseGet {
                        LOGGER.info("Consultando chave Pix '$chave' no Banco Central do Brasil (BCB)")

                        val response = bcbClient.findByKey(chave) // 1
                        when (response.status) { // 1
                            HttpStatus.OK -> response.body()?.toModel() // 1
                            else -> throw ChavePixNaoEncontradaException("Chave Pix não encontrada") // 1
                        }
                    }
        }
    }

    @Introspected
    class Invalido() : Filtro() {

        override fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClient): ChavePixInfo {
            throw IllegalArgumentException("Chave Pix inválida ou não informada")
        }
    }
}


