package br.com.zup.edu.integration.bcb


import br.com.zup.edu.pix.carrega.ChavePixInfo
import br.com.zup.edu.pix.ChavePix
import br.com.zup.edu.pix.ContaAssociada
import br.com.zup.edu.pix.TipoDeChave
import br.com.zup.edu.pix.TipoDeConta
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
import java.time.LocalDateTime

/**
 * https://www.bcb.gov.br/content/estabilidadefinanceira/forumpireunioes/api-dict.html
 */
@Client("\${bcb.pix.url}")
interface BancoCentralClient {

    @Post(
        "/api/v1/pix/keys",
        produces = [MediaType.APPLICATION_XML],
        consumes = [MediaType.APPLICATION_XML]
    )
    fun create(@Body request: CreatePixKeyRequest): HttpResponse<CreatePixKeyResponse>

    @Delete("/api/v1/pix/keys/{key}",
        produces = [MediaType.APPLICATION_XML],
        consumes = [MediaType.APPLICATION_XML]
    )
    fun delete(@PathVariable key: String, @Body request: DeletePixKeyRequest): HttpResponse<DeletePixKeyResponse>

    @Get("/api/v1/pix/keys/{key}",
        consumes = [MediaType.APPLICATION_XML])
    fun findByKey(@PathVariable key: String): HttpResponse<PixKeyDetailsResponse>

}

data class DeletePixKeyRequest(
    val key: String,
    val participant: String = ContaAssociada.ITAU_UNIBANCO_ISPB,
)

data class DeletePixKeyResponse(
        val key: String,
        val participant: String,
        val deletedAt: LocalDateTime
)

data class CreatePixKeyRequest(
    val keyType: PixKeyType,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner
) {

    companion object {

        fun of(chave: ChavePix): CreatePixKeyRequest {
            return CreatePixKeyRequest(
                    keyType = PixKeyType.by(chave.tipo),
                    key = chave.chave,
                    bankAccount = BankAccount(
                            participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
                            branch = chave.conta.agencia,
                            accountNumber = chave.conta.numeroDaConta,
                            accountType = BankAccount.AccountType.by(chave.tipoDeConta),
                    ),
                    owner = Owner(
                            type = Owner.OwnerType.NATURAL_PERSON,
                            name = chave.conta.nomeDoTitular,
                            taxIdNumber = chave.conta.cpfDoTitular
                    )
            )
        }
    }
}

data class PixKeyDetailsResponse (
    val keyType: PixKeyType,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner,
    val createdAt: LocalDateTime
) {

    fun toModel(): ChavePixInfo {
        return ChavePixInfo(
            tipo = keyType.domainType!!,
            chave = this.key,
            tipoDeConta = when (this.bankAccount.accountType) {
                    BankAccount.AccountType.CACC -> TipoDeConta.CONTA_CORRENTE
                    BankAccount.AccountType.SVGS -> TipoDeConta.CONTA_POUPANCA
                },
            conta = ContaAssociada(
                instituicao = bankAccount.participant,
                nomeDoTitular = owner.name,
                cpfDoTitular = owner.taxIdNumber,
                agencia = bankAccount.branch,
                numeroDaConta = bankAccount.accountNumber
            )
        )
    }
}

data class CreatePixKeyResponse (
    val keyType: PixKeyType,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner,
    val createdAt: LocalDateTime
)

data class Owner(
    val type: OwnerType,
    val name: String,
    val taxIdNumber: String
) {

    enum class OwnerType {
        NATURAL_PERSON,
        LEGAL_PERSON
    }
}

data class BankAccount(
        /**
         * 60701190 ITAÚ UNIBANCO S.A.
         * https://www.bcb.gov.br/pom/spb/estatistica/port/ASTR003.pdf (line 221)
         */
        val participant: String,
        val branch: String,
        val accountNumber: String,
        val accountType: AccountType
) {

    /**
     * https://open-banking.pass-consulting.com/json_ExternalCashAccountType1Code.html
     */
    enum class AccountType() {

        CACC, // Current: Account used to post debits and credits when no specific account has been nominated
        SVGS; // Savings: Savings

        companion object {
            fun by(domainType: TipoDeConta): AccountType {
                return when (domainType) {
                    TipoDeConta.CONTA_CORRENTE -> CACC
                    TipoDeConta.CONTA_POUPANCA -> SVGS
                }
            }
        }
    }

}

enum class PixKeyType(val domainType: TipoDeChave?) {

    CPF(TipoDeChave.CPF),
    CNPJ(null),
    PHONE(TipoDeChave.CELULAR),
    EMAIL(TipoDeChave.EMAIL),
    RANDOM(TipoDeChave.ALEATORIA);

    companion object {

        private val mapping = PixKeyType.values().associateBy(PixKeyType::domainType)

        fun by(domainType: TipoDeChave): PixKeyType {
            return  mapping[domainType] ?: throw IllegalArgumentException("PixKeyType invalid or not found for $domainType")
        }
    }
}
