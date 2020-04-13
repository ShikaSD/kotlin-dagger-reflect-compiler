import dagger.Component

@Component
interface TestComponent {

    fun test(): Int

    @Component.Factory
    interface Factory {
        fun factory(): TestComponent
    }
}
