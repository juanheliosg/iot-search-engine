package v1.extractor

/**
 * Enum for the different type of extractors avalaible in the system
 * Current avalaible extractors are:
 *
 * http -> Uses http protocol to extract data in JSON format from an endpoint
 * rw_sym -> Simulate a random walk process
 */
object ExtractorType extends Enumeration {
  type ExtractorType = Value
  val Http = Value("http")
  val RANDOM_WALK_SYM = Value("rw_sym")

  /**
   * Check if string is inside the enum
   *
   * @param s
   * @return
   */
  def isExtractorType(s: String) = values.exists(_.toString == s)

  /**
   * Enables matching against all ExtractorType.values
   * source: https://stackoverflow.com/questions/3407032/comparing-string-and-enumeration
   *
   * @param s
   * @return
   */
  def unapply(s: String): Option[Value] =
    values.find(s == _.toString)

  /**
   * Trait for doing matching pattern in enum
   * source: https://stackoverflow.com/questions/3407032/comparing-string-and-enumeration
   */
  trait Matching {
    // enables matching against a particular Role.Value
    def unapply(s: String): Boolean =
      (s == toString)
  }

}
