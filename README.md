# Kotlin compiler plugin for dagger-reflect

Adds kapt-like code generation of dagger-reflect code with a compiler plugin.

Instructions to get the latest version [here](https://plugins.gradle.org/plugin/me.shika.dagger-reflect-compiler-plugin).

You also have to add this to **each module it is applied on**:
```groovy
buildscript {
    repositories {
        maven { url "https://plugins.gradle.org/m2/" }
    }
}
```


