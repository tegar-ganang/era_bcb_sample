package jdc.util;

import java.io.*;

public class jhe3 {

    public static final String appname = "jhe3";

    public static final String enc_mode = "encoding";

    public static final String dec_mode = "decoding";

    public static final String enc_extension = ".he3";

    public static final String dec_extension = ".decoded";

    private static long file_size = 0L;

    public jhe3() {
    }

    public static void _he3Encode(String in_file) {
        try {
            File out = new File(in_file + enc_extension);
            File in = new File(in_file);
            file_size = in.length();
            FileInputStream in_stream = new FileInputStream(in_file);
            out.createNewFile();
            FileOutputStream out_stream = new FileOutputStream(out.getName());
            InputStreamReader inputReader = new InputStreamReader(in_stream, "ISO8859_1");
            OutputStreamWriter outputWriter = new OutputStreamWriter(out_stream, "ISO8859_1");
            System.out.println(appname + ".\n" + enc_mode + ": " + in_file + "\n" + enc_mode + " to: " + in_file + enc_extension + "\n");
            System.out.print("\n" + enc_mode + ": ");
            _encode(inputReader, outputWriter);
            System.out.print("complete\n\n");
        } catch (java.io.FileNotFoundException fnfEx) {
            System.err.println("Exception: " + fnfEx.getMessage());
        } catch (java.io.IOException ioEx) {
            System.err.println("Exception: " + ioEx.getMessage());
        }
    }

    private static void _encode(InputStreamReader in, OutputStreamWriter out) {
        try {
            char list_contents[] = new char[(int) file_size];
            byte b_list_contents[] = new byte[(int) file_size];
            int nr_of_chars_read = in.read(list_contents);
            jdc.util.Encoder encoder = new jdc.util.Encoder();
            for (int i = 0; i < b_list_contents.length; i++) b_list_contents[i] = (byte) list_contents[i];
            String encoded_file = encoder.encode(b_list_contents);
            byte encoded_file_bytes[] = encoded_file.getBytes("ISO-8859-1");
            for (int i = 0; i < list_contents.length; i++) list_contents[i] = (char) encoded_file_bytes[i];
            out.write(list_contents, 0, (int) file_size);
            out.flush();
            out.close();
        } catch (java.io.IOException ioEx) {
            System.err.println("Exception: " + ioEx.getMessage());
        }
    }

    public static void _he3Decode(String in_file) {
        try {
            File out = new File(in_file + dec_extension);
            File in = new File(in_file);
            int file_size = (int) in.length();
            FileInputStream in_stream = new FileInputStream(in_file);
            out.createNewFile();
            FileOutputStream out_stream = new FileOutputStream(out.getName());
            InputStreamReader inputReader = new InputStreamReader(in_stream, "ISO8859_1");
            OutputStreamWriter outputWriter = new OutputStreamWriter(out_stream, "ISO8859_1");
            ByteArrayOutputStream os = new ByteArrayOutputStream(file_size);
            byte byte_arr[] = new byte[8];
            char char_arr[] = new char[8];
            int buff_size = char_arr.length;
            int _fetched = 0;
            int _chars_read = 0;
            System.out.println(appname + ".\n" + dec_mode + ": " + in_file + "\n" + dec_mode + " to: " + in_file + dec_extension + "\n" + "\nreading: ");
            while (_fetched < file_size) {
                _chars_read = inputReader.read(char_arr, 0, buff_size);
                if (_chars_read == -1) break;
                for (int i = 0; i < _chars_read; i++) byte_arr[i] = (byte) char_arr[i];
                os.write(byte_arr, 0, _chars_read);
                _fetched += _chars_read;
                System.out.print("*");
            }
            System.out.print("\n" + dec_mode + ": ");
            outputWriter.write(new String(_decode((ByteArrayOutputStream) os), "ISO-8859-1"));
            System.out.print("complete\n\n");
        } catch (java.io.FileNotFoundException fnfEx) {
            System.err.println("Exception: " + fnfEx.getMessage());
        } catch (java.io.IOException ioEx) {
            System.err.println("Exception: " + ioEx.getMessage());
        }
    }

    private static byte[] _decode(ByteArrayOutputStream os) {
        jdc.util.Decoder dec = new jdc.util.Decoder();
        String raw_tree = dec.decode(os.toByteArray());
        return raw_tree.getBytes();
    }

    public static void main(String[] args) {
        jhe3 jhe = null;
        if (args.length > 1) {
            if (args[0].equalsIgnoreCase("-d")) _he3Decode(args[1]);
            if (args[0].equalsIgnoreCase("-e")) _he3Encode(args[1]);
        } else System.err.println("\n" + appname + " missing arguments...\n" + "\nUsage: " + appname + " [OPTION] <filename>\n" + "OPTIONS:\n" + " -d decode filename\n" + " -e encode filename\n" + "\n");
    }
}
