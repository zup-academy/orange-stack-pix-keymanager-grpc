package br.com.zup.edu.chaves

import br.com.zup.edu.chaves.integration.ChavePixInfo
import br.com.zup.edu.chaves.integration.bcb.BancoCentralClient
import br.com.zup.edu.chaves.integration.bcb.CreatePixKeyRequest
import br.com.zup.edu.chaves.integration.bcb.DeletePixKeyRequest
import br.com.zup.edu.chaves.integration.itau.ContasDeClientesNoItauClient
import br.com.zup.edu.shared.validation.ValidUUID
import br.com.zup.pix.chaves.ChavePix
import br.com.zup.pix.chaves.ContaAssociada
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class NovaChavePixService(@Inject val itauClient: ContasDeClientesNoItauClient,
                          @Inject val repository: ChavePixRepository,
                          @Inject val bcbClient: BancoCentralClient,) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        // 1. verifica se chave já existe no sistema
        if (repository.existsByChave(novaChave.chave))
            throw IllegalArgumentException("Chave Pix '${novaChave.chave}' existente")

        // 2. busca dados da conta no ERP do ITAU
        val response = itauClient.buscaContaPorTipo(novaChave.clienteId!!, novaChave.tipoDeConta!!.name)
        val conta = response.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        // 3. grava no banco de dados
        val chave = novaChave.toModel(conta)
        repository.save(chave)

        // 4. registra chave no BCB
        val bcbRequest = CreatePixKeyRequest.of(chave).also {
            LOGGER.info("Registrando chave Pix no Banco Central do Brasil (BCB): $it")
        }

        val bcbResponse = bcbClient.create(bcbRequest)
        if (bcbResponse.status != HttpStatus.CREATED)
            throw IllegalStateException("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)")

        // 5. atualiza chave do dominio com chave gerada pelo BCB
        if (chave.isAleatoria()) {
            chave.chave = bcbResponse.body()!!.key
        }

        return chave
    }

    @Transactional
    fun remove(
        @NotBlank @ValidUUID(message = "cliente ID com formato inválido") clienteId: String?,
        @NotBlank @ValidUUID(message = "pix ID com formato inválido") pixId: String?,
    ) {

        val uuidPixId = UUID.fromString(pixId)
        val uuidClienteId = UUID.fromString(clienteId)

        val chave = repository.findById(uuidPixId)
            .orElseThrow { IllegalStateException("Chave Pix não encontrada") }

        if (!chave.pertenceAo(uuidClienteId)) {
            throw IllegalStateException("Cliente nao tem acesso a esta chave Pix")
        }

        repository.deleteById(uuidPixId)

        val request = DeletePixKeyRequest(
            key = chave.chave,
            participant = ContaAssociada.ITAU_UNIBANCO_ISPB
        )

        val bcbResponse = bcbClient.delete(key = chave.chave, request = request)
        if (bcbResponse.status != HttpStatus.OK) {
            throw IllegalStateException("Erro ao remover chave Pix no Banco Central do Brasil (BCB)")
        }
    }

    fun carregaPor(@Valid filtro: Filtro): ChavePixInfo? {
        return when (filtro) {
            is Filtro.PorPixId -> {
                repository.findById(filtro.pixIdAsUuid())
                            .filter { it.pertenceAo(filtro.clienteIdAsUuid()) }
                            .map(ChavePixInfo::of)
                            .orElse(null)
            }
            is Filtro.PorChave -> carregaPorChave(filtro.chave)
            is Filtro.Invalido -> null
        }
    }

    private fun carregaPorChave(chave: String): ChavePixInfo? {
        return repository.findByChave(chave)
            .map(ChavePixInfo::of)
            .orElseGet {
                LOGGER.info("Consultando chave Pix '$chave' no Banco Central do Brasil (BCB)")

                val response = bcbClient.findByKey(chave)
                when (response.status) {
                    HttpStatus.OK -> response.body()?.toModel()
                    else -> null
            }
        }
    }

}