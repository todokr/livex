class Iowa {

  // livex: 文字列をいい感じに処理する
  def double(s: String): String = s * 2
  // livexend

  object Nested {
    // livex: 文字列をいい感じに処理する
    def method(s: String): String = {
      // this is just a comment
      // this is also just a comment
      s.toUpperCase
    }
    // livexend
  }

  // livex: ファイルパスの操作
  val basePath = Paths.get("baseDir")
  val targetFile = basePath.resolve("config.json")
  basePath.relativise(targetFile)
  // livexend
}
