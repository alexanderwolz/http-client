package de.alexanderwolz.http.client.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(val id:String, val name:String)