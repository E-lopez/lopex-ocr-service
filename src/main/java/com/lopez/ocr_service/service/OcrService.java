package com.lopez.ocr_service.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.SAXException;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import javax.imageio.ImageIO;
import java.awt.image.*;
import java.awt.Rectangle;
import java.io.*;

@Service
public class OcrService {

  private Tesseract tesseract;

  public OcrService(Tesseract tesseract) {
    this.tesseract = tesseract;
  }

  Logger logger = LoggerFactory.getLogger(OcrService.class);

  public String recognizeText(java.io.InputStream inputStream) throws IOException {
    BufferedImage image = ImageIO.read(inputStream);
    try {
      return tesseract.doOCR(image);
    } catch (TesseractException e) {
      e.printStackTrace();
    }
    return "failed";
  }

  private String extractFromScanned(PDDocument document) throws IOException, TesseractException {
    // Extract images from file
    PDFRenderer pdfRenderer = new PDFRenderer(document);
    StringBuilder out = new StringBuilder();

    for (int page = 0; page < document.getNumberOfPages(); page++) {
      BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);

      // Create a temp image file
      File temp = File.createTempFile("tempfile_" + page, ".png");
      ImageIO.write(bim, "png", temp);

      String result = tesseract.doOCR(temp);

      out.append(result);

      // Delete temp file
      temp.delete();

    }

    return out.toString();

  }

  public ResponseEntity<Object> extractPdfContent(MultipartFile file) throws IOException {
    try {

      PDDocument document = PDDocument.load(file.getBytes());
      PDFTextStripper stripper = new PDFTextStripper();
      String strippedText = stripper.getText(document);

      logger.warn("Extract from pdf, {}", strippedText.trim().isEmpty());

      if (strippedText.trim().isEmpty()) {
        strippedText = extractFromScanned(document);
      }

      JSONObject obj = new JSONObject();
      obj.put("filename", file.getOriginalFilename());
      obj.put("text", strippedText);

      return new ResponseEntity<>(obj.toString(), HttpStatus.OK);

    } catch (Exception e) {

      return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

    }
  }

  public ResponseEntity<Object> extractPdfContentAlt(MultipartFile file) throws IOException {
    try {

      PDDocument document = PDDocument.load(file.getBytes());

      PDFTextStripperByArea stripper = new PDFTextStripperByArea();
      stripper.setSortByPosition(true);
      stripper.setShouldSeparateByBeads(true);

      Rectangle rect1 = new Rectangle(38, 155, 555, 11);
      Rectangle rect2 = new Rectangle(38, 167, 555, 11);
      Rectangle rect3 = new Rectangle(25, 179, 568, 11);

      stripper.addRegion("row1", rect1);
      stripper.addRegion("row2", rect2);
      stripper.addRegion("row3", rect3);
      PDPage firstPage = document.getPage(0);

      stripper.extractRegions(firstPage);
      String row1 = stripper.getTextForRegion("row1");
      String row2 = stripper.getTextForRegion("row2");
      String row3 = stripper.getTextForRegion("row3");

      logger.warn("row1 {} - row2 {} - row3 {}", row1, row2, row3);

      JSONObject obj = new JSONObject();
      obj.put("filename", file.getOriginalFilename());
      obj.put("row1", row1);
      obj.put("row3", row3);

      return new ResponseEntity<>(obj.toString(), HttpStatus.OK);

    } catch (Exception e) {

      return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

    }
  }

  public ResponseEntity<Object> extractContentUsingParser(MultipartFile file)
      throws IOException, TikaException, SAXException {

    try {
      InputStream stream = file.getInputStream();
      BodyContentHandler handler = new BodyContentHandler(-1);
      Metadata metadata = new Metadata();
      ParseContext context = new ParseContext();

      AutoDetectParser parser = new AutoDetectParser();

      parser.parse(stream, handler, metadata, context);

      return new ResponseEntity<>(handler.toString(), HttpStatus.OK);
    } catch (IOException | TikaException | SAXException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
