package no.ndla.learningpathapi.model.api

import java.util.Date

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Summary of meta information for a learningpath")
case class LearningPathSummary(
                                @(ApiModelProperty @field)(description = "The unique id of the learningpath") id:Long,
                                @(ApiModelProperty @field)(description = "The titles of the learningpath") title:List[Title],
                                @(ApiModelProperty @field)(description = "The descriptions of the learningpath") description:List[Description],
                                @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningpath can be found") metaUrl:String,
                                @(ApiModelProperty @field)(description = "Url to where a cover photo can be found") coverPhotoUrl:Option[String],
                                @(ApiModelProperty @field)(description = "The duration of the learningpath in minutes") duration:Option[Int],
                                @(ApiModelProperty @field)(description = "The publishing status of the learningpath.", allowableValues = "PUBLISHED,PRIVATE,NOT_LISTED") status:String,
                                @(ApiModelProperty @field)(description = "The date when this learningpath was last updated.") lastUpdated:Date,
                                @(ApiModelProperty @field)(description = "The author of this learningpath") author:Author
                                )