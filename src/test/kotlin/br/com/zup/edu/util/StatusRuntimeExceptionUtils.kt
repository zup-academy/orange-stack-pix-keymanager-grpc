package br.com.zup.edu.util

import com.google.rpc.BadRequest
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto

class StatusRuntimeExceptionUtils {

    companion object {

        fun violationsFrom(exception: StatusRuntimeException): List<Pair<String, String>>? {

            val details = StatusProto.fromThrowable(exception)
                ?.detailsList?.get(0)!!
                .unpack(BadRequest::class.java)

            return details.fieldViolationsList
                .map { Pair(it.field, it.description) }
        }

    }
}