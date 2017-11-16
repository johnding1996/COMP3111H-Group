package controller;

import com.google.common.io.ByteStreams;
import com.linecorp.bot.client.MessageContentResponse;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import utility.FormatterMessageJSON;

@Slf4j
@Component
public class ImageControl {

    private static File file;

    @Value
    public static class DownloadedContent {
        Path path;
        String uri;
    }

    public static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now().toString() + '-' 
                        + UUID.randomUUID().toString() + '.' + ext;
        Path tempFilePath= DietingChatbotApplication.downloadedContentDir.resolve(fileName);
        file = tempFilePath.toFile();
        file.deleteOnExit();
        return new DownloadedContent(tempFilePath, createUri("/downloaded/" + tempFilePath.getFileName()));
    }

    public static String saveContent(MessageContentResponse responseBody, String type) {
        log.info("Got content-type: {}", responseBody);
        InputStream inputStream = responseBody.getStream();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            String encodingMethod = inputStreamReader.getEncoding();
            String mimeType = responseBody.getMimeType();
            String extension = mimeType.substring(6);   // image/jpeg or image/png
            log.info("extension: {}", extension);
            log.info("Encoding method: {}", encodingMethod);
            if(type.equals("TempFile")) {
                return inputToTempFile(extension, inputStream);
                // return the uri of the downloaded image
            }
            else if(type.equals("DB")) {
                // ByteArrayOutputStream bos = new ByteArrayOutputStream();
                // try {
                //     long numOfCopiesInBytes = ByteStreams.copy(responseBody.getStream(), bos);
                //     log.info("copied " + numOfCopiesInBytes + " bytes");
                //     String decodedContent = new String(bos.toString(StandardCharsets.UTF_8.name()));
                //     log.info("************  decodedContent = " + decodedContent.substring(0, 100));
                //     // store encodedContent to DB
                    
                //     inputStream = new ByteArrayInputStream(decodedContent.getBytes(StandardCharsets.UTF_8.name()));
                //     String tempFileUri = inputToTempFile(extension, inputStream);
                //     log.info("prepare to get tempFileUri");
                //     return tempFileUri;
                //     // DownloadedContent tempFile = createTempFile(extension);
                //     // try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
                //     //     bos.writeTo(outputStream); 
                //     //     log.info("Saved {}: {}", extension, tempFile);
                //     //     return tempFile.getUri();
                //     // } catch (IOException e) {
                //     //     throw new UncheckedIOException(e);
                //     // }
                    
                // }
                // catch (IOException e) {
                //     log.info("Caught IOException when testing DB part");
                // }
                
                StringBuilder contents = new StringBuilder();
                int len = inputStreamReader.read();
                char[] buffer = new char[500000];
                while (len >= 0) {
                    contents.append(buffer, 0, len);
                }
                String decodedContent = buffer.toString();


                DownloadedContent tempFile = createTempFile(extension);
                
                OutputStream outputStream = Files.newOutputStream(tempFile.path); 
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
                outputStreamWriter.write(decodedContent);
                log.info("Saved {}: {}", extension, tempFile);
                return tempFile.getUri();
            }    
                
        } catch (Exception e) {
            log.info("Do not support this kind of edcoding");
        }
    }
        
        
    
    static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).build().toString();
    }

    public static void addBorder(String fileName) {
        // BufferedImage bimg;
		// try {
		// 	bimg = ImageIO.read(file);
		// } catch (IOException e) {
        //     log.info("cannot read in file");
		// }
        // int width          = bimg.getWidth();
        // int height         = bimg.getHeight();

    }

    public String sendCoupon(String userId, String... info) {
        String encodedContent = "";
        String tempFileUri = "";
        Boolean test = (info.length > 1);
        if(test) {
            encodedContent = "some method here to get the string from DB";
            encodedContent = info[1];
            try {
                InputStream inputStream = new ByteArrayInputStream(encodedContent.getBytes(StandardCharsets.UTF_8.name()));
                tempFileUri = inputToTempFile("png", inputStream);
            } catch (UnsupportedEncodingException e) {
                log.info("Encounter UnsupportedEncodingException when decoding encodedContent from DB");
            }

        } else {
            String pathName = info[0];
            // currently for testing
            InputStream inputStream = getClass().getResourceAsStream(pathName);
            // "/static/sample_menu.txt"
            tempFileUri = inputToTempFile("png", inputStream);
        }
        return tempFileUri;
    }

    public static String inputToTempFile(String extension, InputStream inputStream) {
        DownloadedContent tempFile = createTempFile(extension);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ByteStreams.copy(inputStream, outputStream);
            log.info("Saved {}: {}", extension, tempFile);
            return tempFile.getUri();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}