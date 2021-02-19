package br.com.zup.pix.chaves

import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(uniqueConstraints = [UniqueConstraint(
        name = "uk_chave_pix",
        columnNames = ["chave"]
)])
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
        var chave: String,

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

        /**
         * Verifica se esta chave pertence a este cliente
         */
        fun pertenceAo(clienteId: UUID) = this.clienteId.equals(clienteId)

        /**
         * Verifica se é chave uma aleatória
         */
        fun isAleatoria(): Boolean {
                return tipo == TipoDeChave.ALEATORIA
        }

}