import dagger.Component

@Component
interface SomeComponent : TestInterface {

    abstract fun test(): Int

    @Component.Factory
    interface Factory {
        fun factory(): SomeComponent
    }
}
