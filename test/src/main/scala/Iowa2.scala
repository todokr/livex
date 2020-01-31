class Iowa2 {

  // livex: 数値をいい感じに処理する
  def double(n: Int): Int = n * 2
  // livexend

  // livex: 数値をいい感じに処理する
  def method2(n: Int): Int = {
    val hashCode = n.toString.hashCode
    n * hashCode
  }
  // livexend

  // livex:  数値をいい感じに処理する
  def method3(n: Int): Int = {
    // this is just a comment
    n * 3
  }
  // livexend


  // livex:  数値をいい感じに処理する
  def method4(n: Int): Int = {
    // this is just a comment
    // this is also just a comment
    n * 4
  }
  // livexend
}
