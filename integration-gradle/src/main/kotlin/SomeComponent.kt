import dagger.Component

@Component
interface SomeComponent : TestInterface {
    fun test(): Long

    @Component.Factory
    interface actory {
        fun factory(): SomeComponent
    }
}
