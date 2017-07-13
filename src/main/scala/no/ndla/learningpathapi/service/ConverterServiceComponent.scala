/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.api.{CoverPhoto, LearningStepSummaryV2, Title}
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.LearningpathApiProperties.{Domain, InternalImageApiUrl}
import no.ndla.learningpathapi.model.domain.{EmbedType, LanguageField}
import no.ndla.learningpathapi.model.domain.Language._
import no.ndla.network.ApplicationUrl
import no.ndla.mapping.License.getLicense
import com.netaporter.uri.dsl._

trait ConverterServiceComponent {
  this: ImageApiClientComponent =>

  val converterService: ConverterService

  class ConverterService {
    def asEmbedUrl(embedUrl: api.EmbedUrl): domain.EmbedUrl = {
      domain.EmbedUrl(embedUrl.url, embedUrl.language, EmbedType.valueOfOrError(embedUrl.embedType))
    }

    def asDescription(description: api.Description): domain.Description = {
      domain.Description(description.description, description.language)
    }

    def asTitle(title: api.Title): domain.Title = {
      domain.Title(title.title, title.language)
    }

    def asLearningPathTags(tags: api.LearningPathTags): domain.LearningPathTags = {
      domain.LearningPathTags(tags.tags, tags.language)
    }

    def asApiLearningPathTags(tags: domain.LearningPathTags): api.LearningPathTags = {
      api.LearningPathTags(tags.tags, tags.language)
    }

    def asApiCopyright(copyright: domain.Copyright): api.Copyright = {
      api.Copyright(asApiLicense(copyright.license), copyright.contributors.map(asApiAuthor))
    }

    def asApiLicense(license: String): api.License =
      getLicense(license) match {
        case Some(l) => api.License(l.license, Option(l.description), l.url)
        case None => api.License(license, Some("Invalid license"), None)
      }

    def asApiAuthor(author: domain.Author): api.Author = {
      api.Author(author.`type`, author.name)
    }

    def asAuthor(user: domain.NdlaUserName): api.Author = {
      val names = Array(user.first_name, user.middle_name, user.last_name).filter(_.isDefined).map(_.get)
      api.Author("Forfatter", names.mkString(" "))
    }

    def asCoverPhoto(imageId: String): Option[CoverPhoto] = {
      imageApiClient.imageMetaOnUrl(createUrlToImageApi(imageId))
        .map(imageMeta => {
          val imageUrl = s"$Domain${imageMeta.imageUrl.path}"
          val metaUrl = s"$Domain${imageMeta.metaUrl.path}"
          api.CoverPhoto(imageUrl, metaUrl)
        })
    }

    def asCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(copyright.license.license, copyright.contributors.map(asAuthor))
    }

    def asAuthor(author: api.Author): domain.Author = {
      domain.Author(author.`type`, author.name)
    }

    def asApiLearningpath(lp: domain.LearningPath, user: Option[String]): api.LearningPath = {
      api.LearningPath(lp.id.get,
        lp.revision.get,
        lp.isBasedOn,
        lp.title.map(asApiTitle),
        lp.description.map(asApiDescription),
        createUrlToLearningPath(lp),
        lp.learningsteps.map(ls => asApiLearningStepSummary(ls, lp)).toList.sortBy(_.seqNo),
        createUrlToLearningSteps(lp),
        lp.coverPhotoId.flatMap(asCoverPhoto),
        lp.duration,
        lp.status.toString,
        lp.verificationStatus.toString,
        lp.lastUpdated,
        lp.tags.map(asApiLearningPathTags),
        asApiCopyright(lp.copyright),
        lp.canEdit(user))
    }

    def asApiLearningpathV2(lp: domain.LearningPath, language: String, user: Option[String]): Option[api.LearningPathV2] = {
      val supportedLanguages = findSupportedLanguages(lp)
      // val searchLanguage = if (language == Language.AllLanguages) Language.DefaultLanguage else language

      if (language != AllLanguages && !supportedLanguages.contains(language)) {
        return None
      }

      val title =         setByLanguage(lp.title, language)
      val description =   setByLanguage(lp.description, language)
      val learningSteps = lp.learningsteps.flatMap(ls => asApiLearningStepSummaryV2(ls, lp, language)).toList.sortBy(_.seqNo)
      val tags =          setByLanguage(lp.tags, language)

      if (title.isDefined || description.isDefined || learningSteps.nonEmpty || tags.isDefined) {
        val titleString = title.getOrElse("")
        val descriptionString = description.getOrElse("")
        val learningStepsSeq = if (learningSteps.nonEmpty) learningSteps else Seq.empty[LearningStepSummaryV2]
        val tagsSeq = tags.getOrElse(Seq.empty[String])

        Some(api.LearningPathV2(
          lp.id.get,
          lp.revision.get,
          lp.isBasedOn,
          titleString,
          if (language == AllLanguages) "" else language,
          descriptionString,
          createUrlToLearningPath(lp),
          learningStepsSeq,
          createUrlToLearningSteps(lp),
          lp.coverPhotoId.flatMap(asCoverPhoto),
          lp.duration,
          lp.status.toString,
          lp.verificationStatus.toString,
          lp.lastUpdated,
          tagsSeq,
          asApiCopyright(lp.copyright),
          lp.canEdit(user),
          supportedLanguages
        ))
      } else {
        None
      }
    }

    def asApiIntroduction(introStepOpt: Option[domain.LearningStep]): Seq[api.Introduction] = {
      introStepOpt match {
        case None => List()
        case Some(introStep) => introStep.description.map(desc => api.Introduction(desc.description, desc.language))
      }
    }

    def asApiLearningpathSummary(learningpath: domain.LearningPath): api.LearningPathSummary = {
      api.LearningPathSummary(learningpath.id.get,
        learningpath.title.map(asApiTitle),
        learningpath.description.map(asApiDescription),
        asApiIntroduction(learningpath.learningsteps.find(_.`type` == domain.StepType.INTRODUCTION)),
        createUrlToLearningPath(learningpath),
        learningpath.coverPhotoId.flatMap(asCoverPhoto).map(_.url),
        learningpath.duration,
        learningpath.status.toString,
        learningpath.lastUpdated,
        learningpath.tags.map(asApiLearningPathTags),
        asApiCopyright(learningpath.copyright),
        learningpath.isBasedOn)
    }

    def asApiLearningStep(ls: domain.LearningStep, lp: domain.LearningPath, user: Option[String]): api.LearningStep = {
      api.LearningStep(
        ls.id.get,
        ls.revision.get,
        ls.seqNo,
        ls.title.map(asApiTitle),
        ls.description.map(asApiDescription),
        ls.embedUrl.map(asApiEmbedUrl),
        ls.showTitle,
        ls.`type`.toString,
        ls.license.map(asApiLicense),
        createUrlToLearningStep(ls, lp),
        lp.canEdit(user),
        ls.status.toString)
    }

    def asApiLearningStepV2(ls: domain.LearningStep, lp: domain.LearningPath, language: String, user: Option[String]): Option[api.LearningStepV2] = {
      val supportedLanguages = findSupportedLanguages(ls)
      //val searchLanguage = if (language == Language.AllLanguages) Language.DefaultLanguage else language

      if (language != AllLanguages && !supportedLanguages.contains(language)) {
        return None
      }

      val title = setByLanguage(ls.title, language)
      val description = setByLanguage(ls.description, language)
      val embedUrl = (
        if (language == AllLanguages) {
          findByLanguage(ls.embedUrl, DefaultLanguage) match {
            case Some(e) => Some(e)
            case None => ls.embedUrl.headOption.map(lf => lf.value)
          }
        }
        else {
          findByLanguage(ls.embedUrl, language)
        }).asInstanceOf[Option[domain.EmbedUrl]]
        .map(asApiEmbedUrlV2)

      if (title.isDefined || description.isDefined || embedUrl.isDefined) {
        val titleString = title.getOrElse("")
        val descriptionString = description.getOrElse("")
        val embedUrlObj = embedUrl.getOrElse(api.EmbedUrlV2("", ""))

        Some(api.LearningStepV2(
          ls.id.get,
          ls.revision.get,
          ls.seqNo,
          titleString,
          if (language == AllLanguages) "" else language,
          descriptionString,
          embedUrlObj,
          ls.showTitle,
          ls.`type`.toString,
          ls.license.map(asApiLicense),
          createUrlToLearningStep(ls, lp),
          lp.canEdit(user),
          ls.status.toString,
          supportedLanguages
        ))
      } else {
        None
      }
    }

    def asApiLearningStepSummary(ls: domain.LearningStep, lp: domain.LearningPath): api.LearningStepSummary = {
      api.LearningStepSummary(
        ls.id.get,
        ls.seqNo,
        ls.title.map(asApiTitle),
        ls.`type`.toString,
        createUrlToLearningStep(ls, lp)
      )
    }

    def asApiLearningStepSummaryV2(ls: domain.LearningStep, lp: domain.LearningPath, language: String): Option[api.LearningStepSummaryV2] = {
      setByLanguage(ls.title, language).map(title =>
        api.LearningStepSummaryV2(
          ls.id.get,
          ls.seqNo,
          title,
          ls.`type`.toString,
          createUrlToLearningStep(ls, lp)
        )
      )
    }

    def asApiTitle(title: domain.Title): api.Title = {
      api.Title(title.title, title.language)
    }

    def asApiDescription(description: domain.Description): api.Description = {
      api.Description(description.description, description.language)
    }

    def asApiEmbedUrl(embedUrl: domain.EmbedUrl): api.EmbedUrl = {
      api.EmbedUrl(embedUrl.url, embedUrl.language, embedUrl.embedType.toString)
    }

    def asApiEmbedUrlV2(embedUrl: domain.EmbedUrl): api.EmbedUrlV2 = {
      api.EmbedUrlV2(embedUrl.url, embedUrl.embedType.toString)
    }

    def createUrlToLearningStep(ls: domain.LearningStep, lp: domain.LearningPath): String = {
      s"${createUrlToLearningSteps(lp)}/${ls.id.get}"
    }

    def createUrlToLearningSteps(lp: domain.LearningPath): String = {
      s"${createUrlToLearningPath(lp)}/learningsteps"
    }

    def createUrlToLearningPath(lp: domain.LearningPath): String = {
      s"${ApplicationUrl.get}${lp.id.get}"
    }

    def createUrlToLearningPath(lp: api.LearningPath): String = {
      s"${ApplicationUrl.get}${lp.id}"
    }

    def createUrlToImageApi(imageId: String): String = {
      s"http://$InternalImageApiUrl/$imageId"
    }
  }
}
