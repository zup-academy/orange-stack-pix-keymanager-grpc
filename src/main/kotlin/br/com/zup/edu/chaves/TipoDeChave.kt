package br.com.zup.pix.chaves

import io.micronaut.validation.validator.constraints.EmailValidator
import org.hibernate.validator.internal.constraintvalidators.hv.br.CPFValidator

enum class TipoDeChave {

    CPF {
        override fun valida(chave: String?): Boolean {
            if (chave == null || chave.isBlank()) {
                return false
            }

            if (!chave.matches("[0-9]+".toRegex())) {
                return false
            }

            return CPFValidator().run {
                initialize(null)
                isValid(chave, null)
            }
        }
    },
    CELULAR {
        override fun valida(chave: String?): Boolean {
            if (chave == null || chave.isBlank()) {
                return false
            }
            return chave.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex())
        }
    },
    EMAIL {
        override fun valida(chave: String?): Boolean {
            if (chave == null || chave.isBlank()) {
                return false
            }
            return EmailValidator().run {
                initialize(null)
                isValid(chave, null)
            }
        }
    },
    ALEATORIA {
        override fun valida(chave: String?) = chave == null || chave.isBlank() // não deve se preenchida
    };

    abstract fun valida(chave: String?): Boolean
}
