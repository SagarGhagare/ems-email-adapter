package net.nhs.ems.emailadapter.handler;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.nhs.ems.emailadapter.events.SESEvent;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EmailHandler {

  // TODO configure email sending address
  public static final String EMAIL_SOURCE = "EMS Email Adapter <ems@nhs.net>";

  protected Logger log = LoggerFactory.getLogger(getClass());
  protected AmazonSimpleEmailServiceAsync ses =
      AmazonSimpleEmailServiceAsyncClient.asyncBuilder().build();
  protected ObjectMapper mapper = new ObjectMapper()
      .configure(SerializationFeature.INDENT_OUTPUT, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public void handleRequest(
      InputStream input, OutputStream output, Context context) throws IOException {
    byte[] message = IOUtils.toByteArray(input);
    log.trace(new String(message, UTF_8));
    SESEvent event = mapper.readValue(message, SESEvent.class);
    String result = handleEvent(event, message, context);
    output.write(result.getBytes(UTF_8));
  }

  public abstract String handleEvent(SESEvent input, byte[] rawMessage, Context context);
}
