package com.lopez.ocr_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import com.lopez.ocr_service.service.GCDocApiService;
import com.lopez.ocr_service.service.OcrService;

import java.io.IOException;

import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/ocr")
public class OcrController {
  private OcrService ocrService;

  Logger logger = LoggerFactory.getLogger(OcrController.class);

  public OcrController(OcrService ocrService) {
    this.ocrService = ocrService;
  }

  @GetMapping
  public String getResponse() {
    return "OCR IS UP";
  }

  @PostMapping
  public String recognizeText(@RequestPart MultipartFile file) throws IOException {
    logger.warn("Recognize Text");
    return ocrService.recognizeText(file.getInputStream());
  }

  @PostMapping("/extract")
  public ResponseEntity<Object> extractPdfContent(@RequestPart MultipartFile file) throws IOException {
    logger.warn("Extract from pdf");
    return ocrService.extractPdfContentAlt(file);
  }

  @PostMapping("/tika")
  public ResponseEntity<Object> extractContentUsingParser(@RequestPart MultipartFile file)
      throws IOException, TikaException, SAXException {
    logger.warn("Extract from pdf with Tika");
    return ocrService.extractContentUsingParser(file);
  }

  @PostMapping("/gcp")
  public ResponseEntity<Object> documentAi(@RequestPart MultipartFile file)
      throws IOException {
    logger.warn("Extract from pdf with Document AI");
    return GCDocApiService.readDocument(file);
  }

}
