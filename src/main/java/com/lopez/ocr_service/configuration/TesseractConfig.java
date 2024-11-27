package com.lopez.ocr_service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.sourceforge.tess4j.Tesseract;

@Configuration
public class TesseractConfig {
  private static String dataPath = "/opt/homebrew/Cellar/tesseract/5.4.1_1/share/tessdata";

  @Bean
  Tesseract tesseract() {
    Tesseract tesseract = new Tesseract();
    tesseract.setDatapath(dataPath);
    return tesseract;
  }

}
