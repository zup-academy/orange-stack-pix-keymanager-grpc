package br.com.zup.edu.pix.registra

import br.com.zup.edu.grpc.RegistraChavePixRequest
import br.com.zup.edu.pix.TipoDeChave
import br.com.zup.edu.pix.TipoDeConta
import br.com.zup.edu.pix.registra.NovaChavePix

fun RegistraChavePixRequest.toModel() : NovaChavePix {
    return NovaChavePix( // 1
        clienteId = clienteId,
        tipo = TipoDeChave.valueOf(tipoDeChave.name), // 1
        chave = chave,
        tipoDeConta = TipoDeConta.valueOf(tipoDeConta.name) // 1
    )
}
