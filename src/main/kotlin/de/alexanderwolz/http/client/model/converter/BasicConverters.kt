package de.alexanderwolz.http.client.model.converter

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.OAuthTokenResponse
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.type.ContentType

object BasicConverters {

    val STRING = Converter<String>(
        { it.toByteArray() },
        { it.decodeToString() }
    )

    val JSON = Converter<JsonElement>(
        { Gson().toJson(it).toByteArray() },
        { Gson().toJsonTree(it.decodeToString()) }
    )

    val OAUTH_TOKEN = Converter<OAuthTokenResponse>(
        { Gson().toJson(it).toByteArray() },
        { Gson().fromJson(it.decodeToString(), OAuthTokenResponse::class.java) }
    )

    val FORM = Converter<Form>(
        { it.encodeToString().toByteArray() },
        { Form(it.decodeToString()) }
    )
}