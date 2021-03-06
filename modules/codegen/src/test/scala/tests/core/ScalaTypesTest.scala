package tests.core

import com.twilio.guardrail.generators.AkkaHttp
import com.twilio.guardrail.generators.syntax.Scala.companionForStaticDefns
import com.twilio.guardrail.{ ClassDefinition, Context, ProtocolDefinitions }
import org.scalatest.{ FunSuite, Matchers }
import support.SwaggerSpecRunner

import scala.meta._

class ScalaTypesTest extends FunSuite with Matchers with SwaggerSpecRunner {

  val swagger: String = s"""
    |swagger: "2.0"
    |info:
    |  title: Whatever
    |  version: 1.0.0
    |host: localhost:1234
    |definitions:
    |  Baz:
    |    type: object
    |    properties:
    |      foo:
    |        type: string
    |        x-scala-type: com.twilio.foo.bar.Baz
    |""".stripMargin

  test("Generate no definitions") {
    val (
      ProtocolDefinitions(ClassDefinition(_, _, _, cls, staticDefns, _) :: Nil, _, _, _),
      _,
      _
    )       = runSwaggerSpec(swagger)(Context.empty, AkkaHttp)
    val cmp = companionForStaticDefns(staticDefns)

    val definition = q"""
      case class Baz(foo: Option[com.twilio.foo.bar.Baz] = None)
    """

    val companion = q"""
      object Baz {
        implicit val encodeBaz: ObjectEncoder[Baz] = {
          val readOnlyKeys = Set[String]()
          new ObjectEncoder[Baz] { final def encodeObject(a: Baz): JsonObject = JsonObject.fromIterable(Vector(("foo", a.foo.asJson))) }.mapJsonObject(_.filterKeys(key => !(readOnlyKeys contains key)))
        }
        implicit val decodeBaz: Decoder[Baz] = new Decoder[Baz] { final def apply(c: HCursor): Decoder.Result[Baz] = for (v0 <- c.downField("foo").as[Option[com.twilio.foo.bar.Baz]]) yield Baz(v0) }
      }
    """

    cls.structure shouldEqual definition.structure
    cmp.structure shouldEqual companion.structure
  }
}
