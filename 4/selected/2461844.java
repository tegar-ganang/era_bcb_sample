package ppa.marc.robot;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ppa.marc.RecordReader;
import ppa.marc.RecordWriter;
import ppa.marc.domain.Record;

public class RecordAccessLibrary {

    private RecordReader recordReader;

    private RecordWriter recordWriter;

    public RecordAccessLibrary(String readerBeanName, String writerBeanName) {
        recordReader = (RecordReader) new ClassPathXmlApplicationContext(RecordReader.class.getName() + ".xml").getBean(readerBeanName);
        recordWriter = (RecordWriter) new ClassPathXmlApplicationContext(RecordWriter.class.getName() + ".xml").getBean(writerBeanName);
    }

    RecordAccessLibrary(RecordReader recordReader, RecordWriter recordWriter) {
        this.recordReader = recordReader;
        this.recordWriter = recordWriter;
    }

    public String readRecord(String filename) throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        recordWriter.writeRecords(outputStream, recordReader.read(openInputFile(filename)));
        return outputStream.toString();
    }

    OutputStream getOutputStream() throws FileNotFoundException {
        return System.out;
    }

    InputStream openInputFile(String filename) throws FileNotFoundException {
        return new FileInputStream(filename);
    }

    public String printRecordWithToString(String filename) throws IOException {
        List<Record> records = recordReader.read(openInputFile(filename));
        StringBuilder stringBuilder = new StringBuilder();
        for (Record record : records) {
            if (stringBuilder.length() > 0) stringBuilder.append('\n');
            stringBuilder.append(record.toString());
        }
        return stringBuilder.toString();
    }
}
