name := """mutuel_poste"""
organization := "nigerposte.ne"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, LauncherJarPlugin)


scalaVersion := "2.13.11"


libraryDependencies += guice

libraryDependencies ++= Seq(
	javaJdbc,
	javaWs,
	"net.sf.jasperreports" % "jasperreports" % "6.5.1"  withSources()
    ,"net.sf.barcode4j" % "barcode4j" % "2.1"
    ,"org.apache.xmlgraphics" % "batik-bridge" % "1.9.1"
)

// https://mvnrepository.com/artifact/org.jooq/jooq
libraryDependencies += "org.jooq" % "jooq" % "3.10.1"

// https://mvnrepository.com/artifact/org.jooq/jooq-meta 
libraryDependencies += "org.jooq" % "jooq-meta" % "3.10.1"

// https://mvnrepository.com/artifact/org.jooq/jooq-codegen 
libraryDependencies += "org.jooq" % "jooq-codegen" % "3.10.1"

import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys
EclipseKeys.skipParents in ThisBuild := false


// https://mvnrepository.com/artifact/com.google.zxing/javase
libraryDependencies += "com.google.zxing" % "javase" % "3.3.1"

// https://mvnrepository.com/artifact/com.google.zxing/core
libraryDependencies += "com.google.zxing" % "core" % "3.3.1"

libraryDependencies += "com.itextpdf" % "itextpdf" % "5.4.2"
libraryDependencies += "com.itextpdf.tool" % "xmlworker" % "5.4.1"
//jasper barcode
libraryDependencies += "net.sourceforge.barbecue" % "barbecue" % "1.5-beta1"
//resolvers += "Jasper3rd" at "http://jaspersoft.artifactoryonline.com/jaspersoft/third-party-ce-artifacts/"

//resolvers += ("Jasper3rd" at "http://jaspersoft.artifactoryonline.com/jaspersoft/third-party-ce-artifacts/").withAllowInsecureProtocol(true)
resolvers += ("Jasper3rd" at "https://jaspersoft.jfrog.io/artifactory/third-party-ce-artifacts/").withAllowInsecureProtocol(true)
