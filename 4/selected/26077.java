package org.hlj.commons.nio;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import org.hlj.commons.io.file.FileUtil;
import org.hlj.param.constants.EncodeConstants;

/**
 * 文件通道操作
 * @author WD
 * @since JDK5
 * @version 1.0 2009-04-21
 */
public final class FileChannels {

    /**
	 * 复制文件
	 * @param src 原文件
	 * @param target 目标文件
	 */
    public static final boolean copy(String src, String target) {
        return write(target, FileUtil.getInputStream(src));
    }

    /**
	 * 复制文件
	 * @param src 原文件
	 * @param target 目标文件
	 */
    public static final boolean copy(File src, File target) {
        return StreamChannel.write(FileUtil.getOutputStream(target), FileUtil.getInputStream(src));
    }

    /**
	 * 读取文件
	 * @param fileName 要读取的文件
	 * @return 读取文件的内容
	 */
    public static final String readString(String fileName) {
        return readString(fileName, EncodeConstants.UTF_8);
    }

    /**
	 * 读取文件
	 * @param fileName 要读取的文件
	 * @param charsetName 编码格式
	 * @return 读取文件的内容
	 */
    public static final String readString(String fileName, String charsetName) {
        return StreamChannel.readString(FileUtil.getInputStream(fileName), charsetName);
    }

    /**
	 * 把文件写指定路径中
	 * @param fileName 文件名
	 * @param file 文件
	 * @return true 成功 false 失败
	 */
    public static final boolean write(String fileName, File file) {
        return StreamChannel.write(FileUtil.getOutputStream(fileName), FileUtil.getInputStream(file));
    }

    /**
	 * 把InputStream流中的内容保存到文件中
	 * @param fileName 文件名
	 * @param is 流
	 * @return true 成功 false 失败
	 */
    public static final boolean write(String fileName, InputStream is) {
        return StreamChannel.write(FileUtil.getOutputStream(fileName), is);
    }

    /**
	 * 写文件 默认使用UTF-8编码
	 * @param text 写入的内容
	 * @param fileName 文件名
	 * @return true false
	 */
    public static final boolean writeString(String fileName, String text) {
        return writeString(fileName, text, EncodeConstants.UTF_8);
    }

    /**
	 * 把文件写指定路径中
	 * @param fileName 文件名
	 * @param file 文件
	 * @return true 成功 false 失败
	 */
    public static final boolean write(String fileName, byte[] b) {
        return StreamChannel.write(FileUtil.getOutputStream(fileName), b);
    }

    /**
	 * 写文件
	 * @param text 写入的内容
	 * @param fileName 文件名
	 * @param charsetName 编码格式
	 * @return true false
	 */
    public static final boolean writeString(String fileName, String text, String charsetName) {
        return StreamChannel.writeString(FileUtil.getOutputStream(fileName), text, charsetName);
    }

    /**
	 * 获得文件输入流 如果
	 * @param fileName 文件名
	 * @return 输入流
	 */
    public static final FileChannel getInputChannel(String fileName) {
        return getInputChannel(FileUtil.getFile(fileName));
    }

    /**
	 * 获得文件输入流 如果
	 * @param file 文件
	 * @return 输入流
	 */
    public static final FileChannel getInputChannel(File file) {
        return FileUtil.getInputStream(file).getChannel();
    }

    /**
	 * 获得文件输出流 如果
	 * @param fileName 文件名
	 * @return 输出流
	 */
    public static final FileChannel getOutputChannel(String fileName) {
        return getOutputChannel(FileUtil.getFile(fileName));
    }

    /**
	 * 获得文件输出流 如果
	 * @param file 文件
	 * @return 输出流
	 */
    public static final FileChannel getOutputChannel(File file) {
        return FileUtil.getOutputStream(file).getChannel();
    }

    private FileChannels() {
    }
}
