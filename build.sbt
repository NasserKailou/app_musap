import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys

name := "mutuel_poste"
organization := "nigerposte.ne"
version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava, LauncherJarPlugin)

scalaVersion := "2.13.11"

// Configuration Eclipse
EclipseKeys.skipParents in ThisBuild := false

// Dépendances
libraryDependencies ++= Seq(
  // Play
  guice,
  javaJdbc,
  javaWs,

  // JasperReports + codes barres
  "net.sf.jasperreports"    % "jasperreports" % "6.5.1" withSources(),
  "net.sf.barcode4j"        % "barcode4j"    % "2.1",
  "org.apache.xmlgraphics"  % "batik-bridge" % "1.9.1",

  // jOOQ
  "org.jooq" % "jooq"         % "3.10.1",
  "org.jooq" % "jooq-meta"    % "3.10.1",
  "org.jooq" % "jooq-codegen" % "3.10.1",

  // ZXing (QRCode / barcodes)
  "com.google.zxing" % "javase" % "3.3.1",
  "com.google.zxing" % "core"   % "3.3.1",

  // iText pour PDF / HTML -> PDF
  "com.itextpdf"      % "itextpdf" % "5.4.2",
  "com.itextpdf.tool" % "xmlworker" % "5.4.1",

  // Jasper barcode
  "net.sourceforge.barbecue" % "barbecue" % "1.5-beta1",

  // Apache POI pour Excel (import/export)
  "org.apache.poi"      % "poi"                % "4.1.2",
  "org.apache.poi"      % "poi-ooxml"          % "4.1.2",
  "org.apache.poi"      % "poi-ooxml-schemas"  % "4.1.2",
  "org.apache.xmlbeans" % "xmlbeans"           % "3.1.0"
)

// On force les versions de POI (et xmlbeans) même pour les dépendances transitives (ex: JasperReports)
dependencyOverrides ++= Seq(
  "org.apache.poi"      % "poi"                % "4.1.2",
  "org.apache.poi"      % "poi-ooxml"          % "4.1.2",
  "org.apache.poi"      % "poi-ooxml-schemas"  % "4.1.2",
  "org.apache.xmlbeans" % "xmlbeans"           % "3.1.0"
)

// Dépôt Jasper pour certaines dépendances tierces
resolvers += ("Jasper3rd" at "https://jaspersoft.jfrog.io/artifactory/third-party-ce-artifacts/")
  .withAllowInsecureProtocol(true)
