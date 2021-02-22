package br.com.zup.edu.chaves

import br.com.zup.edu.chaves.integration.ChavePixInfo
import br.com.zup.edu.grpc.CarregaChavePixResponse
import com.google.protobuf.Timestamp
import java.time.ZoneId

class CarregaChavePixResponseConverter {

    fun convert(chaveInfo: ChavePixInfo): CarregaChavePixResponse {
        return CarregaChavePixResponse.newBuilder()
            .setClienteId(chaveInfo.clienteId?.toString() ?: "") // Protobuf usa "" como default value para String
            .setPixId(chaveInfo.pixId?.toString() ?: "") // Protobuf usa "" como default value para String
            .setChave(CarregaChavePixResponse.ChavePix // 1
                .newBuilder()
                .setTipo(br.com.zup.edu.grpc.TipoDeChave.valueOf(chaveInfo.tipo.name)) // 2
                .setChave(chaveInfo.chave)
                .setConta(CarregaChavePixResponse.ChavePix.ContaInfo.newBuilder() // 1
                    .setTipo(br.com.zup.edu.grpc.TipoDeConta.valueOf(chaveInfo.tipoDeConta.name)) // 2
                    .setInstituicao(chaveInfo.conta.instituicao) // 1 (Conta)
                    .setNomeDoTitular(chaveInfo.conta.nomeDoTitular)
                    .setCpfDoTitular(chaveInfo.conta.cpfDoTitular)
                    .setAgencia(chaveInfo.conta.agencia)
                    .setNumeroDaConta(chaveInfo.conta.numeroDaConta)
                    .build()
                )
                .setCriadaEm(chaveInfo.registradaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
            )
            .build()
    }

}
