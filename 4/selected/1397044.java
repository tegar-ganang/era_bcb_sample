package com.nhncorp.usf.core.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.nhncorp.lucy.common.config.model.ApplicationInfo;

/**
 * 파일 처리를 하기 위한 유틸리티 클래스
 * 
 * @author Web Platform Development Team
 */
public class FileUtil {

    private static Log log = LogFactory.getLog(FileUtil.class);

    public static final char NON_EUC_KR_CHAR = 65533;

    private static String encoding;

    /**
	 * 이 메소드는 예약어나, 시스템 인코딩 상태를 고려해서 서비스를 할 때 파일명으로 인한 문제가 발생하지 않도록 
	 * 파일명을 변경한다.
	 * 
	 * @param path 파일 경로명
	 * @return 변경된 파일 경로명
	 */
    public static String getCorrectedFileName(String path) {
        if (path == null) {
            return null;
        }
        String reserved = ";|/|\\?|:|@|&|=|\\+|\\s";
        String regEx = reserved;
        if (encoding == null) {
            encoding = ApplicationInfo.getPageEncoding();
        }
        boolean isMS949 = "MS949".equalsIgnoreCase(encoding);
        if (isMS949) {
            try {
                path = new String(path.getBytes(encoding), "EUC-KR");
            } catch (UnsupportedEncodingException e) {
                path = "myfile";
            }
            regEx += "|" + Character.toString(NON_EUC_KR_CHAR);
        }
        return "myfile".equals(path) ? path : path.replaceAll(regEx, "_");
    }

    /**
	 * 이 메소드는 파일명의 확장자를 주어진 확장자로 변경한다.
	 * 
	 * @param filename 파일명
	 * @param extension 확장자
	 * @return 확장자가 변경된 파일명 
	 */
    public static String changeExtension(String filename, String extension) {
        return FilenameUtils.removeExtension(filename) + "." + extension;
    }

    /**
	 * 이 메소드는 기존 파일(Source File)을 대상 파일(Destination File)로 Overwrite하는 메소드이다. <br/>
	 * 대상 파일이 존재하지 않으면 기존 파일을 대상 파일에 복사한다.
	 * @param srcFile 기존 파일
	 * @param destFile Overwrite 대상 파일
	 * @throws IOException source 또는 destination이 invalid 이거나 IO error가 발생하는 경우
	 * NullPointerException source 또는 destination이 null 인 경우
	 */
    public static void overwrite(File srcFile, File destFile) throws IOException {
        FileUtils.copyFile(srcFile, destFile);
    }

    /**
	 * 이 메소드는 주어진 path에 있는 파일의 내용을 byte array로 읽는다.
	 *  
	 * @param path 경로를 포함한 파일명
	 * @return byte array type의 파일 내용 또는 null
	 * @throws IOException IO error가 발생하는 경우 
	 */
    public static byte[] readFileToByteArray(String path) throws IOException {
        return FileUtils.readFileToByteArray(new File(path));
    }

    /**
	 * 이 메소드는 전체 파일 경로에서 디렉터리 경로를 제외한 파일 이름을 반환한다. <br/><br/>
	 * <pre>
	 * a/b/c.txt	--> c.txt
	 * a.txt		--> a.txt
	 * a/b/c		--> c
	 * a/b/c/		--> ""
	 * </pre>
	 * @param path 파일 전체 경로
	 * @return 파일 경로가 없는 파일 이름 또는 경로가 존재하지 않으면 ""
	 */
    public static String getFileName(String path) {
        return FilenameUtils.getName(path);
    }

    /**
	 * 이 메소드는 system-dependent 파일 구분자를 반환한다.
	 * @return 파일 구분자 
	 */
    public static String getFileSeparator() {
        return File.separator;
    }

    /**
	 * 이 메소드는 동일한 파일이 존재하는 경우 Unique한 파일명을 만들어준다. 
	 * 그렇지 않은 경우는 입력된 파일을 그대로 반환한다. <br/>
	 * Unique 파일명 만드는 법 : 파일이 존재하는 경우 확장자 앞에 (1), (2), ... 등을 추가한다. <br/><br/>
	 * <pre>
	 * /a/b.txt가 존재하는 경우 --> /a/b(1).txt 생성
	 * </pre>
	 * @param file 파일 
	 * @return Unique 파일
	 * NullPointerException 파일이 null 인 경우
	 */
    public static File getUniqueFile(File file) {
        String parent = file.getParent();
        String name = file.getName();
        String baseName = FilenameUtils.getBaseName(name);
        String extension = FilenameUtils.getExtension(name);
        File uniqueFile = file;
        int i = 1;
        while (uniqueFile.exists()) {
            StringBuilder sb = new StringBuilder();
            if (parent != null) {
                sb.append(parent).append(File.separator);
            }
            sb.append(baseName).append('(').append(i++).append(')');
            if (!StringUtils.isEmpty(extension)) {
                sb.append('.').append(extension);
            }
            uniqueFile = new File(sb.toString());
        }
        return uniqueFile;
    }

    /**
	 * 이 메소드는 입력된 byte array를 해당 repository에 fileName으로 저장한다.
	 * 
	 * @param bytes 저장할 내용
	 * @param repository 파일 저장소
	 * @param fileName 저장할 파일 이름
	 * @return 저장된 바이트 수
	 * @throws IOException IO error가 발생할 경우 
	 * NullPointerException param이 null인 경우
	 * ArithmeticException 바이트 수가 매우 클 경우(over 2GB)
	 */
    public static int save(byte[] bytes, String repository, String fileName) throws IOException {
        File outFile = new File(FilenameUtils.concat(repository, fileName));
        return save(bytes, outFile);
    }

    /**
	 * 이 메소드는 입력된 byte array를 해당 outputFile로 저장한다.
	 * @param bytes 저장할 내용
	 * @param outputFile 파일 저장소
	 * @return 저장된 바이트 수
	 * @throws IOException IO error가 발생할 경우 
	 * NullPointerException param이 null인 경우
	 * ArithmeticException 바이트 수가 매우 클 경우(over 2GB)
	 */
    public static int save(byte[] bytes, File outputFile) throws IOException {
        InputStream in = new ByteArrayInputStream(bytes);
        outputFile.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(outputFile);
        try {
            return IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            try {
                out.close();
            } catch (IOException ioe) {
                ioe.getMessage();
            }
            try {
                in.close();
            } catch (IOException ioe) {
                ioe.getMessage();
            }
        }
    }

    /**
	 * inputFile을 outputFile로 저장한다.
	 * @param inputFile 저장할 파일
	 * @param outputFile 파일 저장소
	 * @return 저장된 바이트 수
	 * @throws IOException IO error가 발생할 경우 
	 * NullPointerException param이 null인 경우
	 * ArithmeticException 바이트 수가 매우 클 경우(over 2GB)
	 */
    public static int save(File inputFile, File outputFile) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(inputFile);
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream(outputFile);
        } catch (Exception e) {
            e.getMessage();
        }
        try {
            return IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                ioe.getMessage();
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
                ioe.getMessage();
            }
        }
    }

    /**
	 * 이 메소드는 입력된 byte array를 Unique한 outputFile로 저장한다.
	 * 
	 * @param bytes bytes 저장할 내용
	 * @param outputFile 파일 저장소
	 * @return Unique 파일
	 * @throws IOException IO error가 발생할 경우 
	 * NullPointerException param이 null인 경우
	 * ArithmeticException 바이트 수가 매우 클 경우(over 2GB)
	 */
    public static File saveToUniqueFileName(byte[] bytes, File outputFile) throws IOException {
        outputFile = getUniqueFile(outputFile);
        save(bytes, outputFile);
        return outputFile;
    }

    /**
	 * inputFile을 Unique한 outputFile로 저장한다.
	 * 
	 * @param inputFile 저장할 내용
	 * @param outputFile 파일 저장소
	 * @return Unique 파일
	 * @throws IOException IO error가 발생할 경우 
	 * NullPointerException param이 null인 경우
	 * ArithmeticException 바이트 수가 매우 클 경우(over 2GB)
	 */
    public static File saveToUniqueFileName(File inputFile, File outputFile) throws IOException {
        outputFile = getUniqueFile(outputFile);
        save(inputFile, outputFile);
        return outputFile;
    }

    /**
	 * 이 메소드는 srcFile을 destFile로 복사한다.<br/>
	 * destFile이 있는 디렉터리가 존재 하지 않을 경우 새로 생성되며, destFile이 존재 할 경우 Unique 파일로 저장한다.
	 * 
	 * @param srcFile 복사할 파일, null이 아니어야 한다.
	 * @param destFile 복사 대상 파일, null이 아니어야 한다.
	 * @return 복사 대상 파일(Destination File)의 절대 경로
	 * @throws IOException IO error가 발생할 경우
	 * NullPointerException param이 null인 경우
	 */
    public static String copy(File srcFile, File destFile) throws IOException {
        destFile = getUniqueFile(destFile);
        FileUtils.copyFile(srcFile, destFile, true);
        return destFile.getAbsolutePath();
    }

    /**
	 * 이 메소드는 srcPath에 있는 파일을 destDir로 복사한다.<br/>
	 * destDir이 존재 하지 않을 경우 새로 생성되며, 같은 이름의 파일이 존재 할 경우 Unique 파일로 저장한다.
	 * 
	 * @param srcPath 복사할 파일 경로, null이 아니어야 함.
	 * @param destDir 복사 대상 디렉터리, null이 아니어야 함.
	 * @return 복사 대상 파일의 절대 경로
	 * @throws IOException IO error가 발생할 경우
	 * NullPointerException param이 null인 경우
	 */
    public static String copy(String srcPath, File destDir) throws IOException {
        File srcFile = new File(srcPath);
        File destFile = new File(destDir, FilenameUtils.getName(srcPath));
        return copy(srcFile, destFile);
    }

    /**
	 * 이 메소드는 복수의 파일 경로에 있는 파일들을 destDirPath의 디렉터리로 복사한다.<br/>
	 * destDirPath가 존재하지 않을 경우 새로 생성되며, 복사할 파일명과 같은 이름의 파일이 존재할 경우 Unique 파일로 저장한다.
	 * 
	 * @param srcPaths 복사할 파일 경로 Array, null이 아니어야 한다.
	 * @param destDirPath 복사 대상 디렉터리 경로, null이 아니어야 한다.
	 * @return 복사 대상 파일의 절대 경로 Array
	 * @throws IOException IO error가 발생할 경우
	 * NullPointerException param이 null인 경우
	 */
    public static String[] copy(String[] srcPaths, String destDirPath) throws IOException {
        String[] destPaths = new String[srcPaths.length];
        File destDir = new File(destDirPath);
        for (int i = 0; i < srcPaths.length; i++) {
            destPaths[i] = copy(srcPaths[i], destDir);
        }
        return destPaths;
    }

    /**
	 * 이 메소드는 srcFile을 destFile로 이동한다. <br/>
	 * overwite가 true이면, 기존 destFile에 srcFile을 덮어쓰게 되고, 그렇지 않은 경우는
	 * Unique 파일명으로 저장된다.
	 * 
	 * @param srcFile 이동할 파일
	 * @param destFile 이동되는 파일
	 * @param overwrite 덮어쓰기 여부
	 * @return 이동되는 파일의 절대 경로
	 * NullPointerException param이 null인 경우
	 */
    public static String move(File srcFile, File destFile, boolean overwrite) {
        if (overwrite) {
            destFile.delete();
        } else {
            destFile = getUniqueFile(destFile);
        }
        destFile.getParentFile().mkdirs();
        boolean success = srcFile.renameTo(destFile);
        if (!success) {
            try {
                copy(srcFile, destFile);
                deleteFile(srcFile.getAbsolutePath());
            } catch (IOException ioe) {
                ioe.getMessage();
            }
        }
        return destFile.getAbsolutePath();
    }

    /**
	 * 이 메소드는 srcFile 경로의 파일을 destFile 경로로 이동한다. <br/>
	 * overwite가 true이면, 기존 destFile에 srcFile을 덮어쓰게 되고, 그렇지 않은 경우는
	 * Unique 파일명으로 저장된다. 
	 * 
	 * @param srcFile 이동할 파일 경로명
	 * @param destFile 이동되는 파일 경로명
	 * @param overwrite 덮어쓰기 여부
	 * @return 이동되는 파일의 절대 경로
	 * NullPointerException param이 null인 경우
	 */
    public static String move(String srcFile, String destFile, boolean overwrite) {
        return move(new File(srcFile), new File(destFile), overwrite);
    }

    /**
	 * 이 메소드는 srcPath 경로의 파일을 destPath 경로로 이동한다. <br/>
	 * destPath의 파일이 이미 존재할 경우 Overwrite는 하지 않고 Unique 파일로 저장된다. 
	 * @param srcPath 이동할 파일 경로명
	 * @param destPath 이동되는 파일 경로명
	 * @return 이동되는 파일 절대 경로
	 * NullPointerException param이 null인 경우
	 */
    public static String move(String srcPath, String destPath) {
        return move(srcPath, destPath, false);
    }

    /**
	 * 이 메소드는 srcFile 파일을 destFile로 이동한다. <br/>
	 * destFile이 이미 존재할 경우 Overwrite는 하지 않고 Unique 파일로 저장된다.
	 * @param srcFile 이동할 파일
	 * @param destFile 이동되는 파일
	 * @return 이동되는 파일 절대 경로
	 * NullPointerException param이 null인 경우
	 */
    public static String move(File srcFile, File destFile) {
        return move(srcFile, destFile, false);
    }

    /**
	 * 이 메소드는 srcPath 경로의 파일을 destDirPath 경로의 디렉터리로 이동한다. <br/>
	 * overwrite가 true이면, destDirPath 경로에 동일한 파일이 있을 경우 
	 * srcPath 경로의 파일로 덮어쓰게 되고, 그렇지 않은 경우는 Unique 파일명으로 저장된다.
	 * 
	 * @param srcPath 이동할 파일 경로명
	 * @param destDirPath 이동될 디렉터리 경로명
	 * @param overwrite 덮어쓰기
	 * @return 이동되는 파일 절대 경로
	 * NullPointerException param이 null인 경우
	 */
    public static String moveToDir(String srcPath, String destDirPath, boolean overwrite) {
        File srcFile = new File(srcPath);
        File destFile = new File(destDirPath, FilenameUtils.getName(srcPath));
        return move(srcFile, destFile, overwrite);
    }

    /**
	 * 이 메소드는 srcPath 경로의 파일을 destDir 디렉터리로 이동한다. <br/>
	 * overwrite가 true이면, destDir에 동일한 파일이 있을 경우 srcPath 경로의 
	 * 파일로 덮어쓰게 되고, 그렇지 않은 경우는 Unique 파일명으로 저장된다.
	 * 
	 * @param srcPath 이동할 파일 경로명
	 * @param destDir 이동될 디렉터리
	 * @param overwrite 덮어쓰기
	 * @return 이동되는 파일 절대 경로
	 * NullPointerException param이 null인 경우
	 */
    public static String moveToDir(String srcPath, File destDir, boolean overwrite) {
        File srcFile = new File(srcPath);
        File destFile = new File(destDir, FilenameUtils.getName(srcPath));
        return move(srcFile, destFile, overwrite);
    }

    /**
	 * 이 메소드는 srcPath 경로의 파일을 destDir 디렉터리로 이동한다. <br/>
	 * destDir 디렉터리에 srcPath 경로의 파일명과 동일한 이름의 파일이 존재할 경우
	 * , Unique 파일명으로 저장된다.
	 * @param srcPath 이동 할 파일 경로명
	 * @param destDir 이동 될 디렉터리
	 * @return 이동 되는 파일 절대 경로
	 * NullPointerException param이 null인 경우
	 */
    public static String moveToDir(String srcPath, File destDir) {
        return moveToDir(srcPath, destDir, false);
    }

    /**
	 * 이 메소드는 srcPath 경로의 파일을 destDirPath 경로의 디렉터리로 이동한다. <br/>
	 * destDirPath 경로의 디렉터리에 srcPath 경로의 파일명과 동일한 이름의 파일이 존재할 경우
	 * , Unique 파일명으로 저장된다.
	 * 
	 * @param srcPath 이동할 파일 경로명
	 * @param destDirPath 이동될 디렉터리 경로
	 * @return 이동되는 파일 절대 경로
	 * NullPointerException param이 null인 경우
	 */
    public static String moveToDir(String srcPath, String destDirPath) {
        return moveToDir(srcPath, destDirPath, false);
    }

    /**
	 * 이 메소드는 srcPaths 경로의 파일들을 destDirPath 경로의 디렉터리로 이동한다. <br/>
	 * overwrite가 true이면, destDirPath 경로에 동일한 파일이 있을 경우 
	 * srcPaths 경로의 파일로 덮어쓰게 되고, 그렇지 않은 경우는 Unique 파일명으로 생성된다.
	 * 
	 * @param srcPaths 이동할 파일 경로명 array
	 * @param destDirPath 이동될 디렉터리 경로명
	 * @param overwrite 덮어쓰기
	 * @return 이동되는 파일 절대 경로들의 array
	 * NullPointerException param이 null인 경우
	 */
    public static String[] moveToDir(String[] srcPaths, String destDirPath, boolean overwrite) {
        String[] destPaths = new String[srcPaths.length];
        for (int i = 0; i < srcPaths.length; i++) {
            destPaths[i] = moveToDir(srcPaths[i], destDirPath, overwrite);
        }
        return destPaths;
    }

    /**
	 * 이 메소드는 srcPaths 경로의 파일들을 destDirPath 경로의 디렉터리로 이동한다. <br/>
	 * destDirPath 경로의 디렉터리에 srcPaths 경로의 파일명과 동일한 이름의 파일이 존재할 경우
	 * , Unique 파일명으로 저장된다.
	 * 
	 * @param srcPaths 이동할 파일 경로명 array
	 * @param destDirPath 이동될 디렉터리 경로명
	 * @return 이동되는 파일 절대 경로들의 array
	 * NullPointerException param이 null인 경우
	 */
    public static String[] moveToDir(String[] srcPaths, String destDirPath) {
        return moveToDir(srcPaths, destDirPath, false);
    }

    /**
	 * 이 메소드는 path 경로에 있는 파일을 삭제한다.
	 * 
	 * @param path 삭제할 파일 경로명
	 * @return 삭제된 파일 크기
	 * @throws IOException IO error가 발생할 경우
	 * NullPointerException param이 null인 경우
	 */
    public static long deleteFile(String path) throws IOException {
        long size = 0;
        File deleteFile = new File(path);
        if (deleteFile.isDirectory()) {
            log.error("File(" + path + ") is directory. Use method named deleteDirectory.");
        } else {
            size = deleteFile.length();
            FileUtils.forceDelete(deleteFile);
            log.info("File(" + path + ") is deleted...");
        }
        return size;
    }

    /**
	 * 이 메소드는 paths 경로에 있는 파일들을 삭제한다.
	 * @param paths 삭제할 파일 경로명 Array
	 * @return 삭제된 파일의 총 크기
	 * @throws IOException IO error가 발생할 경우
	 * NullPointerException param이 null인 경우
	 */
    public static long deleteFiles(String[] paths) throws IOException {
        long size = 0;
        for (String path : paths) {
            size += deleteFile(path);
        }
        return size;
    }

    /**
	 * 이 메소드는 dirPaths 경로의 디렉터리들을 재귀적으로 삭제한다.
	 * @param dirPaths 삭제할 디렉터리 경로명 Array
	 * @return 삭제된 디렉터리 총 크기
	 * @throws IOException IO error가 발생할 경우 
	 * NullPointerException param이 null인 경우
	 */
    public static long deleteDirectory(String[] dirPaths) throws IOException {
        long size = 0;
        for (String dirPath : dirPaths) {
            File dir = new File(dirPath);
            size += FileUtils.sizeOfDirectory(dir);
            FileUtils.deleteDirectory(dir);
            log.info("Directory(" + dirPath + ") is deleted...");
        }
        return size;
    }

    /**
	 * 이 메소드는 file의 존재 여부를 체크한다.
	 * @param file 체크할 파일
	 * @return 존재하면 true, 존재하지 않으면 false
	 * NullPointerException param이 null인 경우
	 */
    public static boolean fileExists(File file) {
        return (file.exists() && file.isFile());
    }

    /**
	 * 이 메소드는 path 경로에 있는 파일의 존재 여부를 체크한다.
	 * @param path 체크할 파일 경로명
	 * @return 존재하면 true, 존재하지 않으면 false
	 * NullPointerException param이 null인 경우
	 */
    public static boolean fileExists(String path) {
        return fileExists(new File(path));
    }

    /**
	 * 이 메소드는 file이 파일인지 아닌지 체크한다.
	 * @param file 체크할 파일
	 * @return 파일이면 true, 디렉터리이면 false
	 * NullPointerException param이 null인 경우
	 */
    public static boolean isFile(File file) {
        return file.isFile();
    }

    /**
	 * 이 메소드는 dir 경로의 실제 디렉터리를 생성한다.
	 * @param dir 디렉터리
	 * NullPointerException param이 null인 경우
	 */
    public static void mkdirs(File dir) {
        dir.mkdirs();
    }

    /**
	 * 이 메소드는 dirPath 경로의 모든 디렉터리를 생성한다.
	 * @param dirPath 디렉터리 경로
	 * NullPointerException param이 null인 경우
	 */
    public static void mkdirs(String dirPath) {
        mkdirs(new File(dirPath));
    }

    /**
	 * 이 메소드는 해당 file의 parent를 반환한다.
	 * @param file 파일
	 * @return parent 파일 
	 * NullPointerException param이 null인 경우
	 */
    public static File getParentFile(File file) {
        return file.getParentFile();
    }

    /**
	 * 이 메소드는 파일의 존재 여부를 체크한다.
	 * @param file 체크할 파일
	 * @return 존재하면 true, 그렇지 않으면 false
	 * NullPointerException param이 null인 경우
	 */
    public static boolean exists(File file) {
        return file.exists();
    }

    /**
	 * 이 메소드는 파일 경로에 있는 파일의 존재 여부를 체크한다.
	 * @param path 체크할 파일 경로명
	 * @return 존재하면 true, 그렇지 않으면 false
	 * NullPointerException param이 null인 경우
	 */
    public static boolean exists(String path) {
        return new File(path).exists();
    }

    /**
	 * 이 메소드는 zipFilePath 경로에 있는 ZipFile을 destDirPath 경로의 디렉터리에 압축을 푼다.
	 * @param zipFilePath ZipFile 경로명
	 * @param destDirPath 압축을 풀 디렉터리 경로명
	 * @return 압축을 푼 파일들의 경로명 List
	 * @throws IOException IOException
	 */
    public static List<String> extract(String zipFilePath, String destDirPath) throws IOException {
        List<String> list = null;
        ZipFile zip = new ZipFile(zipFilePath);
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File destFile = new File(destDirPath, entry.getName());
                if (entry.isDirectory()) {
                    destFile.mkdirs();
                } else {
                    InputStream in = zip.getInputStream(entry);
                    OutputStream out = new FileOutputStream(destFile);
                    try {
                        IOUtils.copy(in, out);
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                        try {
                            out.close();
                        } catch (IOException ioe) {
                            ioe.getMessage();
                        }
                        try {
                            in.close();
                        } catch (IOException ioe) {
                            ioe.getMessage();
                        }
                    }
                }
                if (list == null) {
                    list = new ArrayList<String>();
                }
                list.add(destFile.getAbsolutePath());
            }
            return list;
        } finally {
            try {
                zip.close();
            } catch (Exception e) {
                e.getMessage();
            }
        }
    }
}
