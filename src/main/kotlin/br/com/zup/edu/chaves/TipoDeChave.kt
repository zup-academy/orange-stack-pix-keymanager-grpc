package br.com.zup.pix.chaves

import io.micronaut.validation.validator.constraints.EmailValidator

enum class TipoDeChave {

    CPF {
        override fun valida(chave: String) = chave.matches("^[0-9]{11}\$".toRegex())
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
