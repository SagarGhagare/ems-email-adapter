package net.nhs.ems.emailadapter.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Map;

public class SendHandler implements RequestHandler<Map<String, String>, String> {
  Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Override
  public String handleRequest(Map<String, String> event, Context context) {
    LambdaLogger logger = context.getLogger();
    logger.log("Env variables: " + gson.toJson(System.getenv()));
    logger.log("Event: " + gson.toJson(event));
    logger.log("Context: " + gson.toJson(context));
    return "200 OK";
  }
}
