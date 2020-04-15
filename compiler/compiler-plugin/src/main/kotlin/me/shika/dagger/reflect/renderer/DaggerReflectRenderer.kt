package me.shika.dagger.reflect.renderer

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.TypeSpec
import dagger.Dagger
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import java.io.File

class DaggerReflectRenderer(
    private val outputDir: File,
    private val component: ClassDescriptor,
    private val componentAnnotation: AnnotationDescriptor
) {
    private val componentClassName = component.fqNameSafe.run { ClassName(parent().asString(), shortName().asString()) }
    private val generatedComponentClassName =
        ClassName(componentClassName.packageName, GENERATED_COMPONENT_PREFIX + componentClassName.simpleName)

    fun generateFactory(factory: ClassDescriptor) =
        file {
            generatedComponent {
                reflectCreator(FACTORY_FUNCTION_NAME, FACTORY_REFLECT_NAME, factory.name.asString())
            }
        }

    fun generateBuilder(builder: ClassDescriptor) =
        file {
            generatedComponent {
                reflectCreator(BUILDER_FUNCTION_NAME, BUILDER_REFLECT_NAME, builder.name.asString())
            }
        }

    fun generateDefaultBuilder() =
        file {
            generatedComponent {
//                copyDaggerAnnotations()
//                defaultBuilder {
//                    modules(componentAnnotation)
//                    dependencies(componentAnnotation)
//                }
            }
        }

    private inline fun file(block: FileSpec.Builder.() -> Unit) =
        FileSpec.builder(generatedComponentClassName.packageName, generatedComponentClassName.simpleName)
            .apply(block)
            .build()
            .writeTo(outputDir)

    private inline fun FileSpec.Builder.generatedComponent(block: TypeSpec.Builder.() -> Unit) {
        addType(
            TypeSpec.classBuilder(generatedComponentClassName)
                .addModifiers(KModifier.ABSTRACT)
                .addType(
                    TypeSpec.companionObjectBuilder()
                        .addModifiers(KModifier.COMPANION)
                        .apply(block)
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.reflectCreator(name: String, creator: MemberName, resultTypeName: String) {
        val creatorClassName = componentClassName.nestedClass(resultTypeName)
        addFunction(
            FunSpec.builder(name)
                .returns(creatorClassName)
                .addAnnotation(JvmStatic::class.java)
                .addCode("return %M(%T::class.java)", creator, creatorClassName)
                .build()
        )
    }

    private fun AnnotationDescriptor.classList(name: String): List<KotlinType> =
        (this.allValueArguments[Name.identifier(name)] as ArrayValue)
            .value
            .map { (it as KClassValue).getType(component.module) }

    companion object {
        private const val GENERATED_COMPONENT_PREFIX = "Dagger"

        private const val FACTORY_FUNCTION_NAME = "factory"
        private val FACTORY_REFLECT_NAME = Dagger::class.member("factory")

        private const val BUILDER_FUNCTION_NAME = "builder"
        private val BUILDER_REFLECT_NAME = Dagger::class.member("builder")
    }
}
