import dagger.Component

@Component
interface SomeComponent {

    fun test(): Int

    @Component.Factory
    interface Factory {
        fun factory(): SomeComponent
    }
}
