package br.com.zup.edu.pix

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class ChavePixTest {

    companion object {
        val TIPOS_DE_CHAVES_EXCETO_ALEATORIO = TipoDeChave.values().filterNot { it == TipoDeChave.ALEATORIA }
    }

    @Test
    fun deveChavePertencerAoCliente() {

        val clienteId = UUID.randomUUID()
        val outroClienteId = UUID.randomUUID()

        with (newChave(tipo = TipoDeChave.ALEATORIA, clienteId = clienteId)) {
            assertTrue(this.pertenceAo(clienteId))
            assertFalse(this.pertenceAo(outroClienteId))
        }
    }

    @Test
    fun deveChaveSerDoTipoAleatoria() {
        with (newChave(TipoDeChave.ALEATORIA)) {
            assertTrue(this.isAleatoria())
        }
    }

    @Test
    fun naoDeveChaveSerDoTipoAleatoria() {
        TIPOS_DE_CHAVES_EXCETO_ALEATORIO
            .forEach {
                assertFalse(newChave(it).isAleatoria())
            }
    }

    @Test
    fun deveAtualizarChaveQuandoChaveForAleatoria() {
        with (newChave(TipoDeChave.ALEATORIA)) {
            assertTrue(this.atualiza("nova-chave"))
            assertEquals("nova-chave", this.chave)
        }
    }

    @Test
    fun naoDeveAtualizarChaveQuandoChaveForDiferenteDeAleatoria() {

        val original = "<chave-aleatoria-qualquer>"

        TIPOS_DE_CHAVES_EXCETO_ALEATORIO
            .forEach {
                with (newChave(tipo = it, chave = original)) {
                    assertFalse(this.atualiza("nova-chave"))
                    assertEquals(original, this.chave)
                }
            }
    }

    private fun newChave(
        tipo: TipoDeChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID(),
    ): ChavePix {
        return ChavePix(
            clienteId = clienteId,
            tipo = tipo,
            chave = chave,
            tipoDeConta = TipoDeConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "UNIBANCO ITAU",
                nomeDoTitular = "Rafael Ponte",
                cpfDoTitular = "12345678900",
                agencia = "1218",
                numeroDaConta = "123456"
            )
        )
    }

}