import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class Person {

    public static void testSyb(String[] args) throws Exception {
        String path = "D:\\Programs\\Microsoft SQL Server\\MSSQL.1\\MSSQL\\Data\\pdd_log.ldf";
        String path1 = "\\\\.\\D:";
        File f = new File(path1);
        RandomAccessFile _deviceAccessFile = new RandomAccessFile(f, "r");
        FileChannel _deviceAccessChannel = _deviceAccessFile.getChannel();
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(1024);
        _deviceAccessChannel.read(bb);
        System.out.println(bb);
    }

    private String name;

    public boolean equals(Person anotherOne) {
        if (anotherOne == null) {
            return false;
        }
        if (name == null) {
            return anotherOne.name == null;
        }
        return name.equalsIgnoreCase(anotherOne.name);
    }

    @Override
    public String toString() {
        return name;
    }

    public static void main(String[] args) {
        List list = new ArrayList();
        final Person MAN = new Person("TOM");
        Person firstMan = new Person("Tom");
        Person secondMan = new Person("Jerry");
        list.add(firstMan);
        list.add(secondMan);
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
            System.out.println(MAN.equals(list.get(i)));
        }
        System.out.println("--2--" + Person.name1);
        Person2.doSth(null, null);
    }

    public Person(String name) {
        System.out.println("===creating Person Object===");
        this.name = name;
    }

    class Male extends Person {

        public Male() {
            super(null);
            System.out.println("===creating Male Object===");
        }
    }

    private static String name1 = getName();

    private static String name2 = "John";

    private static String getName() {
        return name2;
    }
}

class Person2 {

    public Person2() {
    }

    public Person2(String name) {
    }

    public static void doSth(String str1, String str2) {
        System.out.println("2 String Version");
    }

    public static void doSth(String... strs) {
        System.out.println("String Array Version");
    }
}
