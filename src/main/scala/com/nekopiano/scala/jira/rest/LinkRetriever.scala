package com.nekopiano.scala.jira.rest

import java.io.File
import java.net.URI

import com.atlassian.jira.rest.client.api.domain.IssueField
import com.atlassian.jira.rest.client.api.domain.IssueLinkType.Direction
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import com.github.tototoshi.csv.CSVWriter
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.util.parsing.json.JSON

/**
  * Created on 2015/12/02.
  */
object LinkRetriever {

  // 1000 is the limit of the result of JIRA
  // up to 1000 items even if you specify more.
  val MAX_RESULTS = 1000

  def main(args: Array[String]) {

    val startTime = DateTime.now

    val restClientFactory = new AsynchronousJiraRestClientFactory()
    // http://host:port/context/rest/api-name/api-version/resource-name

    import com.typesafe.config.ConfigFactory
    val conf = ConfigFactory.load("account.conf")

    //val JIRA_BASE_URL = "http://[team name].atlassian.net/plugins/servlet/oauth/request-token"
    val jiraBaseURL = conf.getString("baseurl")

    // JIRA_BASE_URL + "/plugins/servlet/oauth/request-token"
    val uri = new URI(jiraBaseURL)

    val username = conf.getString("username")
    val password = conf.getString("password")
    val client = restClientFactory.createWithBasicHttpAuthentication(uri, username, password)


    val project = conf.getString("project")
    val epicQuery = s"project=$project AND issuetype=epic"
    val epicPromise = client.getSearchClient().searchJql(epicQuery, MAX_RESULTS, null, null)
    val epic2StoryMapping = epicPromise.claim().getIssues.asScala.map(epic => {
      val epicName = epic.getFieldByName("Epic Name").getValue
      val epic2StoryQuery = s"""project=$project AND "Epic Link" = "$epicName""""
      println(epic2StoryQuery)
      val epic2StoryPromise = client.getSearchClient().searchJql(epic2StoryQuery, MAX_RESULTS, null, null)
      val stories = epic2StoryPromise.claim().getIssues.asScala.toIndexedSeq
      println("Epic: " + epic.getKey + " Stories:" + stories.map(_.getKey).mkString(","))
      epic -> stories
    })

    println("epic2StoryMapping.size=" + epic2StoryMapping.size)

    val wholeStories = epic2StoryMapping.map(epic2Stories => {
      val epic = epic2Stories._1
      println("Epic (" + epic.getFieldByName("Epic Name") + ") started to retrieve.")
      val stories = epic2Stories._2.map(story => {
          val linkedStories = story.getIssueLinks.asScala.map(link => {
            val linkType = link.getIssueLinkType
            if (linkType.getDirection == Direction.INBOUND && linkType.getName == "Blocks" ) {
              Option(client.getIssueClient.getIssue(link.getTargetIssueKey).claim())
            } else {None}
          }).toIndexedSeq
         story -> linkedStories
      })
      epic -> stories
    }).toIndexedSeq

    val lines = wholeStories.map(epic2Stories=>{
      val epic = epic2Stories._1
      epic2Stories._2.map(story2Substories => {
        val story = story2Substories._1
        story2Substories._2.map(optionalSubstory =>{
          optionalSubstory match {
            case Some(substory) => List(substory.getKey, "3", epic.getKey, extractFieldValue(epic.getFieldByName("Epic Name")), epic.getSummary, story.getKey, story.getSummary, extractFieldValue(story.getFieldByName("Story Points")), substory.getKey, substory.getSummary, extractFieldValue(substory.getFieldByName("Story Points")), extractFieldJsonValue(substory.getFieldByName("Custom Field A")), extractFieldJsonValue(substory.getFieldByName("Custom Field B")), extractFieldJsonValue(substory.getFieldByName("Custom Field C")))
            case None => List(story.getKey, "2", epic.getKey, extractFieldValue(epic.getFieldByName("Epic Name")), epic.getSummary, story.getKey, story.getSummary, extractFieldValue(story.getFieldByName("Story Points")))
          }
        })
      }) flatten
    }) flatten

    println("lines.size=" + lines.size)

    val filteredLines = lines.filter(line =>{
      if(line(2) == "2") {
        lines.find(anotherline => {anotherline(0) == line(0)}).isEmpty
      } else {
        true
      }
    })
    println("filteredLines.size=" + filteredLines.size)

    val csvFile = new File("substories.csv")
    val writer = CSVWriter.open(csvFile)

    val header = List("primary key","hierarchy level","epic.getKey", """epic.getFieldByName("Epic Name")""", "epic.getSummary", "story.getKey", "story.getSummary", """story.getFieldByName("Story Points")""", "substory.getKey", "substory.getSummary", """substory.getFieldByName("Story Points")""", """substory.getFieldByName("Custom Field A")""", """substory.getFieldByName("Custom Field B")""", """substory.getFieldByName("Custom Field C")""")
    writer.writeAll(header :: filteredLines.toList)
    writer.close()

    // Done
    println("Finished. elapsed time = " + (DateTime.now.getMillis - startTime.getMillis) + " ms")
    System.exit(0)

  }

  def extractFieldValue(field:IssueField) = {
    Option(field) match {
      case Some(field) => {
        Option(field.getValue) match {
          case Some(value) => value.toString
          case None => ""
        }
      }
      case None => ""
    }
  }

  def extractFieldJsonValue(field:IssueField) = {
    Option(field) match {
      case Some(field) => {
        Option(field.getValue) match {
          case Some(value) => extractJsonValue(value.toString)
          case None => ""
        }
      }
      case None => ""
    }
  }

  def extractJsonValue(jsonString :String) = {
    JSON.parseFull(jsonString).get.asInstanceOf[Map[String, String]]("value")
  }


}
