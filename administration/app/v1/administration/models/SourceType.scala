package v1.administration.models

/**
 * Tag-like field for user source classification
 * An example of use is for a source representing heavy machinery name could be: Machines
 *
 * @param id
 * @param name        name of the tag
 * @param description description of the tag
 */
case class SourceType(id: Long, name: String, description: Option[String])
