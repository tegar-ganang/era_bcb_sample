package org.gocha.inetools.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gocha.collection.Predicate;
import org.gocha.files.FileUtil;

/**
 Базовый класс HTTP ответа. 
 Тело ответа хранит в оперативной памяти, если оно не превышает __ байт,
 иначе тело записывается во временный файл.
 @author gocha
 */
public class BasicHttpResponse extends AbstractHttpResponse {

    private static Level defaultLogLevel = null;

    private static Level getDefaultLogLevel() {
        if (defaultLogLevel == null) defaultLogLevel = Level.FINE;
        return defaultLogLevel;
    }

    private Level logLevel = null;

    private Level getLogLevel() {
        if (logLevel == null) logLevel = getDefaultLogLevel();
        return logLevel;
    }

    private void log(String message) {
        Logger.getLogger(BasicHttpResponse.class.getName()).log(getLogLevel(), message);
    }

    /**
     * Конструктор
	 * @param request Запрос
     * @param downloadHeaders Заголовки ответа
     * @param downloadPred Предикат проверки заголовков (т.е. качать тело или не качать)
	 * @param listener Подписчик
     * @param inputStream Тело запроса
     */
    public BasicHttpResponse(HttpRequest request, Map<String, List<String>> downloadHeaders, Predicate<Map<String, List<String>>> downloadPred, DownloadListener listener, InputStream inputStream) {
        super(downloadHeaders, downloadPred);
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream == null");
        }
        this.downloadListener = listener;
        this.request = request;
        if (request != null && request instanceof AbstractHttpRequest) {
            AbstractHttpRequest ar = (AbstractHttpRequest) request;
            this.setBlockSize(ar.getBlockSize());
        }
        boolean _download = true;
        HttpHeaders headers = HttpHeaders.fromHeaders(downloadHeaders);
        if (downloadPred != null) _download = downloadPred.validate(downloadHeaders);
        if (_download) download(headers, inputStream);
    }

    protected HttpRequest request;

    /**
     Нужен для описания делегирования различным релизациям хранения тела запроса
     */
    private abstract class Variant {

        public abstract byte[] lookAheadBytes(int lookHeadBytesCount);

        public abstract long getRealDownloadedSize();

        public abstract String decodeDownloadDataAsText(Charset cs);

        public abstract InputStream getResponseDataStream();
    }

    private class MemoryData extends Variant {

        @Override
        public byte[] lookAheadBytes(int lookHeadBytesCount) {
            if (lookHeadBytesCount < 0) throw new IllegalArgumentException("lookHeadBytesCount<0");
            if (lookHeadBytesCount > memoryData.length) {
                lookHeadBytesCount = memoryData.length;
            }
            if (lookHeadBytesCount == 0) return new byte[] {};
            return Arrays.copyOf(memoryData, lookHeadBytesCount);
        }

        @Override
        public long getRealDownloadedSize() {
            return (long) memoryData.length;
        }

        @Override
        public String decodeDownloadDataAsText(Charset cs) {
            return new String(memoryData, charset);
        }

        @Override
        public InputStream getResponseDataStream() {
            return new ByteArrayInputStream(memoryData);
        }
    }

    private class NotAvaliabble extends Variant {

        @Override
        public byte[] lookAheadBytes(int lookHeadBytesCount) {
            return new byte[] {};
        }

        @Override
        public long getRealDownloadedSize() {
            return 0;
        }

        @Override
        public String decodeDownloadDataAsText(Charset cs) {
            return null;
        }

        @Override
        public InputStream getResponseDataStream() {
            return new ByteArrayInputStream(new byte[] {});
        }
    }

    private class FileData extends Variant {

        @Override
        public byte[] lookAheadBytes(int lookHeadBytesCount) {
            try {
                FileInputStream fin = new FileInputStream(fileData);
                byte[] buff = new byte[lookHeadBytesCount];
                int tot = 0;
                while (tot < lookHeadBytesCount) {
                    int readed = fin.read(buff, tot, lookHeadBytesCount - tot);
                    if (readed < 0) break;
                    if (readed > 0) {
                        tot += readed;
                    }
                }
                fin.close();
                if (tot < lookHeadBytesCount) buff = Arrays.copyOf(buff, tot);
                return buff;
            } catch (IOException ex) {
                Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
            }
            return new byte[] {};
        }

        @Override
        public long getRealDownloadedSize() {
            Long size = fileData.length();
            return size;
        }

        @Override
        public String decodeDownloadDataAsText(Charset cs) {
            return FileUtil.readAllText(fileData, charset);
        }

        @Override
        public InputStream getResponseDataStream() {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(fileData);
                return fin;
            } catch (FileNotFoundException ex) {
                Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fin.close();
                } catch (IOException ex) {
                    Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }
    }

    private Variant variant = new NotAvaliabble();

    @Override
    protected byte[] lookAheadBytes(int lookHeadBytesCount) {
        return variant.lookAheadBytes(lookHeadBytesCount);
    }

    @Override
    protected long getRealDownloadedSize() {
        return variant.getRealDownloadedSize();
    }

    @Override
    protected String decodeDownloadDataAsText(Charset cs) {
        return variant.decodeDownloadDataAsText(cs);
    }

    @Override
    public InputStream getResponseDataStream() {
        return variant.getResponseDataStream();
    }

    /**
     Описывает максимальный размер хранимых данных в памяти,
     при привышении данного размера, данные записываются в файл и в дальнейшем
     чтение происходит из этого файла.
     */
    private static int defMaxMemorySize = 1024 * 256;

    /**
     * Указывает максимальное кол-во байтов которое удерживается в памяти, по умолчанию
     * @return Макс. объем для оперативной памяти; -1 - нет ограничения.
     */
    public static int getDefaultMaxMemorySize() {
        return defMaxMemorySize;
    }

    /**
     * Указывает максимальное кол-во байтов которое удерживается в памяти, по умолчанию
     * @param maxSize Макс. объем для оперативной памяти; -1 - нет ограничения.
     */
    public static void setDefaultMaxMemorySize(int maxSize) {
        defMaxMemorySize = maxSize;
    }

    private int maxMemorySize = getDefaultMaxMemorySize();

    /**
     * Указывает максимальное кол-во байтов которое удерживается в памяти
     * @return Макс. объем для оперативной памяти; -1 - нет ограничения.
     */
    public int getMaxMemorySize() {
        return maxMemorySize;
    }

    /**
     * Указывает максимальное кол-во байтов которое удерживается в памяти
     * @param newMaxMemorySize Макс. объем для оперативной памяти; -1 - нет ограничения.
     */
    public void setMaxMemorySize(int newMaxMemorySize) {
        maxMemorySize = newMaxMemorySize;
    }

    private long dataSize = -1;

    private byte[] memoryData = null;

    private File fileData = null;

    /**
     Читает заголовок contentLength и если он присуствует, то смотрит на размер, при привышении сразу пишет в файл.
     Если отсуствует то начинает сначала качать в память и при привышении начинает записывать уже в файл
     @param headers Заголовки
     @param input Поток
     */
    private void download(HttpHeaders headers, InputStream input) {
        String lengthHeader = getNamedHeaderFiled(HttpHeaders.contentLength);
        fireBegin(this, request, this);
        boolean downloadToMemory = true;
        if (lengthHeader != null) {
            log("Заголовок " + HttpHeaders.contentLength + ": " + lengthHeader);
            try {
                long length = Long.parseLong(lengthHeader);
                long maxMemSize = getMaxMemorySize();
                if (length > maxMemSize && maxMemSize >= 0) {
                    downloadToMemory = false;
                }
            } catch (NumberFormatException ex) {
                downloadToMemory = true;
            }
        }
        if (downloadToMemory) downloadToMemory(input); else downloadToFile(input);
        fireEnd(this, request, this);
    }

    /**
     Читает поток в память, при привышении maxMemorySize, переходит к downloadToFile
     @param input Поток данных
     @see #downloadToFile(java.io.InputStream, java.io.ByteArrayOutputStream)
     */
    private void downloadToMemory(InputStream input) {
        log("downloadToMemory - start");
        ByteArrayOutputStream mem = new ByteArrayOutputStream();
        dataSize = 0;
        byte[] buff = new byte[getBlockSize()];
        try {
            while (true) {
                int readed = input.read(buff);
                if (readed < 0) break;
                if (readed > 0) {
                    mem.write(buff, 0, readed);
                    dataSize += readed;
                }
                log("readed=" + readed + " dataSize=" + dataSize);
                fireProgress(this, request, this, dataSize);
                long maxMemSize = getMaxMemorySize();
                if (dataSize > maxMemSize && maxMemSize >= 0) {
                    downloadToFile(input, mem);
                    log("downloadToMemory - end");
                    return;
                }
            }
        } catch (IOException ex) {
            variant = new NotAvaliabble();
            log("variant - NotAvaliabble");
            Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
            log("downloadToMemory - end");
            return;
        }
        memoryData = mem.toByteArray();
        variant = new MemoryData();
        log("variant - MemoryData");
        try {
            mem.close();
        } catch (IOException ex) {
            Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
        }
        log("downloadToMemory - end");
    }

    /**
     Пишит во временный файл поток
     @param input Поток
     */
    private void downloadToFile(InputStream input) {
        downloadToFile(input, null);
    }

    /**
     Пишит во временный файл поток
     @param input Поток
     @param downloadPart Та часть которую необходимо дописать в начало файла (может быть null)
     */
    private void downloadToFile(InputStream input, ByteArrayOutputStream downloadPart) {
        log("downloadToFile - start");
        try {
            fileData = File.createTempFile("httpResponse-cache-", "");
            fileData.deleteOnExit();
            byte[] buff = new byte[getBlockSize()];
            dataSize = 0;
            FileOutputStream fout = new FileOutputStream(fileData);
            if (downloadPart != null) {
                byte[] downloadPartData = downloadPart.toByteArray();
                fout.write(downloadPartData);
                dataSize += downloadPartData.length;
                log("copied to file =" + fileData.getName() + " dataSize=" + dataSize);
            }
            while (true) {
                int readed = input.read(buff);
                if (readed < 0) break;
                if (readed > 0) {
                    fout.write(buff, 0, readed);
                    dataSize += readed;
                }
                log("readed=" + readed + " dataSize=" + dataSize);
                fireProgress(this, request, this, dataSize);
            }
            fout.close();
        } catch (IOException ex) {
            variant = new NotAvaliabble();
            log("variant - NotAvaliabble");
            memoryData = null;
            fileData = null;
            Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
            log("downloadToFile - end");
            return;
        }
        variant = new FileData();
        log("variant - FileData");
        memoryData = null;
        log("downloadToFile - end");
    }
}
