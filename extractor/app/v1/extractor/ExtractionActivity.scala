package v1.extractor

import akka.actor.typed.ActorRef
import akka.stream.KillSwitch
import v1.extractor.actors.{Extractor, PlayActorConfig}
import v1.extractor.models.extractor.ExtractorState

/**
 * Trait representing all possibles extractor functions
 *
 */
trait ExtractionActivity {
  /**
   * Create an Akka Stream for doint ETL tasks described by ExtractorState
   *
   * @param entityID guardian entity id who is supervising the stream
   * @param extData extractor data including metadata, config and mapping schema
   * @param supervisor reference to actor supervisor
   * @param playConfig play actor and typesafe config
   * @return Returns a killSwitch for terminating the stream when required
   */
  def extraction(entityID: String,extData: ExtractorState, supervisor: ActorRef[Extractor.Command], playConfig :PlayActorConfig): KillSwitch
}
