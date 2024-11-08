package ru.zombator.hpcourse.juc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Кэш для простых чисел. Числа генерируются программой однажды (используя
 * решето Эратосфена), и кладутся в файл, откуда в память загружается только та
 * часть, которая нужна для расчета.
 * 
 * Для быстрого вычисления сумм применяется структура данных <a
 * href="http://e-maxx.ru/algo/segment_tree">дерево отрезков</a>.
 */
public class PrimeCache {

    private static final int SIZE_OF_INT = 4;

    /**
	 * количество считываемых за раз из кэша чисел
	 */
    private static final int BLOCK = 4096;

    /**
	 * дерево сумм для быстрого вычисления сумм элементов
	 */
    ArrayList<BigInt> tree = null;

    /**
	 * кэш активен?
	 */
    boolean isCacheActive;

    /**
	 * @return есть ли файл кэша на диске?
	 */
    private boolean hasCacheFile() {
        File in = new File("primes.bin");
        return in.exists();
    }

    /**
	 * делает всю черную работу по просеиванию целых чисел до 2**31-1 и
	 * нахождению среди них простых (400 мегабайт простых чисел на выходе)
	 */
    private void createPrimesCache() {
        final int n = 2147483647;
        final int sqrtOfN = (int) Math.rint(Math.sqrt(n));
        BitSet isCompound = new BitSet(n);
        int percent = 0;
        int lastPercent = 0;
        System.out.printf("Sifting:   0%%");
        System.out.flush();
        for (int i = 3; i <= sqrtOfN; i += 2) {
            if (!isCompound.get(i - 1)) {
                for (long j = (long) i * (long) i; j <= n; j += i) {
                    if (j % 2 != 0 && !isCompound.get((int) j - 1)) {
                        isCompound.set((int) j - 1, true);
                    }
                }
            }
            percent = i * 100 / (sqrtOfN - 2 + 1);
            if (percent != lastPercent) {
                System.out.printf("\b\b\b\b%3d%%", percent);
                System.out.flush();
            }
            lastPercent = percent;
        }
        System.out.printf("\n");
        System.out.flush();
        percent = 0;
        lastPercent = 0;
        System.out.printf("Writing:   0%%");
        System.out.flush();
        File binFile = new File("primes.bin");
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(binFile);
            ByteBuffer buf2 = ByteBuffer.allocate(SIZE_OF_INT);
            buf2.order(ByteOrder.LITTLE_ENDIAN);
            buf2.putInt(2);
            buf2.rewind();
            stream.getChannel().write(buf2);
            ByteBuffer buf = ByteBuffer.allocate(SIZE_OF_INT * BLOCK);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int numsPut = 0;
            for (long i = 3; i <= n; i += 2) {
                if (!isCompound.get((int) i - 1)) {
                    buf.putInt((int) i);
                    numsPut++;
                    if (numsPut == BLOCK) {
                        buf.rewind();
                        stream.getChannel().write(buf);
                        numsPut = 0;
                        buf.clear();
                    }
                }
                percent = (int) Math.ceil((i * 1.0 / n) * 100.0);
                if (percent != lastPercent) {
                    System.out.printf("\b\b\b\b%3d%%", percent);
                    System.out.flush();
                }
                lastPercent = percent;
            }
            buf.limit(SIZE_OF_INT * numsPut);
            buf.rewind();
            stream.getChannel().write(buf);
            System.out.printf("\n");
            System.out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * читает число из файлового буфера, выданного функцией {@link #primeBuf()}
	 * .
	 */
    private int readCurrentPrime(FileInputStream primeBuf) {
        return readCurrentPrimeBuffered(primeBuf, 1).get();
    }

    /**
	 * возвращает буфер в памяти, содержащий скопированные @p count чисел из
	 * файлового буфера, выданного функцией {@link #primeBuf()};
	 * <code>null</code> в случае ошибки
	 */
    private IntBuffer readCurrentPrimeBuffered(FileInputStream primeBuf, int count) {
        ByteBuffer bytes = ByteBuffer.allocate(SIZE_OF_INT * count);
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        try {
            primeBuf.getChannel().read(bytes);
        } catch (IOException e) {
            return null;
        }
        bytes.rewind();
        return bytes.asIntBuffer();
    }

    /**
	 * @return n-тое, считая с нуля, простое число; <code>null</code> в случае
	 *         ошибки
	 */
    public Integer nThPrime(FileInputStream primeBuf, int n) {
        try {
            primeBuf.getChannel().position(SIZE_OF_INT * n);
            return readCurrentPrime(primeBuf);
        } catch (IOException e) {
            return null;
        }
    }

    /**
	 * @return индексы простых чисел, ближайших к числам @p from и @p to
	 * @require to >= from; <code>null</code> в случае ошибки
	 */
    public Pair<Integer, Integer> nearestPrimeIdx(FileInputStream primeBuf, int from, int to) {
        try {
            primeBuf.getChannel().position(0);
            final int idx_last = (int) primeBuf.getChannel().size() / SIZE_OF_INT - 1;
            int idx_from = binarySearchStream(primeBuf, 0, idx_last, from);
            int idx_to = binarySearchStream(primeBuf, idx_from + 1, idx_last, to);
            return Pair.makePair(idx_from, idx_to);
        } catch (IOException e) {
            return null;
        }
    }

    /**
	 * бинарный поиск числа @p value в файле кэша между элементами номер (считая
	 * с 0) @p first и @p last
	 */
    private int binarySearchStream(FileInputStream primeBuf, int first, int last, int value) {
        int middle;
        int len = last - first;
        int half;
        while (len > 0) {
            half = len >> 1;
            middle = first + half;
            int middle_value = nThPrime(primeBuf, middle);
            if (middle_value < value) {
                first = middle;
                ++first;
                len = len - half - 1;
            } else {
                len = half;
            }
        }
        return first;
    }

    /**
	 * @return количество простых чисел в кэше
	 */
    public int primeCount(FileInputStream primeBuf) {
        if (!isCacheActive) {
            try {
                primeBuf.getChannel().position(0);
                int size = (int) primeBuf.getChannel().size();
                int count = size / SIZE_OF_INT;
                return count;
            } catch (IOException e) {
                return 0;
            }
        } else {
            return tree.size() / 4;
        }
    }

    /**
	 * @return сумма простых чисел, заключенных между заданными индексами в
	 *         списке простых чисел. индексы считаются с нуля.
	 */
    public BigInt sumOfPrimes(FileInputStream primeBuf, int nStart, int nEnd) {
        assert (isCacheActive);
        return treeSum(1, 0, primeCount(primeBuf) - 1, nStart, nEnd);
    }

    /**
	 * Возвращает новый указатель на открытый в режиме чтения файл кэша;
	 * <code>null</code> &mdash; если файл открыть не удалось
	 */
    public FileInputStream primeBuf() {
        File primesFile = new File("primes.bin");
        FileInputStream buf;
        try {
            buf = new FileInputStream(primesFile);
        } catch (FileNotFoundException e) {
            return null;
        }
        return buf;
    }

    /**
	 * Загружает кэш. Если кэш не существует, он будет создан
	 * 
	 * @p from - минимальное загружаемое в кэш число
	 * @p to - максимальное
	 */
    public PrimeCache(int from, int to) {
        if (!hasCacheFile()) {
            System.out.println("It looks like you are running this program for the first time.");
            System.out.println("Cache of prime numbers up to 2^31-1 will be created.");
            System.out.println("It will take ~400 Mb of disk space.");
            System.out.println("You can have a nice coffee while the generator works.");
            createPrimesCache();
            System.out.println("Cache successfully generated. Thank you for your patience.");
        }
        readCache(from, to);
        isCacheActive = true;
    }

    private void buildTree(List<Integer> inArray, int v, int tl, int tr) {
        if (tl == tr) {
            tree.set(v, new BigInt(inArray.get(tl)));
        } else {
            int tm = (tl + tr) >> 1;
            buildTree(inArray, v << 1, tl, tm);
            buildTree(inArray, (v << 1) + 1, tm + 1, tr);
            tree.set(v, tree.get(v << 1).add(tree.get((v << 1) + 1)));
        }
    }

    private BigInt treeSum(int v, int tl, int tr, int l, int r) {
        if (l > r) return new BigInt(0);
        if (l == tl && r == tr) return tree.get(v);
        int tm = (tl + tr) / 2;
        return treeSum(v << 1, tl, tm, l, Math.min(r, tm)).add(treeSum((v << 1) + 1, tm + 1, tr, Math.max(l, tm + 1), r));
    }

    private void makeCacheTree(List<Integer> inArray) {
        int cache_size = inArray.size();
        int maxCapacity = cache_size * 4;
        tree = new ArrayList<BigInt>();
        for (int i = 0; i < maxCapacity; i++) tree.add(new BigInt(0));
        buildTree(inArray, 1, 0, cache_size - 1);
    }

    private void readCache(int from, int to) {
        FileInputStream stream = null;
        try {
            stream = primeBuf();
            Pair<Integer, Integer> nearest = nearestPrimeIdx(stream, from, to);
            int nearestFrom = nearest.first;
            int nearestTo = nearest.second;
            System.out.printf("Reading prime cache...   0%%");
            int cacheSize = nearestTo - nearestFrom + 1;
            ArrayList<Integer> inArray = new ArrayList<Integer>(cacheSize);
            int percent = 0;
            int lastPercent = 0;
            stream.getChannel().position(SIZE_OF_INT * nearestFrom);
            int availCount = cacheSize;
            for (int i = 0; i < cacheSize; i += BLOCK) {
                int readCount = BLOCK < availCount ? BLOCK : availCount;
                IntBuffer block = readCurrentPrimeBuffered(stream, readCount);
                for (int p = 0; p < readCount; p++) {
                    inArray.add(block.get());
                }
                percent = (int) Math.ceil((i + readCount) * 100.0 / cacheSize);
                if (percent != lastPercent) {
                    System.out.printf("\b\b\b\b%3d%%", percent);
                    System.out.flush();
                }
                lastPercent = percent;
                availCount -= BLOCK;
            }
            System.out.printf("\nTotal primes in prime cache: %d\n", cacheSize);
            System.out.flush();
            System.out.printf("Building in-memory tree of prime sums... ");
            System.out.flush();
            makeCacheTree(inArray);
            inArray = null;
            System.out.printf("done.\n");
            System.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

;
