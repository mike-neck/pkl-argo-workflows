package com.github.mikeneck.pkl.argo.workflows.pkl

object PklReserved {
  @JvmStatic
  fun wrapIfReservedWords(name: String): String {
    return if (name in items) "`${name}`" else name
  }

  private val items: Set<String> =
      setOf(
          "abstract",
          "amends",
          "as",
          "case",
          "class",
          "const",
          "delete",
          "else",
          "extends",
          "external",
          "false",
          "fixed",
          "for",
          "function",
          "hidden",
          "if",
          "import",
          "in",
          "is",
          "let",
          "local",
          "module",
          "new",
          "nothing",
          "null",
          "open",
          "out",
          "outer",
          "override",
          "protected",
          "read",
          "record",
          "super",
          "switch",
          "this",
          "throw",
          "trace",
          "true",
          "typealias",
          "unknown",
          "vararg",
          "when",
      )
}
