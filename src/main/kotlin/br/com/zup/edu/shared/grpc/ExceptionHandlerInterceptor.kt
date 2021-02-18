package br.com.zup.edu.shared.grpc

import io.grpc.*
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import javax.validation.ConstraintViolationException
import io.grpc.ServerCall
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener

@Singleton
class ExceptionHandlerInterceptor : ServerInterceptor {

    private val logger = LoggerFactory.getLogger(ExceptionHandlerInterceptor::class.java)

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {

        fun handleException(call: ServerCall<ReqT, RespT>, t: Exception) {
            val translatedStatus = when (t) {
                is ConstraintViolationException -> Status.INVALID_ARGUMENT
                is IllegalArgumentException -> Status.INVALID_ARGUMENT
                is IllegalStateException -> Status.FAILED_PRECONDITION
                else -> Status.UNKNOWN
            }
            val newStatus = translatedStatus.withDescription(t?.message).withCause(t)
            call.close(newStatus, headers)
        }

        val listener: ServerCall.Listener<ReqT> = try {
            next.startCall(call, headers)
        } catch (ex: Exception) {
            handleException(call, ex)
            throw ex
        }

        return object : SimpleForwardingServerCallListener<ReqT>(listener) {
            // No point in overriding onCancel and onComplete; it's already too late
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (ex: Exception) {
                    handleException(call, ex)
                    throw ex
                }
            }

            override fun onReady() {
                try {
                    super.onReady()
                } catch (ex: Exception) {
                    handleException(call, ex)
                    throw ex
                }
            }
        }
    }
}