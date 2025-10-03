# HTTP Client

![GitHub release (latest by date)](https://img.shields.io/github/v/release/alexanderwolz/http-client)
![Maven Central Version](https://img.shields.io/maven-central/v/de.alexanderwolz/http-client)
![GitHub](https://img.shields.io/github/license/alexanderwolz/http-client)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/alexanderwolz/http-client)
![GitHub all releases](https://img.shields.io/github/downloads/alexanderwolz/http-client/total?color=informational)

## 🧑‍💻 About

This repository provide a sophisticated http client wrapper.

## 🛠️ Build
1. Create jar resource using ```./gradlew clean build```
2. Copy  ```/build/libs/*.jar``` into your project
3. Use the http client classes

## 📦 Getting the latest release

You can pull the latest binaries from the central Maven repositories:

with Gradle
```kotlin
implementation("de.alexanderwolz:http-client:1.4.3")
```
with Maven
```xml
<dependency>
  <groupId>de.alexanderwolz</groupId>
  <artifactId>http-client</artifactId>
    <version>1.4.3</version>
</dependency>
```

## 🪄 Example

```kotlin
val formPayload = Payload.create(
    BasicContentTypes.FORM_URL_ENCODED,
    Form(
        "grant_type" to "client_credentials",
        "client_id" to "XYZ12345",
        "client_secret" to "9876543ABC",
        "scope" to "oidc"
    )
)

val client = HttpClient.Builder()
    .method(HttpMethod.POST)
    .endpoint("https://sso.example.com/token")
    .accept(BasicContentTypes.OAUTH_TOKEN)
    .body(formPayload)
    .build()

val response = client.execute()
if (response.isOK && response.body.type == BasicContentTypes.OAUTH_TOKEN) {
    val accessToken = response.body.element as OAuthTokenResponse
} else{ 
    throw Exception("Received error ${response.code}")
}
```

- - -

Made with ❤️ in Bavaria
<br>
© 2025, <a href="https://www.alexanderwolz.de"> Alexander Wolz
