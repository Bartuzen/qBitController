package dev.bartuzen.qbitcontroller.model

data class Plugin(
    val isEnabled: Boolean,
    val fullName: String,
    val name: String,
    val supportedCategories: List<Category>,
    val url: String,
    val version: String
) {
    data class Category(
        val id: String,
        val name: String
    )
}
