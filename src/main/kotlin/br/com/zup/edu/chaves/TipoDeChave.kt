package br.com.zup.pix.chaves

import io.micronaut.validation.validator.constraints.EmailValidator
import org.hibernate.validator.internal.constraintvalidators.hv.br.CPFValidator

enum class TipoDeChave {

    CPF {
        override fun valida(chave: String) = CPFValidator().isValid(chave, null)
    },
    CELULAR {
        override fun valida(chave: String) = chave.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex())
    },
    EMAIL {
        override fun valida(chave: String) = EmailValidator().isValid(chave, null)
    },
    ALEATORIA {
        override fun valida(chave: String) = true
    };

    abstract fun valida(chave: String): Boolean
}
