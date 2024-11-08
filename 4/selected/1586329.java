package com.cross.core;

/**
 * 搬运器
 * 
 * 将数据从一个存储格式转化为另外一种格式.
 * 搬运器一般拥有一对读写器。它从读出器中读出列对象，
 * 然后调用写入到指定格式的文件中
 * 
 * 
 * 2009-5-11
 * 
 * @author craziness_stone
 */
public abstract class AbstractTractor implements Tractor {

    protected Reader<Row> reader;

    protected Writer writer;

    /**
	 * 开始执行搬运
	 * 
	 * 
	 * 2009-5-11
	 * 
	 * @author craziness_stone
	 * @throws Exception
	 */
    public void start() throws Exception {
        writer.setHeader(reader.getHeader());
        run();
        reader.close();
        writer.close();
    }

    /**
	 * 执行搬运
	 * 
	 * 2009-5-11
	 * 
	 * @author craziness_stone cc
	 * @throws Exception
	 */
    protected abstract void run() throws Exception;
}
