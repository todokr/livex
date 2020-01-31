package io.github.todokr.livex

import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.util.zip.Adler32

import com.typesafe.config.{Config, ConfigFactory}
import io.github.todokr.livex.MyVisitor.{Category, CodeFragment}
import sbt.io.IO

import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.matching.Regex

/* TODOs
 * - 除外ディレクトリをconfigで設定できるようにする
 * - cliオプションの整理
 * */

object Main extends App {

  val config = LivexConfig.load match {
    case Left(e)  => throw e
    case Right(c) => c
  }

  val (entryPoint, targetDir) = args match {
    case Array(ep, td) => (Paths.get(ep), Paths.get(td))
    case _             => throw new IllegalArgumentException(args.toString)
  }

  val blackList: Seq[Path] = config.excludeDirs.map(entryPoint.resolve)

  val visitor = new MyVisitor(blackList)

  Files.walkFileTree(entryPoint, visitor)

  val executionDir      = System.getProperty("user.dir")
  val resolvedTargetDir = Paths.get(executionDir).resolve(targetDir).normalize()
  IO.createDirectory(resolvedTargetDir.toFile)

  val assetsDir =
    IO.toFile(Thread.currentThread().getContextClassLoader.getResource("assets").toURI)
  IO.copyDirectory(assetsDir, resolvedTargetDir.resolve("assets").toFile)

  val htmlFile = resolvedTargetDir.resolve("index.html").toFile

  val parsedResult = visitor.getResult
  val exampleCategories = parsedResult.toSeq.map {
    case (c: Category, fragments: Seq[CodeFragment]) =>
      val categoryId = c.value.hashCode.toString
      val examples =
        fragments.map(fragment =>
          CodeExample(entryPoint.relativize(fragment.filePath).toString, fragment.lines))
      ExampleCategory(
        categoryId = categoryId,
        categoryName = c.value,
        description = Some("dummy description"),
        examples = examples
      )
  }
  val exampleCollection = ExampleCollection(
    projectName = "Dummy project",
    lastGenerated = LocalDateTime.now(),
    exampleCategories = exampleCategories
  )

  val htmlString = html.template(exampleCollection).body
  IO.write(htmlFile, htmlString, StandardCharsets.UTF_8)

  println(s"Created: ${resolvedTargetDir.toString}")

}

case class LivexConfig(
    excludeDirs: Seq[String]
)
object LivexConfig {
  def load: Either[Throwable, LivexConfig] =
    Try {
      val c = ConfigFactory.load("default.conf").getConfig("livex")
      LivexConfig(
        excludeDirs = c.getStringList("excludeDir").asScala.toSeq
      )
    }.toEither

}

object MyVisitor {

  val StartLinePattern: Regex = """\s*\/\/\s*livex:\s*(\S*)$""".r
  val EndLinePattern: Regex   = """\s*\/\/\s*livexend""".r

  case class Line(value: String) {
    def parse: ParsedLine = value match {
      case StartLinePattern(rawCategory) => StartLine(Category(rawCategory))
      case EndLinePattern()              => EndLine
      case _                             => PlainLine(value)
    }
  }
  sealed trait ParsedLine
  case class PlainLine(value: String)      extends ParsedLine
  case class StartLine(category: Category) extends ParsedLine
  case object EndLine                      extends ParsedLine

  case class CodeFragment(category: Category, filePath: Path, lines: Vector[String]) {
    def addLine(line: String): CodeFragment = copy(lines = lines :+ line)
  }
  object CodeFragment {
    def init(category: Category, filePath: Path): CodeFragment =
      CodeFragment(category, filePath, Vector.empty)
  }
  case class Category(value: String) extends AnyVal

  case class ParsingContext(currentCategory: Option[Category], fragments: List[CodeFragment])

  object ParsingContext {
    def zero: ParsingContext = ParsingContext(None, List.empty)
  }
}

class MyVisitor(blackList: Seq[Path]) extends SimpleFileVisitor[Path] {
  import MyVisitor._

  val checksum = new Adler32
  private val whole: collection.mutable.Map[Category, Seq[CodeFragment]] =
    collection.mutable.Map.empty

  def getResult: Map[Category, Seq[CodeFragment]] = whole.toMap

  override def preVisitDirectory(
      dir: Path,
      attrs: BasicFileAttributes
  ): FileVisitResult = {
    if (blackList.exists(dir.startsWith)) FileVisitResult.SKIP_SUBTREE
    else FileVisitResult.CONTINUE
  }

  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
    val context = ParsingContext.zero
    val lines   = Files.readAllLines(file, StandardCharsets.UTF_8).asScala
    val parsed = lines
      .map(Line(_).parse)
      .foldLeft(context) {
        case (ParsingContext(None, acc), StartLine(category)) => // Open category
          val fragments = CodeFragment.init(category, file) :: acc
          ParsingContext(Some(category), fragments)
        case (ParsingContext(category: Some[Category], current :: acc), PlainLine(line)) => // Inside category
          ParsingContext(category, current.addLine(line) :: acc)
        case (ParsingContext(_: Some[Category], acc), EndLine) => // カテゴリ終了
          ParsingContext(None, acc)
        case (ctx, _) => ctx
      }
    parsed.fragments.foreach { fragment =>
      whole.get(fragment.category) match {
        case Some(fragments) => whole.update(fragment.category, fragment +: fragments)
        case None            => whole.update(fragment.category, Seq(fragment))
      }
    }
    FileVisitResult.CONTINUE
  }
}
