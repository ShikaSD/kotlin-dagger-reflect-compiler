package folder

import TestInterface
import dagger.Component
import dagger.Module

@Component(modules = [SomeModule::class], dependencies = [TestInterface::class])
interface omeComponent3 {
    fun test(): Long
}

@Module
class SomeModule {
    init {
        DaggeromeComponent3.builder()
    }
}

