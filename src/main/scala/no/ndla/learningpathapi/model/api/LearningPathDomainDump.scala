/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.model.domain.LearningPath
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about learningpaths")
case class LearningPathDomainDump(@(ApiModelProperty@field)(description = "The total number of learningpaths in the database") totalCount: Long,
                             @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                             @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                             @(ApiModelProperty@field)(description = "The search results") results: Seq[LearningPath])
