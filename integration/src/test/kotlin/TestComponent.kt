import dagger.Component

@Component
interface TestComponent {

    fun test(): Int

    @Component.Factory
    interface Fctory {
        fun factory(): TestComponent
    }
}
