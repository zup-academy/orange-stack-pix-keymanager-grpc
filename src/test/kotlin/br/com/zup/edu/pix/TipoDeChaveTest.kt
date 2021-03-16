package br.com.zup.edu.pix

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TipoDeChaveTest {

    @Nested
    inner class ChaveAleatoriaTest {

        @Test
        fun `deve ser valido quando chave aleatoria for nula ou vazia`() {

            val tipoDechave = TipoDeChave.ALEATORIA

            assertTrue(tipoDechave.valida(null))
            assertTrue(tipoDechave.valida(""))
        }

        @Test
        fun `nao deve ser valido quando chave aleatoria possuir um valor`() {

            val tipoDechave = TipoDeChave.ALEATORIA

            assertFalse(tipoDechave.valida("um valor qualquer"))
        }
    }

    @Nested
    inner class CpfTest {

        @Test
        fun `deve ser valido quando cpf for um numero valido`() {

            val tipoDechave = TipoDeChave.CPF

            assertTrue(tipoDechave.valida("35060731332"))
        }

        @Test
        fun `nao deve ser valido quando cpf for um numero invalido`() {

            val tipoDechave = TipoDeChave.CPF

            assertFalse(tipoDechave.valida("35060731331"))
        }

        @Test
        fun `nao deve ser valido quando cpf nao for informado`() {

            val tipoDechave = TipoDeChave.CPF

            assertFalse(tipoDechave.valida(null))
            assertFalse(tipoDechave.valida(""))
        }
    }

    @Nested
    inner class CelularTest {

        @Test
        fun `deve ser valido quando celular for um numero valido`() {

            val tipoDechave = TipoDeChave.CELULAR

            assertTrue(tipoDechave.valida("+5511987654321"))
        }

        @Test
        fun `nao deve ser valido quando celular for um numero invalido`() {

            val tipoDechave = TipoDeChave.CELULAR

            assertFalse(tipoDechave.valida("11987654321"))
            assertFalse(tipoDechave.valida("+55a11987654321"))
        }

        @Test
        fun `nao deve ser valido quando celular for um numero nao for informado`() {

            val tipoDechave = TipoDeChave.CELULAR

            assertFalse(tipoDechave.valida(null))
            assertFalse(tipoDechave.valida(""))
        }
    }

    @Nested
    inner class EmailTest {

        @Test
        fun `deve ser valido quando email for endereco valido`() {

            val tipoDechave = TipoDeChave.EMAIL

            assertTrue(tipoDechave.valida("zup.edu@zup.com.br"))
        }

        @Test
        fun `nao deve ser valido quando email estiver em um formato invalido`() {

            val tipoDechave = TipoDeChave.EMAIL

            assertFalse(tipoDechave.valida("zup.eduzup.com.br"))
            assertFalse(tipoDechave.valida("zup.edu@zup.com."))
        }

        @Test
        fun `nao deve ser valido quando email nao for informado`() {

            val tipoDechave = TipoDeChave.EMAIL

            assertFalse(tipoDechave.valida(null))
            assertFalse(tipoDechave.valida(""))
        }
    }
}