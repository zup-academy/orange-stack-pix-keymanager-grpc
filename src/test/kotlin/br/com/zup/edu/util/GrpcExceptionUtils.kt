package br.com.zup.edu.util

import com.google.rpc.BadRequest
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto


/**
 * Extension Function para ajudar durante os testes de integração
 */
fun StatusRuntimeException.violations(): List<Pair<String, String>> {

    val details = StatusProto.fromThrowable(this)
                            ?.detailsList?.get(0)!!
                            .unpack(BadRequest::class.java)

    return details.fieldViolationsList
        .map { it.field to it.description } // same as Pair(it.field, it.description)
}
