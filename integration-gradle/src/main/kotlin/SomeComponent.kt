import dagger.Component

@Component
interface SomeComponent : TestInterface {

    fun test(): Int

    @Component.Factory
    interface Factory {
        fun factory(): SomeComponent
    }
}
