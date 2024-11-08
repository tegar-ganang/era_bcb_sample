package ces.coffice.webmail.mailmodel.mail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.activation.DataSource;

public class ByteArrayDataSource implements DataSource {

    /**
   * Ĭ�ϱ�������
   */
    public static String DEFAULTENCODING = "iso-8859-1";

    /**
   * ���������
   */
    private byte[] data;

    /**
   * ��������
   */
    private String type;

    /**
   * ����
   */
    private String name;

    /**
   * ���������д���һ�����Դ
   * @param is ������
   * @param type ��������
   * @param name ����
   */
    public ByteArrayDataSource(InputStream is, String type, String name) {
        this.type = type;
        this.name = name;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int ch;
            while ((ch = is.read()) != -1) os.write(ch);
            data = os.toByteArray();
        } catch (IOException ioex) {
        }
    }

    /**
   * �Ӷ����������д������Դ
   * @param data ����������
   * @param type ��������
   * @param name ����
   */
    public ByteArrayDataSource(byte[] data, String type, String name) {
        this.data = data;
        this.type = type;
        this.name = name;
    }

    /**
   * ��string�д������Դ
   * @param data String
   * @param type ��������
   * @param name ����
   */
    public ByteArrayDataSource(String data, String type, String name) {
        this.type = type;
        this.name = name;
        try {
            this.data = data.getBytes(DEFAULTENCODING);
        } catch (UnsupportedEncodingException uex) {
        }
    }

    /**
   * �Ӷ����������еõ�������
   * @return ByteArrayInputStream ����������������
   * @throws IOException
   */
    public InputStream getInputStream() throws IOException {
        if (data == null) throw new IOException("no data available");
        return new ByteArrayInputStream(data);
    }

    /**
   * N/A
   * @return �����
   * @throws IOException
   */
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("N/A");
    }

    /**
   * �õ���������
   * @return ��������
   */
    public String getContentType() {
        return this.type;
    }

    /**
   * �õ����
   * @return ����
   */
    public String getName() {
        return this.name;
    }
}
