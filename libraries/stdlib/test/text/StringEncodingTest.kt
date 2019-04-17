/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package text

import test.assertArrayContentEquals
import test.executeIfNotOnJvm6And7
import test.surrogateCodePointDecoding
import kotlin.test.*

class StringEncodingTest {
    private fun bytes(vararg elements: Int) = ByteArray(elements.size) { elements[it].toByte() }

    private fun testEncoding(isMalformed: Boolean, expected: ByteArray, string: String) {
        assertArrayContentEquals(expected, string.toByteArray())
        if (isMalformed) {
            assertFailsWith<CharacterCodingException> { string.toByteArray(throwOnInvalidSequence = true) }
        } else {
            assertArrayContentEquals(expected, string.toByteArray(throwOnInvalidSequence = true))
            assertEquals(string, stringFrom(string.toByteArray(throwOnInvalidSequence = true)))
        }
    }

    private fun testEncoding(isMalformed: Boolean, expected: ByteArray, string: String, startIndex: Int, endIndex: Int) {
        assertArrayContentEquals(expected, string.toByteArray(startIndex, endIndex))
        if (isMalformed) {
            assertFailsWith<CharacterCodingException> { string.toByteArray(startIndex, endIndex, true) }
        } else {
            assertArrayContentEquals(expected, string.toByteArray(startIndex, endIndex, true))
            assertEquals(
                string.substring(startIndex, endIndex),
                stringFrom(string.toByteArray(startIndex, endIndex, true))
            )
        }
    }

    @Test
    fun toByteArray() {
        // empty string
        testEncoding(false, bytes(), "")

        // 1-byte chars
        testEncoding(false, bytes(0), "\u0000")
        testEncoding(false, bytes(0x2D), "-")
        testEncoding(false, bytes(0x7F), "\u007F")

        // 2-byte chars
        testEncoding(false, bytes(0xC2, 0x80), "\u0080")
        testEncoding(false, bytes(0xC2, 0xBF), "¿")
        testEncoding(false, bytes(0xDF, 0xBF), "\u07FF")

        // 3-byte chars
        testEncoding(false, bytes(0xE0, 0xA0, 0x80), "\u0800")
        testEncoding(false, bytes(0xE6, 0x96, 0xA4), "斤")
        testEncoding(false, bytes(0xED, 0x9F, 0xBF), "\uD7FF")

        // surrogate chars
        testEncoding(true, bytes(0x3F), "\uD800")
        testEncoding(true, bytes(0x3F), "\uDB6A")
        testEncoding(true, bytes(0x3F), "\uDFFF")

        // 3-byte chars
        testEncoding(false, bytes(0xEE, 0x80, 0x80), "\uE000")
        testEncoding(false, bytes(0xEF, 0x98, 0xBC), "\uF63C")
        testEncoding(false, bytes(0xEF, 0xBF, 0xBF), "\uFFFF")

        // 4-byte surrogate pairs
        testEncoding(false, bytes(0xF0, 0x90, 0x80, 0x80), "\uD800\uDC00")
        testEncoding(false, bytes(0xF2, 0xA2, 0x97, 0xBC), "\uDA49\uDDFC")
        testEncoding(false, bytes(0xF4, 0x8F, 0xBF, 0xBF), "\uDBFF\uDFFF")

        // reversed surrogate pairs
        testEncoding(true, bytes(0x3F, 0x3F), "\uDC00\uD800")
        testEncoding(true, bytes(0x3F, 0x3F), "\uDDFC\uDA49")
        testEncoding(true, bytes(0x3F, 0x3F), "\uDFFF\uDBFF")

        testEncoding(
            true,
            bytes(
                0, /**/ 0x2D, /**/ 0x7F, /**/ 0xC2, 0x80, /**/ 0xC2, 0xBF, /**/ 0xDF, 0xBF, /**/ 0xE0, 0xA0, 0x80, /**/
                0xE6, 0x96, 0xA4, /**/ 0xED, 0x9F, 0xBF, /**/ 0x7A, /**/ 0x3F, /**/ 0x3F, /**/ 0x7A, /**/ 0x3F, /**/ 0x7A, /**/ 0x3F
            ),
            "\u0000-\u007F\u0080¿\u07FF\u0800斤\uD7FFz\uDFFF\uD800z\uDB6Az\uDB6A"
        )

        testEncoding(
            false,
            bytes(
                0xEE, 0x80, 0x80, /**/ 0xEF, 0x98, 0xBC, /**/ 0xC2, 0xBF, /**/ 0xEF, 0xBF, 0xBF, /**/
                0xF0, 0x90, 0x80, 0x80, /**/ 0xF2, 0xA2, 0x97, 0xBC, /**/ 0xF4, 0x8F, 0xBF, 0xBF
            ),
            "\uE000\uF63C¿\uFFFF\uD800\uDC00\uDA49\uDDFC\uDBFF\uDFFF"
        )

        val longChars = CharArray(200_000) { 'k' }
        val longBytes = stringFrom(longChars).toByteArray()
        assertEquals(200_000, longBytes.size)
        assertTrue { longBytes.all { it == 0x6B.toByte() } }
    }

    @Test
    fun toByteArraySlice() {
        assertFailsWith<IllegalArgumentException> { "".toByteArray(startIndex = 1) }
        assertFailsWith<IllegalArgumentException> { "123".toByteArray(startIndex = 10) }
        assertFailsWith<IndexOutOfBoundsException> { "123".toByteArray(startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { "123".toByteArray(endIndex = 10) }
        assertFailsWith<IllegalArgumentException> { "123".toByteArray(endIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { "123".toByteArray(startIndex = 5, endIndex = 10) }
        assertFailsWith<IllegalArgumentException> { "123".toByteArray(startIndex = 5, endIndex = 2) }
        assertFailsWith<IndexOutOfBoundsException> { "123".toByteArray(startIndex = 1, endIndex = 4) }

        testEncoding(false, bytes(), "abc", 0, 0)
        testEncoding(false, bytes(), "abc", 3, 3)
        testEncoding(false, bytes(0x62, 0x63), "abc", 1, 3)
        testEncoding(false, bytes(0x61, 0x62), "abc", 0, 2)
        testEncoding(false, bytes(0x62), "abc", 1, 2)

        testEncoding(false, bytes(0x2D), "-", 0, 1)
        testEncoding(false, bytes(0xC2, 0xBF), "¿", 0, 1)
        testEncoding(false, bytes(0xE6, 0x96, 0xA4), "斤", 0, 1)

        testEncoding(true, bytes(0x3F), "\uDB6A", 0, 1)

        testEncoding(false, bytes(0xEF, 0x98, 0xBC), "\uF63C", 0, 1)

        testEncoding(false, bytes(0xF2, 0xA2, 0x97, 0xBC), "\uDA49\uDDFC", 0, 2)
        testEncoding(true, bytes(0x3F), "\uDA49\uDDFC", 0, 1)
        testEncoding(true, bytes(0x3F), "\uDA49\uDDFC", 1, 2)

        testEncoding(
            true,
            bytes(0xE6, 0x96, 0xA4, /**/ 0xED, 0x9F, 0xBF, /**/ 0x7A, /**/ 0x3F, /**/ 0x3F),
            "\u0000-\u007F\u0080¿\u07FF\u0800斤\uD7FFz\uDFFF\uD800z\uDB6Az\uDB6A",
            startIndex = 7,
            endIndex = 12
        )

        testEncoding(
            true,
            bytes(0xC2, 0xBF, /**/ 0xEF, 0xBF, 0xBF, /**/ 0xF0, 0x90, 0x80, 0x80, /**/ 0xF2, 0xA2, 0x97, 0xBC, /**/ 0x3F),
            "\uE000\uF63C¿\uFFFF\uD800\uDC00\uDA49\uDDFC\uDBFF\uDFFF",
            startIndex = 2,
            endIndex = 9
        )

        val longChars = CharArray(200_000) { 'k' }
        val longBytes = stringFrom(longChars).toByteArray(startIndex = 5000, endIndex = 195_000)
        assertEquals(190_000, longBytes.size)
        assertTrue { longBytes.all { it == 0x6B.toByte() } }
    }

    private fun testDecoding(isMalformed: Boolean, expected: String, bytes: ByteArray) {
        assertEquals(expected, stringFrom(bytes))
        if (isMalformed) {
            assertFailsWith<CharacterCodingException> { stringFrom(bytes, throwOnInvalidSequence = true) }
        } else {
            assertEquals(expected, stringFrom(bytes, throwOnInvalidSequence = true))
            assertArrayContentEquals(bytes, stringFrom(bytes, throwOnInvalidSequence = true).toByteArray())
        }
    }

    private fun testDecoding(isMalformed: Boolean, expected: String, bytes: ByteArray, startIndex: Int, endIndex: Int) {
        assertEquals(expected, stringFrom(bytes, startIndex, endIndex))
        if (isMalformed) {
            assertFailsWith<CharacterCodingException> { stringFrom(bytes, startIndex, endIndex, true) }
        } else {
            assertEquals(expected, stringFrom(bytes, startIndex, endIndex, true))
            assertArrayContentEquals(
                bytes.sliceArray(startIndex until endIndex),
                stringFrom(bytes, startIndex, endIndex, true).toByteArray()
            )
        }
    }

    private fun truncatedSurrogateDecoding() =
        surrogateCodePointDecoding.let { if (it.length > 1) it.dropLast(1) else it }

    @Test
    fun stringFromByteArray() {
        testDecoding(false, "", bytes()) // empty
        testDecoding(false, "\u0000", bytes(0x0)) // null char
        testDecoding(false, "zC", bytes(0x7A, 0x43)) // 1-byte chars

        testDecoding(true, "��", bytes(0x85, 0xAF)) // invalid bytes starting with 1 bit
        testDecoding(false, "¿", bytes(0xC2, 0xBF)) // 2-byte char
        testDecoding(true, "�z", bytes(0xCF, 0x7A)) // 2-byte char, second byte starts with 0 bit
        testDecoding(true, "��", bytes(0xC1, 0xAA)) // 1-byte char written in two bytes

        testDecoding(true, "�z", bytes(0xEF, 0xAF, 0x7A)) // 3-byte char, third byte starts with 0 bit
        testDecoding(true, "���", bytes(0xE0, 0x9F, 0xAF)) // 2-byte char written in two bytes
        testDecoding(true, "�z", bytes(0xE0, 0xAF, 0x7A)) // 3-byte char, third byte starts with 0 bit
        testDecoding(false, "\u1FFF", bytes(0xE1, 0xBF, 0xBF)) // 3-byte char

        executeIfNotOnJvm6And7 {
            testDecoding(true, surrogateCodePointDecoding, bytes(0xED, 0xAF, 0xBF)) // 3-byte high-surrogate char
            testDecoding(true, surrogateCodePointDecoding, bytes(0xED, 0xB3, 0x9A)) // 3-byte low-surrogate char
            testDecoding(
                true,
                surrogateCodePointDecoding + surrogateCodePointDecoding,
                bytes(0xED, 0xAF, 0xBF, /**/ 0xED, 0xB3, 0x9A)
            ) // surrogate pair chars
            testDecoding(true, "�z", bytes(0xEF, 0x7A)) // 3-byte char, second byte starts with 0 bit, third byte out of bounds

            testDecoding(true, "�����", bytes(0xF9, 0x94, 0x80, 0x80, 0x80)) // 5-byte code point larger than 0x10FFFF
            testDecoding(true, "������", bytes(0xFD, 0x94, 0x80, 0x80, 0x80, 0x80)) // 6-byte code point larger than 0x10FFFF

            // Ill-Formed Sequences for Surrogates
            testDecoding(
                true,
                surrogateCodePointDecoding + surrogateCodePointDecoding + truncatedSurrogateDecoding() + "A",
                bytes(0xED, 0xA0, 0x80, /**/ 0xED, 0xBF, 0xBF, /**/ 0xED, 0xAF, /**/ 0x41)
            )
            // Truncated Sequences
            testDecoding(true, "����A", bytes(0xE1, 0x80, /**/ 0xE2, /**/ 0xF0, 0x91, 0x92, /**/ 0xF1, 0xBF, /**/ 0x41))
        }

        testDecoding(true, "�", bytes(0xE0, 0xAF)) // 3-byte char, third byte out of bounds

        testDecoding(false, "\uD83D\uDFDF", bytes(0xF0, 0x9F, 0x9F, 0x9F)) // 4-byte char
        testDecoding(true, "����", bytes(0xF0, 0x8F, 0x9F, 0x9F)) // 3-byte char written in four bytes
        testDecoding(true, "����", bytes(0xF4, 0x9F, 0x9F, 0x9F)) // 4-byte code point larger than 0x10FFFF
        testDecoding(true, "����", bytes(0xF5, 0x80, 0x80, 0x80)) // 4-byte code point larger than 0x10FFFF

        // Non-Shortest Form Sequences
        testDecoding(true, "��������A", bytes(0xC0, 0xAF, /**/ 0xE0, 0x80, 0xBF, /**/ 0xF0, 0x81, 0x82, /**/ 0x41))
        // Other Ill-Formed Sequences
        testDecoding(true, "�����A��B", bytes(0xF4, 0x91, 0x92, 0x93, /**/ 0xFF, /**/ 0x41, /**/ 0x80, 0xBF, /**/ 0x42))

        val longBytes = ByteArray(200_000) { 0x6B.toByte() }
        val longString = stringFrom(longBytes)
        assertEquals(200_000, longString.length)
        assertTrue { longString.all { it == 'k' } }
    }

    @Test
    fun stringFromByteArraySlice() {
        assertFailsWith<IllegalArgumentException> { stringFrom(bytes(), 1, 0) }
        assertFailsWith<IllegalArgumentException> { stringFrom(bytes(0x61, 0x62, 0x63), startIndex = 10) }
        assertFailsWith<IndexOutOfBoundsException> { stringFrom(bytes(0x61, 0x62, 0x63), startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { stringFrom(bytes(0x61, 0x62, 0x63), endIndex = 10) }
        assertFailsWith<IllegalArgumentException> { stringFrom(bytes(0x61, 0x62, 0x63), endIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { stringFrom(bytes(0x61, 0x62, 0x63), startIndex = 5, endIndex = 10) }
        assertFailsWith<IllegalArgumentException> { stringFrom(bytes(0x61, 0x62, 0x63), startIndex = 5, endIndex = 2) }
        assertFailsWith<IndexOutOfBoundsException> { stringFrom(bytes(0x61, 0x62, 0x63), startIndex = 1, endIndex = 4) }

        testDecoding(false, "", bytes(), startIndex = 0, endIndex = 0)
        testDecoding(false, "", bytes(0x61, 0x62, 0x63), startIndex = 0, endIndex = 0)
        testDecoding(false, "", bytes(0x61, 0x62, 0x63), startIndex = 3, endIndex = 3)
        testDecoding(false, "abc", bytes(0x61, 0x62, 0x63), startIndex = 0, endIndex = 3)
        testDecoding(false, "ab", bytes(0x61, 0x62, 0x63), startIndex = 0, endIndex = 2)
        testDecoding(false, "bc", bytes(0x61, 0x62, 0x63), startIndex = 1, endIndex = 3)
        testDecoding(false, "b", bytes(0x61, 0x62, 0x63), startIndex = 1, endIndex = 2)

        testDecoding(false, "¿", bytes(0xC2, 0xBF), startIndex = 0, endIndex = 2)
        testDecoding(true, "�", bytes(0xC2, 0xBF), startIndex = 0, endIndex = 1)
        testDecoding(true, "�", bytes(0xC2, 0xBF), startIndex = 1, endIndex = 2)

        testDecoding(true, "�", bytes(0xEF, 0xAF, 0x7A), startIndex = 0, endIndex = 2)
        testDecoding(true, "�z", bytes(0xEF, 0xAF, 0x7A), startIndex = 1, endIndex = 3)
        testDecoding(false, "z", bytes(0xEF, 0xAF, 0x7A), startIndex = 2, endIndex = 3)

        executeIfNotOnJvm6And7 {
            testDecoding(true, surrogateCodePointDecoding, bytes(0xED, 0xAF, 0xBF), startIndex = 0, endIndex = 3)
            testDecoding(true, truncatedSurrogateDecoding(), bytes(0xED, 0xB3, 0x9A), startIndex = 0, endIndex = 2)
            testDecoding(true, "���", bytes(0xED, 0xAF, 0xBF, 0xED, 0xB3, 0x9A), startIndex = 1, endIndex = 4)
            testDecoding(true, "�", bytes(0xEF, 0x7A), startIndex = 0, endIndex = 1)
            testDecoding(false, "z", bytes(0xEF, 0x7A), startIndex = 1, endIndex = 2)
        }

        testDecoding(false, "\uD83D\uDFDF", bytes(0xF0, 0x9F, 0x9F, 0x9F), startIndex = 0, endIndex = 4)
        testDecoding(true, "��", bytes(0xF0, 0x9F, 0x9F, 0x9F), startIndex = 2, endIndex = 4)
        testDecoding(true, "��", bytes(0xF0, 0x9F, 0x9F, 0x9F), startIndex = 1, endIndex = 3)

        val longBytes = ByteArray(200_000) { 0x6B.toByte() }
        val longString = stringFrom(longBytes, startIndex = 5000, endIndex = 195_000)
        assertEquals(190_000, longString.length)
        assertTrue { longString.all { it == 'k' } }
    }

    @Test
    fun kotlinxIOUnicodeTest() {
        fun String.readHex(): ByteArray = split(" ")
            .filter { it.isNotBlank() }
            .map { it.toInt(16).toByte() }
            .toByteArray()

        val smokeTestData = "\ud83c\udf00"
        val smokeTestDataCharArray: CharArray = smokeTestData.toList().toCharArray()
        val smokeTestDataAsBytes = "f0 9f 8c 80".readHex()

        val testData = "file content with unicode " +
                "\ud83c\udf00 :" +
                " \u0437\u0434\u043e\u0440\u043e\u0432\u0430\u0442\u044c\u0441\u044f :" +
                " \uc5ec\ubcf4\uc138\uc694 :" +
                " \u4f60\u597d :" +
                " \u00f1\u00e7"
        val testDataCharArray: CharArray = testData.toList().toCharArray()
        val testDataAsBytes: ByteArray = ("66 69 6c 65 20 63 6f 6e 74 65 6e 74 20 77 69 74 " +
                " 68 20 75 6e 69 63 6f 64 65 20 f0 9f 8c 80 20 3a 20 d0 b7 d0 b4 d0 be d1 " +
                "80 d0 be d0 b2 d0 b0 d1 82 d1 8c d1 81 d1 8f 20 3a 20 ec 97 ac eb b3 b4 ec " +
                " 84 b8 ec 9a 94 20 3a 20 e4 bd a0 e5 a5 bd 20 3a 20 c3 b1 c3 a7").readHex()


        assertArrayContentEquals(smokeTestDataAsBytes, smokeTestData.toByteArray())
        assertArrayContentEquals(testDataAsBytes, testData.toByteArray())

        assertEquals(smokeTestData, stringFrom(smokeTestDataAsBytes))
        assertEquals(testData, stringFrom(testDataAsBytes))

        assertEquals(smokeTestData, stringFrom(smokeTestDataCharArray))
        assertEquals(testData, stringFrom(testDataCharArray))

        assertArrayContentEquals(smokeTestDataCharArray, smokeTestData.toCharArray())
        assertArrayContentEquals(testDataCharArray, testData.toCharArray())

        assertArrayContentEquals(smokeTestDataAsBytes, stringFrom(smokeTestDataCharArray).toByteArray())
        assertArrayContentEquals(testDataAsBytes, stringFrom(testDataCharArray).toByteArray())

        assertArrayContentEquals(smokeTestDataCharArray, stringFrom(smokeTestDataAsBytes).toCharArray())
        assertArrayContentEquals(testDataCharArray, stringFrom(testDataAsBytes).toCharArray())

        assertEquals("\uD858\uDE18\n", stringFrom(byteArrayOf(0xF0.toByte(), 0xA6.toByte(), 0x88.toByte(), 0x98.toByte(), 0x0a)))
        assertEquals("\u0BF5\n", stringFrom(byteArrayOf(0xe0.toByte(), 0xaf.toByte(), 0xb5.toByte(), 0x0a)))
        assertEquals("\u041a\n", stringFrom(byteArrayOf(0xd0.toByte(), 0x9a.toByte(), 0x0a)))
    }
}
