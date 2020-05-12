import dagger.Component
import dagger.Module

@Component(modules = [SomeModule::class], dependencies = [TestInterface::class])
interface SomeComponent3 {
    fun test(): Long
}

@Module
class SomeModule {
    init {
        DaggerSomeComponent3.builder()
    }
}
