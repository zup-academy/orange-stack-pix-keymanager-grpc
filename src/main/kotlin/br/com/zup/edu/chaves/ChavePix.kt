package br.com.zup.pix.chaves

import br.com.zup.edu.shared.validation.ValidPixKey
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@ValidPixKey
@Entity
class ChavePix(
        @field:NotNull
        @Column(nullable = false)
        val clienteId: UUID,

        @field:NotNull
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        val tipo: TipoDeChave,

        @field:NotBlank
        @Column(unique = true, nullable = false)
        val chave: String,

        @field:NotNull
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        val tipoDeConta: TipoDeConta,

        @field:Valid
        @Embedded
        val conta: ContaAssociada
) {
        @Id
        @GeneratedValue
        val id: UUID? = null

        @Column(nullable = false)
        val criadaEm: LocalDateTime = LocalDateTime.now()

        override fun toString(): String {
                return "ChavePix(clienteId=$clienteId, tipo=$tipo, chave='$chave', tipoDeConta=$tipoDeConta, conta=$conta, id=$id, criadaEm=$criadaEm)"
        }

}