package v1.extractor

import akka.actor.typed.ActorRef
import akka.stream.KillSwitch
import v1.extractor.actors.{Extractor, PlayActorConfig}

/**
 * Trait representing all possibles extractor functions
 *
 */
trait ExtractionActivity {
  /**
   * Create an Akka Stream for doint ETL tasks described by ExtractorState
   * Returns a killSwitch for terminating the stream when required
   * @param extData
   */
  def extraction(extData: ExtractorState, supervisor: ActorRef[Extractor.Command], playConfig :PlayActorConfig): KillSwitch
}
