package br.com.zup.pix.chaves

import javax.persistence.Column
import javax.persistence.Embeddable
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size
import kotlin.math.max

@Embeddable
class ContaAssociada(
        @field:NotBlank
        @Column(name = "conta_instituicao", nullable = false)
        val instituicao: String,

        @field:NotBlank
        @Column(name = "conta_titular_nome", nullable = false)
        val nomeDoTitular: String,

        @field:NotBlank
        @field:Size(max = 11)
        @Column(name = "conta_titular_cpf", length = 11, nullable = false)
        val cpfDoTitular: String,

        @field:NotBlank
        @field:Size(max = 4)
        @Column(name = "conta_agencia", length = 4, nullable = false)
        val agencia: String,

        @field:NotBlank
        @field:Size(max = 6)
        @Column(name = "conta_numero", length = 6, nullable = false)
        val numeroDaConta: String
) {

        companion object {
                public val ITAU_UNIBANCO_ISPB: String = "60701190"
        }
}