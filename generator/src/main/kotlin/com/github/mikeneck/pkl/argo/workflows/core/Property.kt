package com.github.mikeneck.pkl.argo.workflows.core

data class Property(
    val name: String,
    val type: Type,
    val comment: Comment,
) {
    fun isRequired(): Boolean {
        val allLines = allLines()
        for (line in allLines) {
            when {
                line.contains("+optional", true) -> return false
                line.contains("+required", true) -> return true
            }
        }
        return false
    }

    fun hasDefaultValue(): Boolean {
        for (line in allLines()) {
            if (defaultValuePatterns.any { it.canExtractFrom(line) }) {
                return true
            }
        }
        return false
    }

    data class ExtractorByRegex(val regex: Regex): DefaultValueExtractor {

        //language regexp
        constructor(pattern: String) : this(Regex(pattern))

        override fun canExtractFrom(value: String): Boolean = regex.matches(value)


        override fun extractFrom(value: String): String? = when (val values = extract(value)) {
            null -> null
            else -> values.drop(1).find { it.isNotEmpty() } ?: ""
        }

        private fun extract(value: String): List<String>? = regex.find(value)?.groupValues
    }

    companion object {

        val defaultValueIsPattern = ExtractorByRegex("""^.*Defaults to (?:['"]([^'"]*)['"]|(\S*[\S&&[^\s\.]]))[.\s,]""")
        val defaultsToValuePattern = ExtractorByRegex("""^.*Defaults to (?:['"]([^'"]*)['"]|(\S*\S&&[^\s.]))[.\s,]""")
        private val defaultValuePatterns = listOf(
            //language=regexp
            defaultValueIsPattern,
            //language=regexp
            defaultsToValuePattern,
            object : DefaultValueExtractor {
                override fun canExtractFrom(value: String): Boolean =
                    value.startsWith("\\+kubebuilder:") && value.contains("default=")

                override fun extractFrom(value: String): String? {
                    val found = value.split(':').find {
                        it.startsWith("default=")
                    }
                    if (found == null) {
                        return null
                    }
                    return found.replace("default=", "")
                }
            }
        )
    }

    fun getDefaultValue(): String? {
        for (line in allLines()) {
            for (extractor in defaultValuePatterns) {
                val value = extractor.extractFrom(line)
                if (value != null) return value
            }
            val extractor = defaultValuePatterns.find { it.canExtractFrom(line) }
            if (extractor == null) {
                continue
            }
            val value = extractor.extractFrom(line)
            return value
        }
        return null
    }

    private fun allLines(): List<String> {
        val titleLines = comment.titleLines(name)
        val descriptionLines = comment.descriptionLines()
        val allLines = listOf(titleLines, descriptionLines).flatten()
        return allLines
    }

    interface DefaultValueExtractor {
        fun canExtractFrom(value: String): Boolean
        fun extractFrom(value: String): String?
    }
}