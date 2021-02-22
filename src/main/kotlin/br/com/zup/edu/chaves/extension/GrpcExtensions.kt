package br.com.zup.edu.chaves.extension

import br.com.zup.edu.chaves.Filtro
import br.com.zup.edu.chaves.NovaChavePix
import br.com.zup.edu.grpc.CarregaChavePixRequest
import br.com.zup.edu.grpc.CarregaChavePixRequest.FiltroCase.*
import br.com.zup.edu.grpc.CarregaChavePixResponse
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

fun CarregaChavePixRequest.toModel() : Filtro { // 1
    return when(filtroCase) { // 1
        PIXID -> pixId.let { // 1
            Filtro.PorPixId(clienteId = it.clienteId, pixId = it.pixId) // 1
        }
        CHAVE -> Filtro.PorChave(chave) // 2
        FILTRO_NOT_SET -> Filtro.Invalido() // 2
    }
}
