import dagger.Component

@Component
interface SomeComponent : TestInterface {
    fun test(): Int

    @Component
    interface Nested {
        fun est(): Long

        @Component.Factory
        interface Factory {
            fun factory(): Nested
        }
    }


    @Component.Factory
    interface actory {
        fun factory(): SomeComponent
    }
}
