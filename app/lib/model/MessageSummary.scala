package lib.model

import java.time.{ZoneId, ZonedDateTime}
import javax.mail.internet.MailDateFormat

import fastparse.core.Result
import lib.Email.Addresses
import play.api.libs.json._
import play.api.mvc.Headers

object MessageSummary {

  implicit val writesAddresses = Json.writes[Addresses]
  implicit val writesMessageSummary = new Writes[MessageSummary] {
    override def writes(o: MessageSummary): JsValue = {
      val suggestion = for {
        prefix <- o.suggestedPrefixForNextPatchBombOpt
      } yield "suggestsPrefix" -> JsString(prefix)

      Json.writes[MessageSummary].writes(o).asInstanceOf[JsObject] ++ JsObject(suggestion.toSeq)
    }
  }

  def fromRawMessage(rawMessage: String, articleUrl: String): MessageSummary = {
    val Result.Success(headerTuples, _) = PatchParsing.headers.parse(rawMessage)
    val headers = Headers(headerTuples: _*)
    val messageId = headers("Message-Id").stripPrefix("<").stripSuffix(">")
    val from = headers("From")
    val date = new MailDateFormat().parse(headers("Date")).toInstant.atZone(ZoneId.of("UTC"))
    MessageSummary(messageId, headers("Subject"), date, Addresses(from), articleUrl)
  }
}


case class MessageSummary(
  id: String,
  subject: String,
  date: ZonedDateTime,
  addresses: Addresses,
  groupLink: String
) {
  lazy val suggestedPrefixForNextPatchBombOpt: Option[String] =
    SubjectPrefixParsing.parse(subject).map(_.suggestsNext.toString)
}
