package br.com.zup.edu.chaves

import br.com.zup.edu.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
sealed class Filtro {

    @Introspected
    data class PorPixId(
        @field:NotBlank @field:ValidUUID val clienteId: String,
        @field:NotBlank @field:ValidUUID val pixId: String,
    ) : Filtro() {

        fun pixIdAsUuid() = UUID.fromString(pixId)
        fun clienteIdAsUuid() = UUID.fromString(clienteId)
    }

    @Introspected
    data class PorChave(@field:NotBlank @Size(max = 77) val chave: String) : Filtro()

    @Introspected
    class Invalido() : Filtro()
}


