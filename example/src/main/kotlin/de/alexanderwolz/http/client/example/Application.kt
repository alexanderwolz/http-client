package de.alexanderwolz.http.client.example

import de.alexanderwolz.http.client.HttpClient


class Application() {

    fun start() {
        val client = HttpClient()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application().start()
        }
    }

}