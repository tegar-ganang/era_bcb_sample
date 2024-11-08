package org.leeing.hadoop.fs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

/**
 * Better scheme than static method.
 * @date Apr 1, 2011
 * @author leeing
 */
public class FileSystemCat {

    public static void main(String[] args) throws IOException {
        String uri = "hdfs://localhost:8020/user/leeing/maxtemp/sample.txt";
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(uri), conf);
        InputStream in = null;
        try {
            in = fs.open(new Path(uri));
            IOUtils.copyBytes(in, System.out, 8192, false);
        } finally {
            IOUtils.closeStream(in);
        }
    }
}
