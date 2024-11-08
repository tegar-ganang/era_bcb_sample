package org.ibex.util.nestedvm;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.ibex.nestedvm.Runtime;
import org.ibex.util.InputStreamToByteArray;
import org.ibex.util.KnownLength;

public class MSPack {

    private static byte[] image;

    private String[] fileNames;

    private int[] lengths;

    private byte[][] data;

    public static class MSPackException extends IOException {

        public MSPackException(String s) {
            super(s);
        }
    }

    public MSPack(InputStream cabIS) throws IOException {
        try {
            Runtime vm = (Runtime) Class.forName("org.ibex.util.MIPSApps").newInstance();
            byte[] cab = InputStreamToByteArray.convert(cabIS);
            int cabAddr = vm.sbrk(cab.length);
            if (cabAddr < 0) throw new MSPackException("sbrk failed");
            vm.copyout(cab, cabAddr, cab.length);
            vm.setUserInfo(0, cabAddr);
            vm.setUserInfo(1, cab.length);
            int status = vm.run(new String[] { "mspack" });
            if (status != 0) throw new MSPackException("mspack.mips failed (" + status + ")");
            int filesTable = vm.getUserInfo(2);
            int count = 0;
            while (vm.memRead(filesTable + count * 12) != 0) count++;
            fileNames = new String[count];
            data = new byte[count][];
            lengths = new int[count];
            for (int i = 0, addr = filesTable; i < count; i++, addr += 12) {
                int length = vm.memRead(addr + 8);
                data[i] = new byte[length];
                lengths[i] = length;
                fileNames[i] = vm.cstring(vm.memRead(addr));
                System.out.println("" + fileNames[i]);
                vm.copyin(vm.memRead(addr + 4), data[i], length);
            }
        } catch (Runtime.ExecutionException e) {
            e.printStackTrace();
            throw new MSPackException("mspack.mips crashed");
        } catch (Exception e) {
            throw new MSPackException(e.toString());
        }
    }

    public String[] getFileNames() {
        return fileNames;
    }

    public int[] getLengths() {
        return lengths;
    }

    public InputStream getInputStream(int index) {
        return new KnownLength.KnownLengthInputStream(new ByteArrayInputStream(data[index]), data[index].length);
    }

    public InputStream getInputStream(String fileName) {
        for (int i = 0; i < fileNames.length; i++) {
            if (fileName.equalsIgnoreCase(fileNames[i])) return getInputStream(i);
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        MSPack pack = new MSPack(new FileInputStream(args[0]));
        String[] files = pack.getFileNames();
        for (int i = 0; i < files.length; i++) System.out.println(i + ": " + files[i] + ": " + pack.getLengths()[i]);
        System.out.println("Writing " + files[files.length - 1]);
        InputStream is = pack.getInputStream(files.length - 1);
        OutputStream os = new FileOutputStream(files[files.length - 1]);
        int n;
        byte[] buf = new byte[4096];
        while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
        os.close();
        is.close();
    }
}
