package net.nhs.ems.emailadapter.transformer;

import net.nhs.ems.emailadapter.model.EncounterReport;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Converts an {@link EncounterReport} into an HTML report document
 */
public class HTMLReportTransformer {

  private final TemplateEngine templateEngine;

  public HTMLReportTransformer() {
    templateEngine = new TemplateEngine();
    templateEngine.addTemplateResolver(new ClassLoaderTemplateResolver());
  }

  public String transform(EncounterReport encounterReport) {
    Context context = new Context();
    context.setVariable("report", encounterReport);
    return templateEngine.process("/templates/report.html", context);
  }
}
