package net.sf.podr.jod_podr;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.fileupload.httpexchange.HttpExchangeFileUpload;
import org.apache.commons.fileupload.httpexchange.HttpExchangeRequestContext;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.document.DocumentFormat;

/**
 * Converter Service
 *
 * @author Eric Letard
 * @copyright  GPL License 2009
 * @license    http://www.gnu.org/copyleft/gpl.html  GPL License
 * @version 0.2
 */
public class ConverterService implements HttpHandler {

    Logger logger = Logger.getLogger(getClass());

    /**
         * Convert the file
         * return a http 403 error if the file isn't sent in multipart.
         * @param exchange
         * @throws java.io.IOException
         */
    protected void doPost(HttpExchange exchange) throws Exception {
        if (!HttpExchangeFileUpload.isMultipartContent(exchange)) {
            exchange.sendResponseHeaders(403, 0);
            exchange.getResponseBody().write("only multipart requests are allowed".getBytes());
            exchange.getResponseBody().flush();
            exchange.getResponseBody().close();
            return;
        }
        ConverterContext webappContext = ConverterContext.getInstance();
        HttpExchangeFileUpload fileUpload = webappContext.getFileUpload();
        OfficeDocumentConverter converter = webappContext.getDocumentConverter();
        String outputExtension = FilenameUtils.getExtension(exchange.getRequestURI().toString());
        FileItem uploadedFile;
        try {
            uploadedFile = getUploadedFile(fileUpload, exchange);
        } catch (FileUploadException fileUploadException) {
            throw new IOException(fileUploadException);
        }
        if (uploadedFile == null) {
            throw new NullPointerException("uploaded file is null");
        }
        String inputExtension = FilenameUtils.getExtension(uploadedFile.getName());
        String baseName = FilenameUtils.getBaseName(uploadedFile.getName());
        File inputFile = File.createTempFile(baseName, "." + inputExtension);
        writeUploadedFile(uploadedFile, inputFile);
        File outputFile = File.createTempFile(baseName, "." + outputExtension);
        try {
            DocumentFormat outputFormat = converter.getFormatRegistry().getFormatByExtension(outputExtension);
            long startTime = System.currentTimeMillis();
            converter.convert(inputFile, outputFile);
            long conversionTime = System.currentTimeMillis() - startTime;
            logger.info(String.format("successful conversion: %s [%db] to %s in %dms", inputExtension, inputFile.length(), outputExtension, conversionTime));
            exchange.getResponseHeaders().add(FileUploadBase.CONTENT_TYPE, outputFormat.getMediaType());
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + baseName + "." + outputExtension);
            exchange.sendResponseHeaders(200, 0);
            sendFile(outputFile, exchange);
            exchange.getResponseBody().close();
        } catch (Exception exception) {
            logger.fatal(String.format("failed conversion: %s [%db] to %s; %s; input file: %s", inputExtension, inputFile.length(), outputExtension, exception, inputFile.getName()));
            throw exception;
        } finally {
            outputFile.delete();
            inputFile.delete();
        }
    }

    private void sendFile(File file, HttpExchange response) throws IOException {
        response.getResponseHeaders().add(FileUploadBase.CONTENT_LENGTH, Long.toString(file.length()));
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            IOUtils.copy(inputStream, response.getResponseBody());
        } catch (Exception exception) {
            throw new IOException("error sending file", exception);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void writeUploadedFile(FileItem uploadedFile, File destinationFile) throws IOException {
        try {
            uploadedFile.write(destinationFile);
        } catch (Exception exception) {
            throw new IOException("error writing uploaded file", exception);
        }
        uploadedFile.delete();
    }

    private FileItem getUploadedFile(HttpExchangeFileUpload fileUpload, HttpExchange request) throws FileUploadException {
        @SuppressWarnings("unchecked") List<FileItem> fileItems = fileUpload.parseRequest(request);
        for (FileItem fileItem : fileItems) {
            if (!fileItem.isFormField()) {
                return fileItem;
            }
        }
        return null;
    }

    /**
     * redirect request to doPost or doGet
     * returns an http 500 error in case of an exception
     * @param exchange request
     * @throws java.io.IOException
     */
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (exchange.getRequestMethod().intern() == "POST") doPost(exchange); else doGet(exchange);
        } catch (Exception ex) {
            logger.fatal("error during handle", ex);
            try {
                exchange.getRequestBody().close();
            } catch (Exception ex2) {
                logger.warn("Could not close request body");
            }
            exchange.getResponseHeaders().add("Exception", ex.toString());
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write(ex.toString().getBytes());
            exchange.getResponseBody().close();
        }
    }

    /**
     * Serves converter.html and documentFormats.js in web dir
     * @param exchange request
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public void doGet(HttpExchange exchange) throws FileNotFoundException, IOException {
        FileInputStream in = null;
        String file = exchange.getRequestURI().getPath().replaceAll("/converter", "").intern();
        if (file == "" || file == "/" || file == "/converter.html") {
            file = "/converter.html";
        } else {
            file = "/documentFormats.js";
        }
        file = "./web" + file;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in = new FileInputStream(file);
        while (in.available() > 0) {
            out.write(in.read());
        }
        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseBody().write(out.toByteArray());
        exchange.getResponseBody().close();
        exchange.close();
    }
}
