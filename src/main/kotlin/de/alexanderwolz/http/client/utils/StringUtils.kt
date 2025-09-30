package de.alexanderwolz.http.client.utils

import de.alexanderwolz.http.client.model.payload.*
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object StringUtils {

    fun resolveVars(input: String, variables: Map<String, String> = System.getenv()): String {
        val p = Pattern.compile("\\$\\{(\\w+)}|\\$(\\w+)")
        val m = p.matcher(input)
        val sb = StringBuilder()
        while (m.find()) {
            val envVarName = if (null == m.group(1)) m.group(2) else m.group(1)
            val envVarValue = variables[envVarName]
            m.appendReplacement(sb, Matcher.quoteReplacement(envVarValue ?: "\${$envVarValue}"))
        }
        m.appendTail(sb)
        return sb.toString()
    }

    fun containsAll(text: String, keywords: String): Boolean {
        keywords.split(",").forEach { keyword ->
            if (!text.contains(keyword)) {
                return false
            }
        }
        return true
    }

    fun capitalize(string: String): String {
        return string.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    fun getBodyString(payload: Payload?): String {

        if (payload == null) {
            return "No body"
        }

        val builder = StringBuilder()
        //TODO parse element?
        builder.append(payload.bytes.decodeToString())
        return builder.toString()
    }

}