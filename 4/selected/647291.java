package cloudSMAP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import org.apache.hadoop.fs.FSDataInputStream;

public class RMAP {

    static final int MAX_SEED_WIDTH = 31;

    static final boolean VERBOSE = true;

    static final long LEAST_SIG_BIT = 1l;

    static final long SECOND_LSB = 1l << 1;

    static final long ALL_BITS_ON = ~0l;

    static final int SIXTY_FOUR = 64;

    static final int MOST_SIG_BIT = 1 << 63;

    static final int alphabet_size = 4;

    public static int base2int(char c) {
        switch(c) {
            case 'A':
                return 0;
            case 'C':
                return 1;
            case 'G':
                return 2;
            case 'T':
                return 3;
            case 'a':
                return 0;
            case 'c':
                return 1;
            case 'g':
                return 2;
            case 't':
                return 3;
        }
        return 4;
    }

    static boolean isvalid(char c) {
        return (base2int(c) != 4);
    }

    static String basename(String filename) {
        String name = (new File(filename)).getName();
        name = name.substring(0, name.indexOf('.'));
        return name;
    }

    static char complement(int i) {
        final int b2c_size = 20;
        final char[] b2c = { 'T', 'N', 'G', 'N', 'N', 'N', 'C', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'A' };
        final char[] b2cl = { 't', 'n', 'g', 'n', 'n', 'n', 'c', 'n', 'n', 'n', 'n', 'n', 'n', 'n', 'n', 'n', 'n', 'n', 'n', 'a' };
        if (i - 'A' >= 0 && i - 'A' < b2c_size) return b2c[i - 'A']; else if (i - 'a' >= 0 && i - 'a' < b2c_size) return b2cl[i - 'a']; else return 'N';
    }

    static String revcomp(String s) {
        String r;
        int len = s.length();
        char re[] = new char[len];
        for (int i = 0; i < len; i++) re[i] = complement(s.charAt(i));
        r = new String(re);
        StringBuffer buf = new StringBuffer(r);
        buf = buf.reverse();
        return buf.toString();
    }

    static boolean is_valid_filename(final String name, final String filename_suffix) {
        int dot = name.lastIndexOf('.');
        final String suffix = name.substring(dot + 1);
        return (suffix == filename_suffix);
    }

    String path_join(final String a, final String b) {
        return a + "/" + b;
    }

    static void read_dir(String dirname, String filename_suffix, Vector<String> filenames) {
        File directory = new File(dirname);
        String filenamelist[] = directory.list();
        for (int i = 0; i < filenamelist.length; i++) {
            if (!is_valid_filename(filenamelist[i], filename_suffix)) filenames.add(filenamelist[i]);
        }
        if (filenames.isEmpty()) System.out.println("no valid files found in: " + dirname);
    }

    static void read_fasta_file(FSDataInputStream inputStream, Vector<String> names, Vector<String> sequences) {
        BufferedReader d = null;
        try {
            d = new BufferedReader(new InputStreamReader(inputStream));
            boolean first_line = true;
            String s = "", l, name = "";
            while ((l = d.readLine()) != null) {
                if (l.charAt(0) == '>') {
                    if (first_line == false && l.length() > 0) {
                        names.add(name);
                        sequences.add(s);
                    } else first_line = false;
                    name = l.substring(1);
                    s = "";
                } else s += l;
            }
            if (!first_line && s.length() > 0) {
                names.add(name);
                sequences.add(s);
            }
        } catch (IOException ioe) {
            System.out.println("Error while closing the stream : " + ioe);
        } finally {
            try {
                if (d != null) {
                    d.close();
                }
            } catch (IOException ioe) {
                System.out.println("Error while closing the stream : " + ioe);
            }
        }
    }

    static void read_filename_file(String filename, Vector<String> filenames) {
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new FileReader(filename));
            String l;
            while ((l = inputStream.readLine()) != null) {
                System.out.println(l);
                filenames.add(l);
            }
        } catch (IOException ioe) {
            System.out.println("Error while closing the stream : " + ioe);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ioe) {
                System.out.println("Error while closing the stream : " + ioe);
            }
        }
    }

    String i2mer(int n, int index) {
        char int2base[] = { 'A', 'C', 'G', 'T', 'N' };
        char[] s = new char[n];
        do {
            --n;
            s[n] = int2base[index % alphabet_size];
            index /= alphabet_size;
        } while (n > 0);
        return s.toString();
    }

    static boolean check_seed(String read, boolean posstrand, int seed_width, long mask, int read_id, int offset, HashMap<Long, Vector<SeedInfo>> seed_hash) {
        long seed_key = 0l;
        boolean good_hit = true;
        for (int k = 0; k < seed_width && good_hit; ++k) if (isvalid(read.charAt(offset + k))) seed_key = (seed_key << 2) + base2int(read.charAt(offset + k)); else good_hit = false;
        if (good_hit) {
            seed_key &= mask;
            Vector<SeedInfo> val = seed_hash.get(seed_key);
            if (val == null) val = new Vector<SeedInfo>();
            val.add(new SeedInfo(read_id, offset, posstrand));
            seed_hash.put(seed_key, val);
            return true;
        }
        return false;
    }

    static void get_read_matches(final Vector<String> reads, final int seed_width, final int seed_increment, HashMap<Long, Vector<SeedInfo>> seed_hash) {
        final long mask = (LEAST_SIG_BIT << 2 * seed_width) - 1;
        for (int i = 0; i < reads.size(); ++i) {
            String read = reads.elementAt(i);
            final int limit = read.length() - seed_width;
            int j = 0;
            for (j = 0; j <= limit; ) if (check_seed(read, true, seed_width, mask, i, j, seed_hash)) j += seed_increment; else ++j;
            if (j != read.length()) check_seed(read, true, seed_width, mask, i, limit, seed_hash);
            read = revcomp(read);
            j = 0;
            for (j = 0; j <= limit; ) if (check_seed(read, false, seed_width, mask, i, j, seed_hash)) j += seed_increment; else ++j;
            if (j != read.length()) check_seed(read, false, seed_width, mask, i, limit, seed_hash);
        }
        Iterator<Map.Entry<Long, Vector<SeedInfo>>> iterator = seed_hash.entrySet().iterator();
        while (iterator.hasNext()) {
            Collections.sort(iterator.next().getValue());
        }
    }

    static long get_filesize(String filename) {
        File f = new File(filename);
        long size = f.length();
        return size;
    }

    static void map_reads(char buffer[], final int buffer_size, final int chrom_id, final int read_width, final int seed_width, final int max_mismatches, final Vector<WordPair> reads, final Vector<WordPair> reads_rc, final HashMap<Long, Vector<SeedInfo>> seed_hash, Vector<ReadInfo> bests) {
        final long small_mask = (LEAST_SIG_BIT << 2 * seed_width) - 1;
        final long big_mask = (-1l >>> (SIXTY_FOUR - read_width));
        final long bad_base_mask = ((LEAST_SIG_BIT << seed_width) - 1) << (read_width - seed_width);
        final long key_update_bit = LEAST_SIG_BIT << (read_width - seed_width);
        long bad_bases = ALL_BITS_ON;
        long bad_bases_reserve = ALL_BITS_ON;
        long seed_key = 0;
        WordPair wp = new WordPair(), wp_reserve = new WordPair(), shifted_wp = new WordPair();
        int chrom_offset = 0;
        int current_base = 0;
        while (current_base != buffer_size) {
            if (buffer[current_base] != '\n') {
                final int base = base2int(buffer[current_base]);
                wp_reserve.shift_reserve(wp);
                wp.shift(base);
                seed_key = wp.update_key(key_update_bit, small_mask, seed_key);
                bad_bases_reserve = ((bad_bases_reserve << 1) + (int) (bad_bases & MOST_SIG_BIT));
                bad_bases = (bad_bases << 1) + ((base == 4) ? 1 : 0);
                if (seed_hash.containsKey(seed_key) && ((bad_base_mask & bad_bases) == 0)) {
                    Iterator<SeedInfo> i = seed_hash.get(seed_key).iterator();
                    wp_reserve.bads = bad_bases_reserve;
                    wp.bads = bad_bases;
                    assert (i.hasNext());
                    SeedInfo si = i.next();
                    wp.combine(wp_reserve, si.shift, shifted_wp);
                    int prev_shift = si.shift;
                    i = seed_hash.get(seed_key).iterator();
                    while (i.hasNext()) {
                        si = i.next();
                        if (prev_shift != si.shift) {
                            wp.combine(wp_reserve, si.shift, shifted_wp);
                            prev_shift = si.shift;
                        }
                        final long score = (si.strand) ? reads.elementAt(si.read).score(shifted_wp, big_mask) : reads_rc.elementAt(si.read).score(shifted_wp, big_mask);
                        if (score <= max_mismatches) {
                            ReadInfo current = bests.elementAt(si.read);
                            if (score <= current.score) {
                                System.out.println("Score = " + score);
                                current.set((int) score, chrom_id, (chrom_offset + 1) - read_width - si.shift, si.strand);
                            }
                        }
                    }
                }
                ++chrom_offset;
            }
            ++current_base;
        }
    }

    static int count(String s, char c) {
        int counter = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) counter++;
        }
        return counter;
    }

    static void swap(Vector<String> s, int i, int j) {
        if (i == j) return;
        String tmp = new String(s.elementAt(i));
        s.set(i, s.elementAt(j));
        s.set(j, tmp);
    }

    static void clean_reads(int max_diffs, int read_width, Vector<String> reads, Vector<String> read_names) {
        int good = 0;
        for (int i = 0; i < reads.size(); ++i) {
            String s = new String(reads.elementAt(i));
            if (s.length() != read_width) {
                if (s.length() < read_width) {
                    System.out.println("Incorrect read width");
                    return;
                } else reads.set(i, s.substring(0, read_width));
            }
            if (count(s, 'N') <= (int) (max_diffs)) {
                swap(reads, good, i);
                swap(read_names, good, i);
                ++good;
            }
        }
    }

    static void sites_to_regions(final Vector<String> chrom, final Vector<String> reads, final Vector<String> read_names, final Vector<ReadInfo> bests, final int max_mismatches, Vector<GenomicRegion> hits) {
        for (int i = 0; i < bests.size(); ++i) if (bests.elementAt(i).unique && bests.elementAt(i).score <= max_mismatches) hits.add(new GenomicRegion(chrom.elementAt(bests.elementAt(i).chrom), bests.elementAt(i).site, bests.elementAt(i).site + reads.elementAt(i).length(), read_names.elementAt(i), bests.elementAt(i).score, (bests.elementAt(i).strand) ? '+' : '-'));
    }

    static void write_non_uniques(String filename, final Vector<String> read_names, final Vector<ReadInfo> bests) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            for (int i = 0; i < read_names.size(); ++i) if (!bests.elementAt(i).unique) {
                writer.write(read_names.elementAt(i));
                writer.newLine();
            }
        } catch (IOException ioe) {
            System.out.println("Error while closing the stream : " + ioe);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ioe) {
                System.out.println("Error while closing the stream : " + ioe);
            }
        }
    }
}
