package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import dev.bartuzen.qbitcontroller.model.Category

class CategoryDeserializer : JsonDeserializer<Category>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Category {
        val json = parser.readValueAsTree<JsonNode>()

        return Category(
            name = json["name"].textValue(),
            savePath = json["savePath"].textValue(),
            downloadPath = if (json["download_path"] == null) {
                Category.DownloadPath.Default
            } else if (json["download_path"].isTextual) {
                Category.DownloadPath.Yes(json["download_path"].textValue())
            } else {
                Category.DownloadPath.No
            }
        )
    }
}
