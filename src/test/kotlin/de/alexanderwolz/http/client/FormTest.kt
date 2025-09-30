package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.Form
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals

class FormTest {

    @Test
    fun testFormSerialization() {

        val map = HashMap<String, String>().apply {
            put("key1", "value1")
            put("SomeKey", "SomeValue")
            put("Niña", "Ötztal-Express")
        }

        val form = Form(map)
        val encoded = form.encodeToString()

        val form2 = Form(encoded)
        assertContentEquals(map.entries.toList(), form2.entries.toList())
    }

}