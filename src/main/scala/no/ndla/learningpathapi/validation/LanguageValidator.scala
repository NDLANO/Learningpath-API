/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain.ValidationException
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.learningpathapi.model.domain.Language

trait LanguageValidator {
  val languageValidator : LanguageValidator

  class LanguageValidator {
    private def languageCodeSupported6391(languageCode: String, allowUnknownLanguage: Boolean): Boolean = {
      val languageCodes = get6391CodeFor6392CodeMappings.values.toSeq ++ (if (allowUnknownLanguage) Seq("unknown") else Seq.empty)
      languageCodes.contains(languageCode)
    }

    def validate(fieldPath: String, languageCode: String, allowUnknownLanguage: Boolean): Option[ValidationMessage] = {
      languageCode.nonEmpty && languageCodeSupported6391(languageCode, allowUnknownLanguage) match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
    }
  }

  object LanguageValidator {
    def validate(fieldPath: String, languageCode: String): String = {
      languageValidator.validate(fieldPath, languageCode, allowUnknownLanguage=false) match {
        case Some(validationMessage) => throw new ValidationException(errors = validationMessage :: Nil)
        case None => languageCode
      }
    }

    def checkIfLanguageIsSupported(supportedLanguages: Seq[String], language: String) = {
      if (!supportedLanguages.contains(language) && language != Language.AllLanguages) {
        val validationMessage = ValidationMessage("language", s"Language '$language' is not a supported value.")
        throw new ValidationException(errors = validationMessage :: Nil)
      }
    }
  }
}