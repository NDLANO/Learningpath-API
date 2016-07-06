package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Description of copyright information")
case class Copyright(@(ApiModelProperty@field)(description = "Describes the license of the image") license: License,
                     @(ApiModelProperty@field)(description = "Reference to where the image is procured") origin: String,
                     @(ApiModelProperty@field)(description = "List of authors") contributers: Seq[Author])
