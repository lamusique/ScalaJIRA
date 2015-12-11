package com.nekopiano.scala.jira.rest

import java.net.URI

import com.atlassian.jira.rest.client.api.domain.IssueLinkType.Direction
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import org.joda.time.DateTime
import org.joda.time.format._
import sun.awt.AWTAccessor.ComponentAccessor

import scala.collection.JavaConverters._

/**
  * Created by lamusique on 2015/12/02.
  */
object LinkRetriever {

  // 1000 is the limit of the result of JIRA
  // up to 1000 items even if you specify more.
  val MAX_RESULTS = 1000

  def main(args: Array[String]) {
    val restClientFactory = new AsynchronousJiraRestClientFactory()
    // http://host:port/context/rest/api-name/api-version/resource-name

    import com.typesafe.config.ConfigFactory
    val conf = ConfigFactory.load("account.conf")

    //val JIRA_BASE_URL = "http://teamname.atlassian.net/plugins/servlet/oauth/request-token"
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

    epic2StoryMapping.map(epic2Stories => {
      val epicInfo = "Epic Name = " + epic2Stories._1.getFieldByName("Epic Name")
      println(epicInfo)
      println("epic2Stories._2.size=" + epic2Stories._2.size)
      val blockingStories = epic2Stories._2.map(story => {
        println("    story=" + story.getKey)
          val links = story.getIssueLinks.asScala.map(link => {
            val linkType = link.getIssueLinkType
            if (linkType.getDirection == Direction.INBOUND && linkType.getName == "Blocks" ) {
              val blockingStory = client.getIssueClient.getIssue(link.getTargetIssueKey)
              story.getKey + story.getSummary + link.getIssueLinkType.toString + link.getTargetIssueKey
            } else {""}
          }).mkString(",")
        println("    links="+links)
      })
    })



    // Done
    println("Finished.");
    System.exit(0);

  }

}
