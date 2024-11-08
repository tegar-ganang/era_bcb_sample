package command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import javax.xml.bind.JAXBException;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;

public class Command {

    private long timeout = 10000;

    private String commandContent;

    private String correctReturnValue;

    private String actualReturnValue;

    private Channel channel;

    private long readInterval = 5000;

    private String successMsg;

    public String getSuccessMsg() {
        return successMsg;
    }

    public void setSuccessMsg(String successMsg) {
        this.successMsg = successMsg;
    }

    public long getReadInterval() {
        return readInterval;
    }

    public void setReadInterval(long readInterval) {
        this.readInterval = readInterval;
    }

    public String getCorrectReturnValue() {
        return correctReturnValue;
    }

    public void setCorrectReturnValue(String correctReturnValue) {
        this.correctReturnValue = correctReturnValue;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getCommandContent() {
        return commandContent;
    }

    public void setCommandContent(String commandContent) {
        this.commandContent = commandContent;
    }

    public String getActualReturnValue() {
        return actualReturnValue;
    }

    public void setActualReturnValue(String actualReturnValue) {
        this.actualReturnValue = actualReturnValue;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
	 * 
	 * send command and get command return value
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws JSchException 
	 */
    public void send() throws IOException, InterruptedException, JSchException {
        InputStream in = new PipedInputStream();
        this.channel.setInputStream(in);
        PipedOutputStream pout = new PipedOutputStream((PipedInputStream) in);
        pout.write(this.commandContent.getBytes());
        pout.flush();
        this.channel.connect();
        OutputStream out = new PipedOutputStream();
        this.channel.setOutputStream(out);
        PipedInputStream pin = new PipedInputStream((PipedOutputStream) out);
        long totaltime = this.timeout;
        ArrayList byteArrayList = new ArrayList();
        while (true) {
            if (totaltime - this.readInterval < 0) {
                System.out.println("read timeout");
                break;
            }
            Thread.sleep(this.readInterval);
            totaltime = totaltime - this.readInterval;
            int byteLength = pin.available();
            System.out.println("byteLength:" + byteLength);
            if (byteLength == 0) {
                continue;
            } else {
                byte[] temp = new byte[byteLength];
                int n = pin.read(temp);
                byteArrayList.add(temp.clone());
                String value = new String(Command.combine(byteArrayList));
                System.out.println("value:" + value);
                this.actualReturnValue = value;
                if (n == -1) {
                    break;
                }
                if (value == this.correctReturnValue) {
                    break;
                }
            }
        }
        System.out.println(this.commandContent + "end");
    }

    /**
	 * 
	 * combine byteArray in ArrayList
	 *
	 * @param byteArrayList
	 * @return
	 */
    private static byte[] combine(ArrayList byteArrayList) {
        int size = 0;
        for (int i = 0; i < byteArrayList.size(); i++) {
            byte[] temp = (byte[]) byteArrayList.get(i);
            size = size + temp.length;
        }
        byte[] array = new byte[size];
        int pos = 0;
        for (int i = 0; i < byteArrayList.size(); i++) {
            byte[] temp = (byte[]) byteArrayList.get(i);
            System.arraycopy(temp, 0, array, pos, temp.length);
            pos = pos + temp.length;
        }
        return array;
    }

    /**
	 * 
	 * check execute command is successful or not
	 *
	 * @return
	 */
    public boolean isSuccess() {
        boolean result = false;
        if (this.actualReturnValue != null && this.successMsg != null && this.actualReturnValue.contains(this.successMsg)) {
            result = true;
        }
        return result;
    }

    public static void main(String[] args) throws JAXBException, JSchException, IOException, InterruptedException {
        byte[] array1 = "1233".getBytes();
        byte[] array2 = "4567".getBytes();
        ArrayList list = new ArrayList();
        list.add(array1.clone());
        list.add(array2.clone());
        String str = new String(combine(list));
        System.out.println(str);
    }
}
