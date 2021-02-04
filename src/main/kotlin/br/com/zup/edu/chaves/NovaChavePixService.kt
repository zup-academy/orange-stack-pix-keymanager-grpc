package br.com.zup.edu.chaves

import br.com.zup.edu.chaves.integration.itau.ContasDeClientesNoItauClient
import br.com.zup.pix.chaves.ChavePix
import io.micronaut.validation.Validated
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(@Inject val itauClient: ContasDeClientesNoItauClient,
                          @Inject val repository: ChavePixRepository,) {


    @Transactional
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        /**
         * 1. buscar dados da conta no ERP-ITAU
         * 2. gravar no banco de dados
         * 3. retornar resultado
         */

        // 1. buscar dados da conta no ERP-ITAU
        val response = itauClient.buscaContaPorTipo(novaChave.clienteId!!, novaChave.tipoDeConta!!.name)
        val conta = response.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        // 2. gravar no banco de dados
        val chave = novaChave.toModel(conta)
        return repository.save(chave)
    }
}