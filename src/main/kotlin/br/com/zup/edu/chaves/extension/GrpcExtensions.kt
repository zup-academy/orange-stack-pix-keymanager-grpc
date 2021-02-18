package br.com.zup.edu.chaves.extension

import br.com.zup.edu.chaves.NovaChavePix
import br.com.zup.edu.grpc.RegistraChavePixRequest
import br.com.zup.pix.chaves.TipoDeChave
import br.com.zup.pix.chaves.TipoDeConta

fun RegistraChavePixRequest.toModel() : NovaChavePix {
    return NovaChavePix( // 1
        clienteId = clienteId,
        tipo = TipoDeChave.valueOf(tipoDeChave.name), // 1
        chave = chave,
        tipoDeConta = TipoDeConta.valueOf(tipoDeConta.name) // 1
    )
}