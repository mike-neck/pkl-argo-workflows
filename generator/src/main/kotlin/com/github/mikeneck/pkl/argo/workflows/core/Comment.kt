package com.github.mikeneck.pkl.argo.workflows.core

data class Comment (
    val title: String?,
    val description: String?,
) {
    @Deprecated("use titleLines() or descriptionLines() instead.")
    fun isEmpty(): Boolean = title == null && description == null
    @Deprecated("use titleLines() or descriptionLines() instead.")
    fun isNotEmpty(): Boolean = !isEmpty()

    fun makeLines(defaultTitle: String): List<String> {
        val titleLines = titleLines(defaultTitle)
        val descriptionLines = descriptionLines()
        return when {
            descriptionLines.isEmpty() -> titleLines
            else -> titleLines + listOf("") + descriptionLines
        }
    }

    fun titleLines(defaultTitle: String): List<String> = if (title == null) listOf(defaultTitle) else title.lines()
    fun descriptionLines(): List<String> = if (description == null) emptyList() else description.lines()
}