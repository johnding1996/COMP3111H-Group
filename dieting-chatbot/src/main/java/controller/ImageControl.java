package controller;

import com.google.common.io.ByteStreams;
import com.linecorp.bot.client.MessageContentResponse;

import database.keeper.CampaignKeeper;
import java.awt.Color;
import java.awt.Graphics2D;
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

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.apache.commons.codec.binary.Base64;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import utility.FormatterMessageJSON;

/**
 * ImageControl: deal with all image matters.
 * @author agong
 * @version v2.0.0
 */
@Slf4j
@Component
public class ImageControl {

    /**
     * store path or uri for the file.
     */
    @Value
    public static class DownloadedContent {
        Path path;
        String uri;
    }

    public static ServletUriComponentsBuilder servletUriComponentsBuilder;
    
    /**
     * Create a temporary file on the current server directory.
     * @param ext extension string, could be jpeg, jpg, png...
     * @return a DownloadedContent instance created
     */
    private static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now().toString() + '-' 
                        + UUID.randomUUID().toString() + '.' + ext;
        Path tempFilePath= DietingChatbotApplication.downloadedContentDir.resolve(fileName);
        tempFilePath.toFile().deleteOnExit();
        return new DownloadedContent(tempFilePath, createUri("/downloaded/" + tempFilePath.getFileName()));
    }

    /**
     * Controller use this static method to save content from the InputStream specified in the response body.
     * @param responseBody the response body get from LINE Messaging Client
     * @param type can either be TempFile (will return uri of the temp image) or DB (the extension and encoded string) 
     * @return will either return the bordered image's uri or extension + encoded string          
     */
    public static String[] saveContent(MessageContentResponse responseBody, String type) {
        log.info("Got content-type: {}", responseBody);
        String mimeType = responseBody.getMimeType();
        String extension = mimeType.substring(6);   // image/jpeg or image/png
        log.info("extension: {}", extension);
        InputStream inputStream = responseBody.getStream();
        log.info("Input Stream: {}", inputStream);
        if (type.equals("TempFile")) {
            log.info("Store temporary file");
            DownloadedContent tempFile = createTempFile(extension);
            try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
                log.info("Trying to copy");
                ByteStreams.copy(inputStream, outputStream);
                log.info("Saved {}: {}", extension, tempFile);
                return new String[] {tempFile.getUri()};
            } catch (IOException e) {
                // throw new UncheckedIOException(e);
                return null;
            }
            // return the uri of the downloaded image
            // String fileName = LocalDateTime.now().toString() + '-' 
            //     + UUID.randomUUID().toString() + '.' + extension;
            // Path filePath= DietingChatbotApplication.downloadedContentDir.resolve(fileName);
            // filePath.toFile().deleteOnExit();
            // createUri(filePath);
            // try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            //     log.info("Trying to copy");
            //     ByteStreams.copy(inputStream, outputStream);
            //     log.info("Saved {} with name {} and path", extension, filePath.getFileName(), filePath);
            // } catch (IOException e) {
            //     throw new UncheckedIOException(e);
            // } 
            // String borderedImageUri = addBorder(filePath.toFile(), "server");
            // log.info("Added border, will return borderedImageUri as: {}", borderedImageUri);
            // return new String[] { borderedImageUri };
        } else if (type.equals("DB")) {
            log.info("Store image uploaded by administrator to DB");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                long numOfBytes = ByteStreams.copy(responseBody.getStream(), bos);
                log.info("copied " + numOfBytes + " bytes");
                byte[] buf = bos.toByteArray();
                String encodedString = Base64.encodeBase64URLSafeString(buf);
                log.info("Encoded String in Base64: {}", encodedString);
                return new String[] { extension, encodedString };
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
     * @return uri from the current context path
     */
    private static String createUri(String path) {
        return servletUriComponentsBuilder.path(path).build().toString();
    }

    /**
     * Add border to a image file to facilitate OCR recognization.
     * @param tempFile temporary file stored on server or local machine
     * @param type can either be test or server
     * @return uri of the bordered image file
     */
    public static String addBorder(File tempFile, String type) {
        String tempFileName = tempFile.getName();
        String extension = tempFileName.substring(tempFileName.lastIndexOf('.'));
        BufferedImage bimg;
		try {
			bimg = ImageIO.read(tempFile);
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            int borderedImageWidth = width + 20;
            int borderedImageHeight = height + 20;
            BufferedImage img = new BufferedImage(borderedImageWidth, borderedImageHeight, BufferedImage.TYPE_3BYTE_BGR);
            img.createGraphics();
            Graphics2D g = (Graphics2D) img.getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, borderedImageWidth, borderedImageHeight);
            g.drawImage(bimg, 10, 10, width + 10, height + 10, 0, 0, width, height, Color.BLACK, null);
            log.info("Default temporary file directory: {}", System.getProperty("java.io.tmpdir"));
            log.info("Creating bordered image...");

            // return path of this output file, if type is specified as test
            if(type.equals("test")) {
                File outputFile = File.createTempFile(tempFile.getParent() + "/bordered_menu", ".png");
                ImageIO.write(img, "png", ImageIO.createImageOutputStream(outputFile));
                return outputFile.getAbsolutePath().toString();
            }

            //create uri for file stored on server, if type is specified as server
            if(type.equals("server")) {
                String fileName = LocalDateTime.now().toString() + '-' 
                    + UUID.randomUUID().toString() + ".png";
                Path tempFilePath= DietingChatbotApplication.downloadedContentDir.resolve(fileName);
                ImageIO.write(img, "png", ImageIO.createImageOutputStream(tempFilePath.toFile()));
                log.info("Written into a new image file with file name: {}", tempFilePath.getFileName());
                tempFilePath.toFile().deleteOnExit();
                return createUri("/downloaded/" + tempFilePath.getFileName());
            }

        } catch (IOException e) {
            log.info(e.toString());
            log.info("cannot read in file {}", tempFile.getName());
        }
        return null;
    }

    /**
     * Store the encoded string retrieved from DB to a temp file.
     * @param userId the user to be sent
     * @param encodedString the encodedContent string retrieved from DB
     * @param extension retrieved from DB
     * @return the temp file's uri
     */ 
    public static String getCouponImageUri(String userId, String encodedString, String extension) {
        byte[] decodedByteArray = Base64.decodeBase64(encodedString);
        InputStream inputStream = new ByteArrayInputStream(decodedByteArray);
        return inputToTempFile(extension, inputStream);
    }

    /**
     * Store content into a temporary file.
     * @param extension the extension of the desired file format, corresponding to its MIME Type
     * @param inputStream the inputStream to get ByteStreams
     * @return the temporary file's uri
     */
    public static String inputToTempFile(String extension, InputStream inputStream) {
        DownloadedContent tempFile = createTempFile(extension);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            log.info("Trying to copy");
            ByteStreams.copy(inputStream, outputStream);
            log.info("Saved {}: {}", extension, tempFile);
            return tempFile.getUri();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}