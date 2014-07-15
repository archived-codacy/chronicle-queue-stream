package org.squbs.httpclient.endpoint

import org.scalatest.{BeforeAndAfterEach, Matchers, FlatSpec}
import org.squbs.httpclient.{HttpClientFactory, HttpClientException}

/**
 * Created by hakuang on 5/22/2014.
 */
class HttpClientEndpointSpec extends FlatSpec with Matchers with BeforeAndAfterEach{

  override def afterEach = {
    EndpointRegistry.endpointResolvers.clear
    HttpClientFactory.httpClientMap.clear
  }

  class LocalhostRouting extends EndpointResolver {
    override def resolve(svcName: String, env: Option[String]): Option[String] = {
      if (svcName == null && svcName.length <= 0) throw new HttpClientException(700, "Service name cannot be null")
      env match {
        case None => Some("http://localhost:8080/" + svcName)
        case Some(env) if env.toLowerCase == "dev" => Some("http://localhost:8080/" + svcName)
        case Some(env) => throw new HttpClientException(701, "LocalhostRouting cannot support " + env + " environment")
      }
    }

    override def name: String = "localhost"
  }

  "RoutingRegistry" should "contain LocalhostRouting" in {
    EndpointRegistry.register(new LocalhostRouting)
    EndpointRegistry.endpointResolvers.length should be (1)
    EndpointRegistry.endpointResolvers.head.isInstanceOf[LocalhostRouting] should be (true)
  }

  "localhost routing" should "be return to the correct value" in {
    EndpointRegistry.register(new LocalhostRouting)
    EndpointRegistry.route("abcService") should not be (None)
    EndpointRegistry.route("abcService").get.name should be ("localhost")
    EndpointRegistry.route("abcService").get.resolve("abcService") should be (Some("http://localhost:8080/abcService"))
  }

  "localhost routing" should "be throw out HttpClientException if env isn't Dev" in {
    a[HttpClientException] should be thrownBy {
      EndpointRegistry.register(new LocalhostRouting)
      EndpointRegistry.route("abcService", Some("qa"))
    }
  }

  "localhost routing" should "be return to the correct value if env is Dev" in {
    EndpointRegistry.register(new LocalhostRouting)
    EndpointRegistry.route("abcService", Some("dev")) should not be (None)
    EndpointRegistry.route("abcService", Some("dev")).get.name should be ("localhost")
    EndpointRegistry.resolve("abcService", Some("dev")) should be (Some("http://localhost:8080/abcService"))
  }

  "Latter registry RoutingDefinition" should "have high priority" in {
    EndpointRegistry.register(new LocalhostRouting)
    EndpointRegistry.register(new EndpointResolver {
      override def resolve(svcName: String, env: Option[String]): Option[String] = Some("http://localhost:8080/override")

      override def name: String = "override"
    })
    EndpointRegistry.endpointResolvers.length should be (2)
    EndpointRegistry.endpointResolvers.head.isInstanceOf[LocalhostRouting] should be (false)
    EndpointRegistry.endpointResolvers.head.asInstanceOf[EndpointResolver].name should be ("override")
    EndpointRegistry.route("abcService") should not be (None)
    EndpointRegistry.route("abcService").get.name should be ("override")
    EndpointRegistry.resolve("abcService") should be (Some("http://localhost:8080/override"))
  }

  "It" should "fallback to the previous RoutingDefinition if latter one cannot be resolve" in {
    EndpointRegistry.register(new LocalhostRouting)
    EndpointRegistry.register(new EndpointResolver {
      override def resolve(svcName: String, env: Option[String]): Option[String] = {
        svcName match {
          case "unique" => Some("http://www.ebay.com/unique")
          case _ => None
        }
      }

      override def name: String = "unique"
    })
    EndpointRegistry.endpointResolvers.length should be (2)
    EndpointRegistry.route("abcService") should not be (None)
    EndpointRegistry.route("abcService").get.name should be ("localhost")
    EndpointRegistry.route("unique") should not be (None)
    EndpointRegistry.route("unique").get.name should be ("unique")
    EndpointRegistry.resolve("abcService") should be (Some("http://localhost:8080/abcService"))
    EndpointRegistry.resolve("unique") should be (Some("http://www.ebay.com/unique"))
  }

  "unregister RoutingDefinition" should "have the correct behaviour" in {
    EndpointRegistry.register(new EndpointResolver {
      override def resolve(svcName: String, env: Option[String]): Option[String] = {
        svcName match {
          case "unique" => Some("http://www.ebay.com/unique")
          case _ => None
        }
      }

      override def name: String = "unique"
    })
    EndpointRegistry.register(new LocalhostRouting)

    EndpointRegistry.endpointResolvers.length should be (2)
    EndpointRegistry.endpointResolvers.head.isInstanceOf[LocalhostRouting] should be (true)
    EndpointRegistry.resolve("unique") should be (Some("http://localhost:8080/unique"))
    EndpointRegistry.unregister("localhost")
    EndpointRegistry.endpointResolvers.length should be (1)
    EndpointRegistry.resolve("unique") should be (Some("http://www.ebay.com/unique"))
  }
}
