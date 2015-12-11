package com.nekopiano.scala.jira.rest

import java.net.URI

import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import org.joda.time.DateTime
import org.joda.time.format._

import scala.collection.JavaConverters._

/**
  * Created by lamusique on 2015/12/02.
  */
object Auth {

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

    // Invoke the JRJC Client
    val promise = client.getUserClient().getUser("admin")
    val user = promise.claim()


        client.getProjectClient().getAllProjects().claim().iterator().asScala.map(project => {
      println(project.getKey() + ": " + project.getName())
    })


    val dateTime = DateTime.now()
    //val dateString = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(dateTime)
    val dateString = DateTimeFormat.forPattern("yyyy-MM-dd").print(dateTime)
    println("dateString="+dateString)
    // up to 1000 items even if you specify more.
    val maxResults = 10
    val searchJqlPromise = client.getSearchClient().searchJql(s"due=$dateString ORDER BY assignee", maxResults, null, null)


    searchJqlPromise.claim().getIssues.asScala.map(issue => {
      println(issue.getKey + ": " + issue.getSummary)
    })

    // Print the result
    println(String.format("Your admin user's email address is: %s\r\n", user.getEmailAddress()));

    // Done
    println("Example complete. Now exiting.");
    System.exit(0);

  }

}
