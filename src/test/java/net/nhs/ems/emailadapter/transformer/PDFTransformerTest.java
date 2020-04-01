package net.nhs.ems.emailadapter.transformer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class PDFTransformerTest {

  public static final String OUTPUT_PATH = "src/test/resources/output.pdf";

  private PDFTransformer pdfTransformer;

  @Before
  public void setup() {
    pdfTransformer = new PDFTransformer();
  }

  @Test
  public void transformEmptyReportPdf() throws IOException {
    byte[] pdfData = pdfTransformer.transform(getInputHtml());

    FileUtils.writeByteArrayToFile(new File(OUTPUT_PATH), pdfData);
  }

  private String getInputHtml() throws IOException {
    File inputFile = new File(HTMLReportTransformerTest.OUTPUT_PATH);
    if (!inputFile.exists()) {
      HTMLReportTransformerTest reportTransformerTest = new HTMLReportTransformerTest();
      reportTransformerTest.setup();
      reportTransformerTest.transformEmptyReport();
    }

    return FileUtils.readFileToString(inputFile, StandardCharsets.UTF_8);
  }
}
