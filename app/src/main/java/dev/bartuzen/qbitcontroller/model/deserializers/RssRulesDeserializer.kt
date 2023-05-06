package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bartuzen.qbitcontroller.model.RssRule

fun parseRssRules(rulesString: String): List<RssRule> {
    val mapper = jacksonObjectMapper()
    val json = mapper.readTree(rulesString)

    val rules = mutableListOf<RssRule>()
    json.fields().forEach { (name, node) ->
        val rule = RssRule(
            name = name,
            isEnabled = node["enabled"].booleanValue()
        )
        rules.add(rule)
    }

    return rules
}
