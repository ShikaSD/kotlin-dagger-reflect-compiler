import dagger.Component

@Component
interface SomeComponent15 : TestInterface {
    fun test(): Int

    @Component.Factory
    interface Factory {
        fun factory(): SomeComponent15
    }
}
