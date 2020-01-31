package io.github.todokr.livex

import java.time.LocalDateTime

case class ExampleCollection(
    projectName: String,
    lastGenerated: LocalDateTime,
    exampleCategories: Seq[ExampleCategory]
)

case class ExampleCategory(
    categoryId: String,
    categoryName: String,
    description: Option[String],
    examples: Seq[CodeExample]
)

case class CodeExample(filePath: String, lines: Seq[String])
