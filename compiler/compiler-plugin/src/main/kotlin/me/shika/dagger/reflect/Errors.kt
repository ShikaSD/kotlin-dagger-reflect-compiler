package me.shika.dagger.reflect

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

val CLASS_SHOULD_BE_INTERFACE = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)

object DaggerReflectErrors : DefaultErrorMessages.Extension {
    private val _map = DiagnosticFactoryToRendererMap("DaggerReflectPlugin")
    override fun getMap(): DiagnosticFactoryToRendererMap = _map

    init {
        _map.put(
            CLASS_SHOULD_BE_INTERFACE,
            "Dagger reflect supports only interface components, factories, and builders"
        )
    }
}
