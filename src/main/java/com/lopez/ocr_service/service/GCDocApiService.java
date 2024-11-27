package com.lopez.ocr_service.service;

import org.apache.commons.io.IOExceptionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.Document.Entity;
import com.google.cloud.documentai.v1.Document.TextAnchor;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessRequest.Builder;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

@Service
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GCDocApiService {
  static Logger logger = LoggerFactory.getLogger(GCDocApiService.class);

  public static ResponseEntity<Object> readDocument(MultipartFile file)
      throws IOException {

    String projectId = "ocr-project-441001";
    String location = "us";
    String processorId = "9b9f43915826b4f";
    String processorVersion = "db4acee3d1069f8";
    String endpoint = String.format("%s-documentai.googleapis.com:443", location);
    String targetFile = "parsed.txt";

    DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder().setEndpoint(endpoint)
        .build();

    try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create(settings)) {

      String name = String.format("projects/%s/locations/%s/processors/%s/processorVersions/%s", projectId,
          location, processorId, processorVersion);

      // Read the file.
      byte[] imageFileData = file.getBytes();

      // Convert the image data to a Buffer and base64 encode it.
      ByteString content = ByteString.copyFrom(imageFileData);

      RawDocument document = RawDocument.newBuilder().setContent(content).setMimeType("application/pdf").build();

      // Configure the process request.
      ProcessRequest request = ProcessRequest.newBuilder().setName(name).setRawDocument(document).build();

      // Recognizes text entities in the PDF document
      ProcessResponse result = client.processDocument(request);
      Document documentResponse = result.getDocument();

      File myObjFile = new File(targetFile);
      FileWriter writer = new FileWriter(myObjFile);

      StringBuilder formText = new StringBuilder();
      String text = documentResponse.getText();

      for (Document.Entity entity : documentResponse.getEntitiesList()) {
        // Fields detected. For a full list of fields for each processor see
        // the processor documentation:
        // https://cloud.google.com/document-ai/docs/processors-list
        String entityType = entity.getType();
        // some other value formats in addition to text are availible
        // e.g. dates: `entity.getNormalizedValue().getDateValue().getYear()`
        // check for normilized value with `entity.hasNormalizedValue()`
        String entityTextValue = getText(entity.getTextAnchor(), text);

        if (entityTextValue == null) {
          formText.append("Group: " + entityType + "\n");
          for (Document.Entity subEntity : entity.getPropertiesList()) {
            String subEntityType = subEntity.getType();
            String subEntityTextValue = getText(subEntity.getTextAnchor(), text);
            formText.append(
                "   >>>>>>>> " + removeNewlines(subEntityType) + ": " + removeNewlines(subEntityTextValue) + "\n");
          }
        } else {
          formText.append(
              ">>> " + removeNewlines(entityType) + ": " + removeNewlines(entityTextValue) + "\n");
        }

      }
      writer.append(formText);

      // Get all of the document text as one big string
      List<Entity> test = documentResponse.getEntitiesList();
      // Document.Page firstPage = documentResponse.getPages(0);
      // List<Document.Page.Paragraph> paragraphs = firstPage.getParagraphsList();
      for (Document.Entity entity : test) {
        String paragraphText = getText(entity.getTextAnchor(), text);
        logger.warn("Paragraph text:\n{}\n", paragraphText);
      }

      logger.warn("Files created: {}", myObjFile.getName());

      List<Document.Page> pages = documentResponse.getPagesList();
      logger.info("There are {} page(s) in this document.\n", pages.size());

      for (Document.Page page : pages) {
        logger.info("\n\n**** Page {} ****\n", page.getPageNumber());

        // List<Document.Page.Table> tables = page.getTablesList();
        // logger.info("Found {} table(s):\n", tables.size());
        // for (Document.Page.Table table : tables) {
        // writer.append(printTableInfo(table, text));
        // }

        // List<Document.Page.FormField> formFields = page.getFormFieldsList();
        // logger.info("Found {} form fields:\n", formFields.size());
        // for (Document.Page.FormField formField : formFields) {
        // String fieldName = getText(formField.getFieldName().getTextAnchor(), text);
        // String fieldValue = getText(formField.getFieldValue().getTextAnchor(), text);
        // StringBuilder formText = new StringBuilder();
        // formText.append(removeNewlines(fieldName) + removeNewlines(fieldValue));
        // writer.append(formText.toString());

        // }
      }

      // for (Document.Page.FormField field : firstPage.getFormFieldsList()) {
      // String fieldName = getText(field.getFieldName().getTextAnchor(), text);
      // String fieldValue = getText(field.getFieldValue().getTextAnchor(), text);
      // writer.append(fieldName + fieldValue);
      // }
      // for (Document.Page.Paragraph paragraph : paragraphs) {
      // String paragraphText = getText(paragraph.getLayout().getTextAnchor(), text);
      // writer.append(paragraphText);
      // }
      writer.close();
      return new ResponseEntity<>(text, HttpStatus.OK);
    } finally {
      logger.warn("SOMETHING WENT WRONG");
    }
  }

  private static String escapeNewlines(String s) {
    return s.replace("\n", "\\n").replace("\r", "\\r");
  }

  private static String printTableInfo(Document.Page.Table table, String text) {
    Document.Page.Table.TableRow firstBodyRow = table.getBodyRows(0);
    int columnCount = firstBodyRow.getCellsCount();
    logger.info("-> Table with {} columns and {} rows:\n", columnCount, table.getBodyRowsCount());

    Document.Page.Table.TableRow headerRow = table.getHeaderRows(0);
    StringBuilder tableText = new StringBuilder();
    StringBuilder headerRowText = new StringBuilder();
    for (Document.Page.Table.TableCell cell : headerRow.getCellsList()) {
      String columnName = getText(cell.getLayout().getTextAnchor(), text);
      headerRowText.append(String.format("%s | ", removeNewlines(columnName)));
      logger.info(" header row text: {}", headerRowText);
    }
    headerRowText.setLength(headerRowText.length() - 3);
    tableText.append(headerRowText.toString());

    StringBuilder firstRowText = new StringBuilder();
    for (Document.Page.Table.TableCell cell : firstBodyRow.getCellsList()) {
      String cellText = getText(cell.getLayout().getTextAnchor(), text);
      firstRowText.append(String.format("%s | ", removeNewlines(cellText)));
      logger.info("Row data: {}", firstRowText);
    }
    firstRowText.setLength(firstRowText.length() - 3);
    tableText.append(firstRowText.toString());
    return tableText.toString();
  }

  // Extract shards from the text field
  private static String getText(Document.TextAnchor textAnchor, String text) {
    if (!textAnchor.getTextSegmentsList().isEmpty()) {
      int startIdx = (int) textAnchor.getTextSegments(0).getStartIndex();
      int endIdx = (int) textAnchor.getTextSegments(0).getEndIndex();
      return text.substring(startIdx, endIdx);
    }
    return null;
  }

  private static String removeNewlines(String s) {
    return s.replace("\n", "").replace("\r", "");
  }

}