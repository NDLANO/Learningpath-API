package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.ValidationMessage
import no.ndla.learningpathapi.model.StepType


object StepTypeValidator {
  def validate(stepType: String): Option[ValidationMessage] = {
    StepType.valueOf(stepType).isEmpty match {
      case true => Some(ValidationMessage("type", s"'$stepType' is not a valid steptype."))
      case false => None
    }
  }
}
