package com.lopez.ocr_service.configuration;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessRequest.Builder;

@Configuration
public class DoumentAiConfig {
  String projectId = "ocr-project-441001";
  String location = "us";
  String processorId = "9b9f43915826b4f";
  String processorVersion = "db4acee3d1069f8";
  String endpoint = String.format("%s-documentai.googleapis.com:443", location);

  DocumentProcessorServiceClient documentProcessor() throws IOException {
    DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder().setEndpoint(endpoint)
        .build();
    return DocumentProcessorServiceClient.create(settings);
  }

}
