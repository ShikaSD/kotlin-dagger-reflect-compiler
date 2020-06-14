import dagger.Component

@Component
interface SomeComponent23 : TestInterface {
    fun test(): Int

    @Component.Factory
    interface Factory {
        fun factory(): SomeComponent23
    }
}
