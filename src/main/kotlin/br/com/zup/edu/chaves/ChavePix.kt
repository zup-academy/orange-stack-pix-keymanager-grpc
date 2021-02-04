package br.com.zup.pix.chaves

import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
class ChavePix(
        val clienteId: UUID,
        @Enumerated(EnumType.STRING)
        val tipo: TipoDeChave,
        @Column(unique = true)
        val chave: String,
        @Enumerated(EnumType.STRING)
        val tipoDeConta: TipoDeConta,
        @Embedded
        val conta: ContaAssociada
) {
        @Id
        @GeneratedValue
        val id: UUID? = null
        val criadaEm: LocalDateTime = LocalDateTime.now()

        override fun toString(): String {
                return "ChavePix(clienteId=$clienteId, tipo=$tipo, chave='$chave', tipoDeConta=$tipoDeConta, conta=$conta, id=$id, criadaEm=$criadaEm)"
        }

}