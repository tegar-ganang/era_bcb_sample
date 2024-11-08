package com.taobao.remote;

/**
 * NTS
 * 
 * ��Ӧһ��socket���ӣ�������selectionKey��
 * 
 * 
 * @version 2007-2-6
 * @author xalinx at gmail dot com
 * 
 */
public class TaskAttach<E, M> {

    private TaskReader<E, M> taskReader;

    private TaskWriter taskWriter;

    public TaskReader<E, M> getTaskReader() {
        return taskReader;
    }

    public TaskWriter getTaskWriter() {
        return taskWriter;
    }

    public TaskAttach(TaskReader<E, M> reader, TaskWriter writer) {
        this.taskReader = reader;
        this.taskWriter = writer;
    }

    public void close() {
        taskReader.close();
        taskReader = null;
        taskWriter.close();
        taskWriter = null;
    }
}
