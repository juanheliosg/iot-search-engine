package v1.querier.models

class ExtendedEnum extends Enumeration {
  /**
   * Check if string is inside the enum
   *
   * @param s
   * @return
   */
  def isType(s: String) = values.exists(_.toString == s)

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