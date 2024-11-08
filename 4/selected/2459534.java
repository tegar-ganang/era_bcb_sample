package com.puppycrawl.tools.checkstyle.coding;

public class InputTooEarlyVariableDeclaration {

    private methodTest1() {
        int i = 3;
        System.err.println("hello");
        System.err.println(i);
        int j;
        j = 4;
        int k = 3;
        k = 9;
        int a, b;
        System.err.println(a + b);
    }

    public int getVersion() {
        String patternOne = "000";
        DecimalFormat format = new DecimalFormat(patternOne);
        ParsePosition parsePosition = new ParsePosition(0);
        return format.parse(version.toString(), parsePosition).intValue();
    }

    public String storeUploadedZip(byte[] zip, String name) {
        List filesToStore = new ArrayList();
        int i = 0;
        ZipInputStream zipIs = new ZipInputStream(new ByteArrayInputStream(zip));
        ZipEntry zipEntry = zipIs.getNextEntry();
        while (zipEntry != null) {
            if (zipEntry.isDirectory() == false) {
                i++;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(zipIs, baos);
                baos.close();
            }
            zipIs.closeEntry();
            zipEntry = zipIs.getNextEntry();
        }
    }

    private void writeGT(List greenTireList, ZipOutputStream out) throws IOException, SomeException {
        byte data[] = new byte[BUFFER];
        String str = "dummy";
        str = str + "dummy";
    }
}
