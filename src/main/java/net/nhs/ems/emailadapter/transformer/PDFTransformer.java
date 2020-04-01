package net.nhs.ems.emailadapter.transformer;

import com.amazonaws.util.StringInputStream;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.source.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Converts HTML into a PDF
 */
public class PDFTransformer {

  public byte[] transform(String html) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    HtmlConverter.convertToPdf(new StringInputStream(html), outputStream);

    return outputStream.toByteArray();
  }
}
