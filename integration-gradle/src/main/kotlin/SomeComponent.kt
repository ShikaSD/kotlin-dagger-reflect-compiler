import dagger.Component

@Component
interface SomeComponent1 : TestInterface {
    fun test(): Long

    @Component
    interface Nested : TestInterface {
        fun test(): Long

        @Component.Factory
        interface Factory {
            fun factory(): Nested
        }
    }

    @Component.Factory
    interface Factor {
        fun factory(): SomeComponent1
    }
}
