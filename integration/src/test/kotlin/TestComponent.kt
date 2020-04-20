import dagger.Component

@Component
interface TestComponent {

    fun test(): Int

    @Component.Factory
    interface Factoy {
        fun factory(): TestComponent
    }
}
