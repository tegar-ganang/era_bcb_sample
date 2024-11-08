package android.com.abb;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.Assert;

/** Class to abstract the details of extracting and accessing files from the ABB
 * content zip file in the same was files on disk are accessed. TODO: This class
 * is not thread-safe. Extracted files are not cached. */
public class Content {

    public static void initialize(Context context) {
        mAssets = context.getAssets();
        mCacheDir = context.getCacheDir().getAbsolutePath() + "/";
        mResources = context.getResources();
        prepare();
    }

    private static synchronized void prepare() {
        if (cacheIsState()) {
            for (String cache_item : (new File(mCacheDir)).list()) {
                deleteRecursively(cache_item);
            }
            extractAssets("");
        }
    }

    private static boolean cacheIsState() {
        if (!new File(mCacheDir + "epoch.txt").exists()) {
            return true;
        }
        try {
            InputStream asset_input_stream = mAssets.open("epoch.txt");
            writeStreamToFile(asset_input_stream, mCacheDir + "real_epoch.txt");
            asset_input_stream.close();
        } catch (IOException ex) {
            Assert.fail("Could not compute assets epoch: " + ex.toString());
        }
        String[] epoch = readFileLines(mCacheDir + "epoch.txt");
        String[] real_epoch = readFileLines(mCacheDir + "real_epoch.txt");
        if (epoch.length != real_epoch.length) {
            return true;
        }
        for (int line = 0; line < epoch.length; ++line) {
            if (!epoch[line].equals(real_epoch[line])) {
                return true;
            }
        }
        return false;
    }

    private static void extractAssets(String path) {
        String[] assets = null;
        try {
            assets = mAssets.list(path);
        } catch (IOException ex) {
        }
        if (assets != null) {
            for (String asset : assets) {
                String full_path = path + (path.length() > 0 ? "/" : "") + asset;
                String target_path = mCacheDir + full_path;
                try {
                    boolean skip = false;
                    for (String ignore_asset : kIgnoreAssets) {
                        if (full_path.indexOf(ignore_asset) != -1) {
                            skip = true;
                            break;
                        }
                    }
                    if (skip) {
                        continue;
                    }
                    Log.d("Content::prepare", "Extracting asset: " + full_path);
                    (new File((new File(target_path)).getParent())).mkdir();
                    InputStream asset_input_stream = mAssets.open(full_path);
                    writeStreamToFile(asset_input_stream, target_path);
                    asset_input_stream.close();
                } catch (IOException ex) {
                    Log.d("Content::extractAssets", "Failed extracting asset: " + full_path + " to " + target_path);
                }
                extractAssets(full_path);
            }
        }
    }

    private static void deleteRecursively(String path) {
        File path_file = new File(path);
        if (path_file.isDirectory()) {
            for (String child : path_file.list()) {
                deleteRecursively(path + child);
            }
        }
        path_file.delete();
    }

    public static boolean exists(Uri uri) {
        Log.d("Content::exists", uri.toString());
        if (uri.toString().indexOf("file:///android_asset/") == 0) {
            return exists(Uri.parse(uri.toString().replaceFirst("file:///android_asset/", "file://" + mCacheDir)));
        }
        if (uri.getScheme().equals("file")) {
            return (new File(uri.getPath())).exists();
        } else {
            Assert.fail("Content::exists. Bad URI scheme: " + uri.toString());
            return false;
        }
    }

    public static String[] list(Uri uri) {
        Log.d("Content::list", uri.toString());
        if (uri.toString().indexOf("file:///android_asset/") == 0) {
            return list(Uri.parse(uri.toString().replaceFirst("file:///android_asset/", "file://" + mCacheDir)));
        }
        if (uri.getScheme().equals("file")) {
            return (new File(uri.getPath())).list();
        } else {
            Assert.fail("Content::list. Bad URI scheme: " + uri.toString());
            return null;
        }
    }

    public static synchronized String getFilePath(Uri uri) {
        Log.d("Content::getFilePath", uri.toString());
        if (uri.toString().indexOf("file:///android_asset/") == 0) {
            return uri.toString().replaceFirst("file:///android_asset/", mCacheDir);
        }
        if (uri.getScheme().equals("file")) {
            return uri.getPath();
        } else {
            Assert.fail("Content::getFilePath. " + "Bad URI scheme: " + uri.toString());
            return null;
        }
    }

    public static String[] readFileAndSplit(String file_path, String split) {
        try {
            FileReader file_reader = new FileReader(new File(file_path));
            ArrayList<Character> data_array = new ArrayList<Character>();
            while (file_reader.ready()) {
                data_array.add(new Character((char) file_reader.read()));
            }
            String[] raw_tokens = (new String(toPrimative(data_array))).split(split);
            ArrayList<String> tokens = new ArrayList<String>();
            for (String token : raw_tokens) {
                if (token.length() > 0) {
                    tokens.add(token);
                }
            }
            return tokens.toArray(new String[0]);
        } catch (IOException ex) {
            Assert.fail("Could not read file: " + file_path + ": " + ex.toString());
        }
        return new String[0];
    }

    /** This method reads a splits an ASCII text file along any new line
   * boundaries. */
    public static String[] readFileLines(String file_path) {
        return readFileAndSplit(file_path, "\\n");
    }

    /** This method reads a splits an ASCII text file along any white space
   * boundaries. */
    public static String[] readFileTokens(String file_path) {
        return readFileAndSplit(file_path, "\\s");
    }

    public static String[] readUriLines(Uri uri) {
        String file_path = getFilePath(uri);
        return readFileLines(file_path);
    }

    /** Utility method to parse a set of key value tokens (as generated by
   * readFileTokens(...) for example) and to insert the key value pairs into a
   * map. Only values with keys already present in the map are accepted and
   * types are checked. The map may not be changed if no key / value pairs are
   * present to it is expected to contain value defaults. */
    public static TreeMap<String, Object> mergeKeyValueTokensWithMap(String[] tokens, TreeMap<String, Object> map) {
        Assert.assertEquals("Expected an even number of key-values in tokens.", tokens.length % 2, 0);
        for (int key = 0; key < tokens.length; key += 2) {
            String key_string = tokens[key];
            String value_string = tokens[key + 1];
            Assert.assertTrue("Unknown key name: " + key_string, map.containsKey(key_string));
            Object default_value = map.get(key_string);
            if (default_value instanceof String) {
                map.put(key_string, value_string);
            } else if (default_value instanceof Integer) {
                try {
                    map.put(key_string, new Integer(value_string));
                } catch (NumberFormatException ex) {
                    Assert.fail("Error, expected Integer: " + ex.toString());
                }
            } else if (default_value instanceof Float) {
                try {
                    map.put(key_string, new Float(value_string));
                } catch (NumberFormatException ex) {
                    Assert.fail("Error, expected Float: " + ex.toString());
                }
            } else {
                Assert.fail("Unsupported value type: " + default_value.getClass().getName());
            }
        }
        return map;
    }

    static void assertStringNotNone(TreeMap<String, Object> parameters, String parameter) {
        Assert.assertTrue("Parameter " + parameter + " must be specified.", !((String) parameters.get(parameter)).equals("none"));
    }

    static void assertIntegerNotNone(TreeMap<String, Object> parameters, String parameter) {
        Assert.assertTrue("Parameter " + parameter + " must be specified.", ((Integer) parameters.get(parameter)).intValue() != -1);
    }

    private static char[] toPrimative(ArrayList<Character> array_list) {
        char[] result = new char[array_list.size()];
        for (int index = 0; index < array_list.size(); ++index) {
            result[index] = array_list.get(index).charValue();
        }
        return result;
    }

    private static String uriToContentEntry(Uri uri) {
        String content_name = uri.getHost() + uri.getPath();
        if (content_name.length() > 0 && content_name.charAt(0) == '/') {
            content_name = content_name.substring(1);
        }
        return "content_package/" + content_name;
    }

    private static void writeStreamToFile(InputStream input_stream, String output_path) throws IOException {
        (new File(output_path)).createNewFile();
        BufferedOutputStream output_stream = new BufferedOutputStream(new FileOutputStream(output_path), 8 * 1024);
        byte[] buffer = new byte[1024];
        int bytes_read;
        while ((bytes_read = input_stream.read(buffer)) >= 0) output_stream.write(buffer, 0, bytes_read);
        output_stream.close();
    }

    private static String mCacheDir;

    private static AssetManager mAssets;

    private static Resources mResources;

    private static final String[] kIgnoreAssets = { "images/", "sounds/", "webkit/" };
}
