package controller;

import com.google.common.io.ByteStreams;
import com.linecorp.bot.client.MessageContentResponse;

import database.keeper.CampaignKeeper;

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
import org.apache.commons.codec.binary.Base64;
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

    /**
     * Create a temporary file on the current server directory.
     * @param ext extension string, could be jpeg, jpg, png...
     * @return a DownloadedContent instance created
     */
    private static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now().toString() + '-' 
                        + UUID.randomUUID().toString() + '.' + ext;
        Path tempFilePath= DietingChatbotApplication.downloadedContentDir.resolve(fileName);
        file = tempFilePath.toFile();
        file.deleteOnExit();
        return new DownloadedContent(tempFilePath, createUri("/downloaded/" + tempFilePath.getFileName()));
    }

    /**
     * Controller use this static method to save content from the InputStream specified in the response body.
     * @param responseBody the response body get from LINE Messaging Client
     * @param type can either be TempFile (will return uri of the temp image) or DB (the extension and encoded string) 
     * @return will either return the uri or extension + encoded string          
     */
    public static String[] saveContent(MessageContentResponse responseBody, String type) {
        log.info("Got content-type: {}", responseBody);
        String mimeType = responseBody.getMimeType();
        String extension = mimeType.substring(6);   // image/jpeg or image/png
        log.info("extension: {}", extension);
        InputStream inputStream = responseBody.getStream();
        if (type.equals("TempFile")) {
            log.info("Store temporary file");
            // return the uri of the downloaded image
            return new String[] {inputToTempFile(extension, inputStream)};
        }
        else if(type.equals("DB")) {
            log.info("Store image uploaded by administrator to DB");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                long numOfBytes = ByteStreams.copy(responseBody.getStream(), bos);
                log.info("copied " + numOfBytes + " bytes");
                byte[] buf = bos.toByteArray();
                String encodedString = Base64.encodeBase64URLSafeString(buf);
                log.info("Encoded String in Base64: {}", encodedString);
                return new String[] {extension, encodedString};
            }
            catch (IOException e) {
                log.info("Caught IOException when testing DB part");
            }
        }    
        return null;
    }
        
        
    /**
     * Create URI from current context path.
     * @param path the desired sub directory path
     */
    private static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).build().toString();
    }

    /**
     * Add border to a image file to facilitate OCR recognization
     * @param fileName the name of the file
     */
    public static void addBorder(String fileName) {
        BufferedImage bimg;
		try {
			bimg = ImageIO.read(file);
		} catch (IOException e) {
            log.info("cannot read in file");
		}
        int width          = bimg.getWidth();
        int height         = bimg.getHeight();

    }

    /**
     * Store the encoded string retrieved from DB to a temp file.
     * @param userId the user to be sent
     * @param encodedString the encodedContent string retrieved from DB
     * @param extension retrieved from DB
     * @return the temp file's uri
     */ 
    public String getCouponImageUri(String userId, String encodedString, String extension) {
        byte[] decodedByteArray = Base64.decodeBase64(encodedString);
        InputStream inputStream = new ByteArrayInputStream(decodedByteArray);
        return inputToTempFile(extension, inputStream);
    }

    /**
     * Store content into a temporary file
     * @param extension the extension of the desired file format, corresponding to its MIME Type
     * @param inputStream the inputStream to get ByteStreams
     * @return the temporary file's uri
     */
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