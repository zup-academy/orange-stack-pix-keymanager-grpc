package br.com.zup.edu.pix.registra

import io.micronaut.core.annotation.AnnotationValue
import io.micronaut.validation.validator.constraints.ConstraintValidator
import io.micronaut.validation.validator.constraints.ConstraintValidatorContext
import javax.inject.Singleton
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.reflect.KClass

@MustBeDocumented
@Target(CLASS, TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = [ValidPixKeyValidator::class])
annotation class ValidPixKey(
    val message: String = "chave Pix inválida (\${validatedValue.tipo})",
    val groups: Array<KClass<Any>> = [],
    val payload: Array<KClass<Payload>> = [],
)


@Singleton
class ValidPixKeyValidator: ConstraintValidator<ValidPixKey, NovaChavePix> {

    override fun isValid(
        value: NovaChavePix?,
        annotationMetadata: AnnotationValue<ValidPixKey>,
        context: ConstraintValidatorContext,
    ): Boolean {

        // must be validated with @NotNull
        if (value?.tipo == null) {
            return true
        }

        /**
         * TODO: we could use Custom property paths here to associate this error to the field "chave"
         * but it seems like Micronaut does not support it, so we'd have to add the dependency "micronaut-hibernate-validator"
         * and uses the Bean Validation API directly
         *
         * - https://docs.micronaut.io/latest/guide/index.html#beanValidation
         * - https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/#section-custom-property-paths
         */

        return value.tipo.valida(value.chave)
    }

}