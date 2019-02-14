resolvers := Seq(DefaultMavenRepository, Resolver.jcenterRepo, Resolver.sonatypeRepo("releases"))

addSbtPlugin("com.codacy" % "codacy-sbt-plugin" % "13.0.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
