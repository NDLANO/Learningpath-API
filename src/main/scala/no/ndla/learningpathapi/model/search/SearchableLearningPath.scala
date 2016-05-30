package no.ndla.learningpathapi.model.search

import java.util.Date

case class SearchableLearningPath(id: Long,
                                  titles: SearchableTitles,
                                  descriptions: SearchableDescriptions,
                                  coverPhotoUrl: Option[String],
                                  duration: Option[Int],
                                  status: String,
                                  verificationStatus: String,
                                  lastUpdated: Date,
                                  tags: SearchableTags,
                                  author: String,
                                  learningsteps: List[SearchableLearningStep])

