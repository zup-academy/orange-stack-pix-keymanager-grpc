package br.com.zup.edu.pix.registra

import br.com.zup.edu.grpc.RegistraChavePixRequest
import br.com.zup.edu.grpc.TipoDeChave.*
import br.com.zup.edu.grpc.TipoDeConta.*
import br.com.zup.edu.pix.TipoDeChave
import br.com.zup.edu.pix.TipoDeConta
import br.com.zup.edu.pix.registra.NovaChavePix

fun RegistraChavePixRequest.toModel() : NovaChavePix {
    return NovaChavePix( // 1
        clienteId = clienteId,
        tipo = when (tipoDeChave) {
            UNKNOWN_TIPO_CHAVE -> null
            else -> TipoDeChave.valueOf(tipoDeChave.name) // 1
        },
        chave = chave,
        tipoDeConta = when (tipoDeConta) {
            UNKNOWN_TIPO_CONTA -> null
            else -> TipoDeConta.valueOf(tipoDeConta.name) // 1
        }
    )
}
