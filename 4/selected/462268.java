package com.cross.core;

/**
 * 搬运器的一个简单实现
 * 
 * 2009-2-7
 * 
 * @author craziness_stone
 */
public class SimpleTractor extends AbstractTractor {

    public SimpleTractor() {
    }

    public SimpleTractor(Reader<Row> reader, Writer writer) {
        super.reader = reader;
        super.writer = writer;
    }

    protected void run() throws Exception {
        if (null == reader) {
            throw new Exception("没有读取器");
        }
        if (null == writer) {
            throw new Exception("没有组装器");
        }
        while (reader.hasNext()) {
            if (writer.available()) {
                writer.write(reader.next());
            } else {
                reader.close();
                writer.close();
                break;
            }
        }
    }
}
