import dagger.Component

@Component
interface SomeComponent2 : TestInterface {
    fun test(): Int

    @Component
    interface Nested {
        fun test(): Long

        @Component.Factory
        interface Factory {
            fun factory(): Nested
        }
    }


    @Component.Factory
    interface Factory {
        fun factory(): SomeComponent2
    }
}
