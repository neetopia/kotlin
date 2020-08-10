// TEST PROCESSOR: PathIdentifierProcessor
// EXPECTED:
// test/pack/Outer
// test/pack/Outer.Val
// test/pack/Outer.Foo
// test/pack/Outer.Inner
// test/pack/Outer.Inner.innerVal
// test/pack/Outer.Inner.innerFoo
// <no name>
// test/pack/Outer.Nested
// test/pack/Outer.Nested.nestedVal
// test/pack/Outer.Nested.nestedFoo
// <no name>
// END
// a.kt
package test.pack

class Outer {
    val Val

    fun Foo() {}

    inner class Inner {
        val innerVal: Int
        fun innerFoo() {
            class InnerLocal
        }
    }
    class Nested {
        private val nestedVal: Int
        fun nestedFoo() {
            val a = 1
        }
    }
}