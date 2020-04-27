package me.shika.dagger.reflect.renderer

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.TypeSpec
import dagger.Component
import dagger.Dagger
import org.jetbrains.kotlin.com.intellij.psi.PsiClassOwner
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.io.File
import javax.inject.Scope

class DaggerReflectRenderer(
    private val outputDir: File,
    private val component: ClassDescriptor,
    private val componentAnnotation: AnnotationDescriptor
) {
    private val componentClassName = component.className()
    private val generatedComponentClassName =
        ClassName(
            packageName = componentClassName.packageName,
            simpleName = GENERATED_COMPONENT_PREFIX + componentClassName.simpleNames.joinToString(separator = "_")
        )

    fun generateFactory(factory: ClassDescriptor) =
        file {
            generatedComponent {
                companion {
                    reflectCreator(
                        FACTORY_FUNCTION_NAME,
                        FACTORY_REFLECT_NAME,
                        componentClassName.nestedClass(factory.name.asString())
                    )
                }
            }
        }

    fun generateBuilder(builder: ClassDescriptor) =
        file {
            generatedComponent {
                companion {
                    reflectCreator(
                        BUILDER_FUNCTION_NAME,
                        BUILDER_REFLECT_NAME,
                        componentClassName.nestedClass(builder.name.asString())
                    )
                }
            }
        }

    fun generateDefaultBuilder() =
        file {
            generatedComponent(isInterface = true) {
                val modules = componentAnnotation.classList(MODULES_PARAM)
                val dependencies = componentAnnotation.classList(DEPENDENCIES_PARAM)
                val scopes = component.annotations.filter { it.isScope }

                addAnnotation(
                    AnnotationSpec.Companion.builder(Component::class)
                        .member(MODULES_PARAM, modules)
                        .member(DEPENDENCIES_PARAM, dependencies)
                        .build()
                )
                scopes.forEach {
                    addAnnotation(it.annotationClass!!.className())
                }

                defaultBuilder {
                    modules.forEach { defaultBuilderMember(it) }
                    dependencies.forEach { defaultBuilderMember(it) }
                }

                companion {
                    reflectCreator(
                        BUILDER_FUNCTION_NAME,
                        BUILDER_REFLECT_NAME,
                        generatedComponentClassName.nestedClass(DEFAULT_BUILDER_NAME)
                    )
                }
            }
        }

    private inline fun file(block: FileSpec.Builder.() -> Unit) =
        FileSpec.builder(generatedComponentClassName.packageName, generatedComponentClassName.simpleName)
            .apply(block)
            .build()
            .apply { writeTo(outputDir) }
            .run { packageName.replace('.', File.separatorChar) + "${File.separatorChar}$name.kt" }

    private inline fun FileSpec.Builder.generatedComponent(isInterface: Boolean = false, block: TypeSpec.Builder.() -> Unit) {
        val builder =
            if (!isInterface) {
                TypeSpec.classBuilder(generatedComponentClassName)
                    .addModifiers(KModifier.ABSTRACT)
            } else {
                TypeSpec.interfaceBuilder(generatedComponentClassName)
            }

        addType(
            builder
                .addSuperinterface(componentClassName)
                .apply {
                    if (component.visibility == Visibilities.INTERNAL) {
                        addModifiers(KModifier.INTERNAL)
                    }
                }
                .apply(block)
                .build()
        )
    }

    private inline fun TypeSpec.Builder.companion(block: TypeSpec.Builder.() -> Unit) {
        addType(
            TypeSpec.companionObjectBuilder()
                .apply(block)
                .build()
        )
    }

    private fun TypeSpec.Builder.reflectCreator(name: String, creator: MemberName, resultTypeName: ClassName) {
        addFunction(
            FunSpec.builder(name)
                .returns(resultTypeName)
                .addAnnotation(JvmStatic::class.java)
                .addCode("return %M(%T::class.java)", creator, resultTypeName)
                .build()
        )
    }

    private fun AnnotationSpec.Builder.member(name: String, values: List<ClassName>): AnnotationSpec.Builder {
        if (values.isNotEmpty()) {
            addMember(
                "$name = [${values.joinToString(separator = ","){ "%T::class" }}]", *values.toTypedArray()
            )
        }
        return this
    }

    private fun TypeSpec.Builder.defaultBuilder(block: TypeSpec.Builder.() -> Unit) {
        addType(
            TypeSpec.interfaceBuilder(DEFAULT_BUILDER_NAME)
                .addFunction(
                    FunSpec.builder(DEFAULT_BUILDER_FUNCTION_NAME)
                        .returns(componentClassName)
                        .addModifiers(KModifier.ABSTRACT)
                        .build()
                )
                .apply(block)
                .build()
        )
    }

    private fun TypeSpec.Builder.defaultBuilderMember(className: ClassName): TypeSpec.Builder {
        val paramName = className.simpleName.decapitalize()
        addFunction(
            FunSpec.builder(paramName)
                .returns(generatedComponentClassName.nestedClass(DEFAULT_BUILDER_NAME))
                .addModifiers(KModifier.ABSTRACT)
                .addParameter(paramName, className)
                .build()
        )
        return this
    }

    private fun AnnotationDescriptor.classList(name: String): List<ClassName> =
        (allValueArguments[Name.identifier(name)] as ArrayValue)
            .value
            .asSequence()
            .map { (it as KClassValue).getArgumentType(component.module) }
            .mapNotNull { it.unwrap().constructor.declarationDescriptor as? ClassDescriptor }
            .map { it.className() }
            .toList()

    private val AnnotationDescriptor.isScope: Boolean
        get() = annotationClass?.annotations?.hasAnnotation(SCOPE_FQ_NAME) == true

    private fun ClassDescriptor.className(): ClassName {
        val psiClass = findPsi()
        return if (psiClass != null) {
            val containingFile = psiClass.containingFile as PsiClassOwner
            val packageName = containingFile.packageName
            val classFqName = fqNameSafe.asString()
            val simpleNames = classFqName.removePrefix("$packageName.").split(".")
            ClassName(packageName, simpleNames.first(), *simpleNames.drop(1).toTypedArray())
        } else {
            ClassName(fqNameSafe.parent().asString(), fqNameSafe.shortName().asString())
        }
    }

    companion object {
        private const val GENERATED_COMPONENT_PREFIX = "Dagger"

        private const val FACTORY_FUNCTION_NAME = "factory"
        private val FACTORY_REFLECT_NAME = Dagger::class.member("factory")

        private const val BUILDER_FUNCTION_NAME = "builder"
        private val BUILDER_REFLECT_NAME = Dagger::class.member("builder")

        private const val MODULES_PARAM = "modules"
        private const val DEPENDENCIES_PARAM = "dependencies"
        private val SCOPE_FQ_NAME = FqName(Scope::class.java.canonicalName)
        private const val DEFAULT_BUILDER_NAME = "Builder"
        private const val DEFAULT_BUILDER_FUNCTION_NAME = "build"
    }
}
