package photospace.beans;

import junit.framework.*;

public class BeansTest extends TestCase {

    public void testCopy() throws Exception {
        TestBean from = new TestBean("from1", "from2", "from3", "from4");
        TestBean to = new TestBean("to1", "to2", "to3", "to4");
        Beans.copy(from, to);
        assertEquals(from.getReadWrite1(), to.getReadWrite1());
        assertEquals(from.getReadWrite2(), to.getReadWrite2());
        assertEquals("to3", to.getReadOnly());
        assertEquals("to4", to.writeOnly);
    }

    public void testMerge() throws Exception {
        TestBean from = new TestBean("from1", "from2", "from3", "from4");
        TestBean to = new TestBean("to1", null, "to3", "to4");
        Beans.merge(from, to);
        assertEquals("to1", to.getReadWrite1());
        assertEquals(from.getReadWrite2(), to.getReadWrite2());
        assertEquals("to3", to.getReadOnly());
        assertEquals("to4", to.writeOnly);
        to = new TestBean("to1", "", "to3", "to4");
        Beans.merge(from, to);
        assertEquals(from.getReadWrite2(), to.getReadWrite2());
    }

    public static class TestBean {

        private String readWrite1;

        private String readWrite2;

        private String readOnly;

        private String writeOnly;

        public TestBean(String readWrite1, String readWrite2, String readOnly, String writeOnly) {
            this.readWrite1 = readWrite1;
            this.readWrite2 = readWrite2;
            this.readOnly = readOnly;
            this.writeOnly = writeOnly;
        }

        public String getReadWrite1() {
            return readWrite1;
        }

        public void setReadWrite1(String s) {
            readWrite1 = s;
        }

        public String getReadOnly() {
            return readOnly;
        }

        public void setWriteOnly(String s) {
            writeOnly = s;
        }

        public String getReadWrite2() {
            return readWrite2;
        }

        public void setReadWrite2(String readWrite2) {
            this.readWrite2 = readWrite2;
        }
    }
}
