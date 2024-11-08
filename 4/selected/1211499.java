package org.wdcode.core.csv;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.wdcode.common.constants.EncodingConstants;
import org.wdcode.common.exception.CustomRuntimeException;
import org.wdcode.common.io.FileUtil;

/**
 * CSV读写器操作工厂
 * @author WD
 * @since JDK6
 * @version 1.0 2009-12-27
 */
public final class CsvFactory {

    /**
	 * 私有构造
	 */
    private CsvFactory() {
    }

    /**
	 * 获得读实例
	 * @param fileName 文件名
	 * @return Csv读写器
	 */
    public static final Csv getReadInstance(String fileName) {
        return getReadInstance(FileUtil.getInputStream(fileName));
    }

    /**
	 * 获得读实例
	 * @param file 文件
	 * @return Csv读写器
	 */
    public static final Csv getReadInstance(File file) {
        return getReadInstance(FileUtil.getInputStream(file));
    }

    /**
	 * 获得读实例 使用UTF-8编码
	 * @param in 流
	 * @return Csv读写器
	 */
    public static final Csv getReadInstance(InputStream in) {
        return getReadInstance(in, EncodingConstants.UTF_8);
    }

    /**
	 * 获得读实例
	 * @param in 流
	 * @param charsetName 编码
	 * @return Csv读写器
	 */
    public static final Csv getReadInstance(InputStream in, String charsetName) {
        try {
            return getReadInstance(new InputStreamReader(in, charsetName));
        } catch (UnsupportedEncodingException e) {
            throw new CustomRuntimeException(e);
        }
    }

    /**
	 * 获得读实例
	 * @param reader
	 * @return Csv读写器
	 */
    public static final Csv getReadInstance(Reader reader) {
        return new CsvImpl(reader);
    }

    /**
	 * 获得读实例
	 * @param fileName 文件名
	 * @return Csv读写器
	 */
    public static final Csv getWriteInstance(String fileName) {
        return getWriteInstance(FileUtil.getOutputStream(fileName));
    }

    /**
	 * 获得读实例
	 * @param file 文件
	 * @return Csv读写器
	 */
    public static final Csv getWriteInstance(File file) {
        return getWriteInstance(FileUtil.getOutputStream(file));
    }

    /**
	 * 获得写实例 使用UTF-8编码
	 * @param out 输出流
	 * @param charsetName 编码
	 * @return Csv读写器
	 */
    public static final Csv getWriteInstance(OutputStream out) {
        return getWriteInstance(out, EncodingConstants.UTF_8);
    }

    /**
	 * 获得写实例
	 * @param out 输出流
	 * @param charsetName 编码
	 * @return Csv读写器
	 */
    public static final Csv getWriteInstance(OutputStream out, String charsetName) {
        try {
            return getWriteInstance(new OutputStreamWriter(out, charsetName));
        } catch (UnsupportedEncodingException e) {
            throw new CustomRuntimeException(e);
        }
    }

    /**
	 * 获得写实例
	 * @param writer 写入器
	 * @return Csv读写器
	 */
    public static final Csv getWriteInstance(Writer writer) {
        return new CsvImpl(writer);
    }

    /**
	 * 获得Csv读写器 使用UTF-8编码
	 * @param in 输入流
	 * @param out 输出流
	 * @return Csv读写器
	 */
    public static final Csv getInstance(InputStream in, OutputStream out) {
        try {
            return new CsvImpl(new InputStreamReader(in, EncodingConstants.UTF_8), new OutputStreamWriter(out, EncodingConstants.UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new CustomRuntimeException(e);
        }
    }

    /**
	 * 获得Csv读写器
	 * @param in 输入流
	 * @param out 输出流
	 * @param charsetName 编码
	 * @return Csv读写器
	 */
    public static final Csv getInstance(InputStream in, OutputStream out, String charsetName) {
        try {
            return new CsvImpl(new InputStreamReader(in, charsetName), new OutputStreamWriter(out, charsetName));
        } catch (UnsupportedEncodingException e) {
            throw new CustomRuntimeException(e);
        }
    }

    /**
	 * 获得Csv读写器
	 * @param reader 读取器
	 * @param writer 写入器
	 * @return Csv读写器
	 */
    public static final Csv getInstance(Reader reader, Writer writer) {
        return new CsvImpl(reader, writer);
    }
}
