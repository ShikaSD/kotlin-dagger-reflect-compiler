import dagger.Component
import dagger.Module

@Component(modules = [SomeModule::class], dependencies = [TestInterface::class, some.pkg.TestInterface::class])
interface SomeComponent3 {
    fun test(): Long
}

@Module
class SomeModule
