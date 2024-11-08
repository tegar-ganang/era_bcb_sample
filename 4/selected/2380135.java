package org.hlj.commons.cvs.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hlj.commons.collection.Lists;
import org.hlj.commons.collection.Maps;
import org.hlj.commons.common.CommonUtil;
import org.hlj.commons.csv.Csv;
import org.hlj.commons.exception.CustomRuntimeException;
import org.hlj.log.log4j.common.SysLog;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * 操作CSV格式文件 <br/>
 * <h2>注: 本操作需要使用opencsv包</h2>
 * @author WD
 * @since JDK5
 * @version 1.0 2009-03-28
 */
public final class CsvImpl implements Csv {

    private CSVReader reader;

    private CSVWriter writer;

    /**
	 * 构造方法
	 * @param reader 读取器
	 * @param writer 写入器
	 */
    public CsvImpl(Reader reader, Writer writer) {
        this.reader = new CSVReader(reader);
        this.writer = new CSVWriter(writer);
    }

    /**
	 * 构造方法
	 * @param reader 读取器
	 */
    public CsvImpl(Reader reader) {
        this.reader = new CSVReader(reader);
    }

    /**
	 * 构造方法
	 * @param writer 写入器
	 */
    public CsvImpl(Writer writer) {
        this.writer = new CSVWriter(writer);
    }

    /**
	 * 读取出所有元素到列表中
	 * @return List 所有集合
	 */
    public final List<String[]> readAll() {
        try {
            return reader.readAll();
        } catch (IOException e) {
            throw new CustomRuntimeException(e);
        }
    }

    /**
	 * 读取出所有元素到列表中
	 * @return List key标题 value值
	 */
    public final List<Map<String, String>> readByAll() {
        List<String[]> list = readAll();
        if (CommonUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        String[] colName = list.get(0);
        int size = list.size();
        List<Map<String, String>> ls = Lists.getList(size - 1);
        Map<String, String> map = null;
        String[] value = null;
        for (int i = 1; i < size; i++) {
            value = list.get(i);
            map = Maps.getMap(colName.length);
            for (int j = 0; j < colName.length; j++) {
                if (CommonUtil.isEmpty(colName[j])) {
                    continue;
                }
                map.put(colName[j], value[j]);
            }
            ls.add(map);
        }
        return ls;
    }

    /**
	 * 读取一行
	 * @return String[]
	 */
    public final String[] readNext() {
        try {
            return reader.readNext();
        } catch (IOException e) {
            throw new CustomRuntimeException(e);
        }
    }

    /**
	 * 写入全部数据到CSV
	 * @param list
	 * @throws CustomException
	 */
    public final void writeAll(List<String[]> list) {
        try {
            writer.writeAll(list);
            writer.flush();
        } catch (IOException e) {
            SysLog.error(e);
        }
    }

    /**
	 * 写一行
	 * @param nextLine
	 * @throws CustomException
	 */
    public final void writeNext(String[] nextLine) {
        try {
            writer.writeNext(nextLine);
            writer.flush();
        } catch (IOException e) {
            SysLog.error(e);
        }
    }

    /**
	 * 关闭方法
	 */
    public final void close() {
        try {
            if (!CommonUtil.isEmpty(reader)) {
                reader.close();
            }
        } catch (IOException e) {
            SysLog.error(e);
        } finally {
            reader = null;
        }
        try {
            if (!CommonUtil.isEmpty(writer)) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            SysLog.error(e);
        } finally {
            writer = null;
        }
    }
}
