// WITH_RUNTIME
// MIN_JAVA_VERSION: 9
// FIX: Replace with `mtableSetOf` function

fun test() {
    val a = java.util.Set.of<caret><String>()
}