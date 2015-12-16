name := "ScalaJIRA"

version := "1.0"

scalaVersion := "2.11.7"


libraryDependencies += "org.apache.poi" % "poi" % "3.13"
libraryDependencies += "org.apache.poi" % "poi-ooxml" % "3.13"

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.2.2"


//libraryDependencies += "com.atlassian.jira" % "jira-rest-java-client" % "2.0.0-m2"
libraryDependencies += "com.atlassian.jira" % "jira-rest-java-client-api" % "3.0.0"
libraryDependencies += "com.atlassian.jira" % "jira-rest-java-client-core" % "3.0.0"

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.6.0"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"


//resolvers += "atlassian-public" at "https://m2proxy.atlassian.com/repository/public"
resolvers += "spring-milestones" at "http://repo.spring.io/libs-milestone/"
