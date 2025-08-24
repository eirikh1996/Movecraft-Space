/**
 * Precompiled [buildlogic.java-conventions.gradle.kts][Buildlogic_java_conventions_gradle] script plugin.
 *
 * @see Buildlogic_java_conventions_gradle
 */
public
class Buildlogic_javaConventionsPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Buildlogic_java_conventions_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
