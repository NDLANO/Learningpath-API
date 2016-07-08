package no.ndla.learningpathapi.validation

import com.netaporter.uri.Uri._
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain._
import no.ndla.mapping.LicenseMapping.getLicenseDefinition


class LearningPathValidator(titleRequired: Boolean = true, descriptionRequired: Boolean = true) {

  val MISSING_DESCRIPTION = "At least one description is required."
  val INVALID_COVER_PHOTO = "The url to the coverPhoto must point to an image in NDLA Image API."

  val languageValidator = new LanguageValidator
  val noHtmlTextValidator = new TextValidator(allowHtml = false)
  val titleValidator = new TitleValidator(titleRequired)
  val durationValidator = new DurationValidator

  def validate(newLearningPath: LearningPath): Seq[ValidationMessage] = {
    titleValidator.validate(newLearningPath.title) ++
      validateDescription(newLearningPath.description) ++
      validateDuration(newLearningPath.duration).toList ++
      validateCoverPhoto(newLearningPath.coverPhotoMetaUrl).toList ++
      validateTags(newLearningPath.tags) ++
      validateCopyright(newLearningPath.copyright)
  }

  def validateDescription(descriptions: Seq[Description]): Seq[ValidationMessage] = {
    (descriptionRequired, descriptions.isEmpty) match {
      case (false, true) => List()
      case (true, true) => List(ValidationMessage("description", MISSING_DESCRIPTION))
      case (_, false) => descriptions.flatMap(description => {
        noHtmlTextValidator.validate("description.description", description.description).toList :::
          languageValidator.validate("description.language", description.language).toList
      })
    }
  }

  def validateDuration(durationOpt: Option[Int]): Option[ValidationMessage] = {
    durationOpt match {
      case None => None
      case Some(duration) => durationValidator.validateRequired(durationOpt)
    }
  }

  def validateCoverPhoto(coverPhotoMetaUrl: Option[String]): Option[ValidationMessage] = {
    coverPhotoMetaUrl.flatMap(url => {
      val possibleImageApiDomains = "api.ndla.no" :: LearningpathApiProperties.Domains.toList
      val parsedUrl = parse(url)
      val host = parsedUrl.host.getOrElse("")

      val hostCorrect = possibleImageApiDomains.contains(host)
      val pathCorrect = parsedUrl.path.startsWith("/images")

      hostCorrect && pathCorrect match {
        case true => None
        case false => Some(ValidationMessage("coverPhotoMetaUrl", INVALID_COVER_PHOTO))
      }
    })
  }

  def validateTags(tags: Seq[LearningPathTags]): Seq[ValidationMessage] = {
    tags.flatMap(tagList => {
      tagList.tags.flatMap(noHtmlTextValidator.validate("tags.tags", _)).toList :::
      languageValidator.validate("tags.language", tagList.language).toList
    })
  }

  def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
    val licenseMessage = validateLicense(copyright.license)
    val originMessage = noHtmlTextValidator.validate("copyright.origin", copyright.origin)
    val contributorsMessages = copyright.contributors.flatMap(validateAuthor)

    licenseMessage ++ originMessage ++ contributorsMessages
  }

  def validateLicense(license: License): Seq[ValidationMessage] = {
    getLicenseDefinition(license.license) match {
      case Some((description, url)) => {
        val descriptionMessage = license.description == description match {
          case false => Seq(new ValidationMessage("license.description", s"${license.description} is not a valid license descrition"))
          case true => Seq()
        }

        val urlMessage = license.url == url match {
          case false => Seq(new ValidationMessage("license.url", s"${license.url} is not a valid license url"))
          case true => Seq()
        }
        descriptionMessage ++ urlMessage
      }
      case None => Seq(new ValidationMessage("license.license", s"${license.license} is not a valid license"))
    }
  }

  def validateAuthor(author: Author): Seq[ValidationMessage] = {
    noHtmlTextValidator.validate("author.type", author.`type`).toList ++
      noHtmlTextValidator.validate("author.name", author.name).toList
  }
}
