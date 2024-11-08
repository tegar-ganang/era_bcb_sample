package name.huzhenbo.java.newio;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import java.io.*;
import java.nio.channels.FileChannel;

/**
 * NIO非堵塞应用通常适用用在I/O读写等方面，我们知道，系统运行的性能瓶颈通常在I/O读写，包括对端口和文件的操作上，过去，在打开一个I/O通道后
 * ，read()将一直等待在端口一边读取字节内容，如果没有内容进来，read()也是傻傻的等，这会影响我们程序继续做其他事情，那么改进做法就是开设线
 * 程，让线程去等待，但是这样做也是相当耗费资源的。
 * <p/>
 * java NIO非堵塞技术实际是采取Reactor模式，或者说是Observer模式为我们监察I/O端口，如果有内容进来，会自动通知我们，这样，我们就不必开启多
 * 个线程死等，从外界看，实现了流畅的I/O读写，不堵塞了。
 * <p/>
 * java NIO出现不只是一个技术性能的提高，你会发现网络上到处在介绍它，因为它具有里程碑意义，从JDK1.4开始，Java开始提高性能相关的功能，从而使
 * 得Java在底层或者并行分布式计算等操作上已经可以和C或Perl等语言并驾齐驱。
 */
public class FileChannelTest {

    private FileInputStream is;

    private FileOutputStream os;

    @Before
    public void setup() throws FileNotFoundException {
        is = new FileInputStream("res/input.data");
        os = new FileOutputStream("res/output.data");
    }

    @Test
    public void should_copy_file_using_new_io() throws IOException {
        FileChannel src = is.getChannel();
        assertEquals(0, src.position());
        FileChannel target = os.getChannel();
        long l = src.transferTo(0, src.size(), target);
        assertEquals(l, src.size());
        src.close();
        target.close();
        is.close();
        os.close();
    }

    @After
    public void teardown() {
        new File("res/output.data").delete();
    }
}
