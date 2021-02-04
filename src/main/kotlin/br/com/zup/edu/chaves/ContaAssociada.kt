package br.com.zup.pix.chaves

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class ContaAssociada(
        @Column(name = "conta_instituicao")
        val instituicao: String,
        @Column(name = "conta_titular_nome")
        val nomeDoTitular: String,
        @Column(name = "conta_titular_cpf", length = 11)
        val cpfDoTitular: String,
        @Column(name = "conta_agencia", length = 4)
        val agencia: String,
        @Column(name = "conta_numero", length = 6)
        val numeroDaConta: String
) {

        companion object {
                public val ITAU_UNIBANCO_ISPB: String = "60701190"
        }
}