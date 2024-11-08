package ru.susu.algebra.ranks;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import ru.susu.algebra.number.MapPrimeNumbersProvider;
import ru.susu.algebra.util.LRUHashMap;

/**
 * @author akargapolov
 * @since: 21.03.2011
 */
public class FullDPRanksCounterPostgreSQLNewNew {

    private static Logger _log = Logger.getLogger("ranks");

    public static int NUMBER = 400;

    public static int DEPTH_SAVE = 2;

    private static Map<Integer, BitSet> _factors = new NumbersBitSetFactorizationProvider().performOperation(NUMBER);

    private static Map<Integer, Integer> _primes = new MapPrimeNumbersProvider().performOperation(NUMBER);

    private static int[][][] _dp = new int[NUMBER + 1][NUMBER + 1][4];

    private static Connection _connection;

    private static LRUHashMap<String, BigInteger> _cache = new LRUHashMap<String, BigInteger>(10000000);

    private static Set<String> _data2Save = Sets.newHashSet();

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        NumberFormat doubleFormat = new DecimalFormat();
        doubleFormat.setMaximumFractionDigits(5);
        for (int i = 1; i <= NUMBER; i++) {
            Date start = new Date();
            _readOps = _writeOps = _readMS = _writeMS = _readLines = 0;
            _connection = getConnection();
            BigInteger rank = recCounter(i, 1, 0, new BitSet(_primes.size()), i % DEPTH_SAVE, 0);
            if (i % 10 == 0) System.gc();
            flush();
            _connection.commit();
            _connection.close();
            _log.info("cache size - " + _cache.size());
            _log.info("rank[" + i + "] - " + rank);
            _log.info("total - " + Runtime.getRuntime().totalMemory() / 1000000);
            _log.info("free - " + Runtime.getRuntime().freeMemory() / 1000000);
            _log.info("used - " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000);
            _log.info("time - " + (new Date().getTime() - start.getTime()) + "ms");
            _log.info("Read - " + _readOps + " operations " + _readMS + " ms " + _readLines + " lines " + doubleFormat.format((((double) _readMS) / _readLines)) + " per line");
            _log.info("Write - " + _writeOps + " operations " + _writeMS + " ms");
        }
        _log.info("finished");
        Thread.sleep(120000);
    }

    protected static BigInteger recCounter(int i, int k, int shift, BitSet bitset, int depthSave, int depth) throws Exception {
        BigInteger result = BigInteger.ZERO;
        if (i == 0 && shift == 0) {
            if (!bitset.isEmpty()) result = BigInteger.ONE;
        } else if (k <= i) {
            List<String> keys = Lists.newArrayList();
            for (int j = k; j <= i; j += 2) if (recDP(i - j, j + 2, (shift + 1 - j + 4 * i) % 4) == 1) {
                BitSet tmp = (BitSet) bitset.clone();
                tmp.xor(_factors.get(j));
                keys.add(getKey(i - j, j + 2, (shift + 1 - j + 4 * i) % 4, tmp));
            }
            Map<String, BigInteger> data = getData(keys, depthSave, depth);
            int index = 0;
            if (depthSave == 0 || depth == 0) {
                for (int j = k; j <= i; j += 2) if (recDP(i - j, j + 2, (shift + 1 - j + 4 * i) % 4) == 1) {
                    BitSet tmp = (BitSet) bitset.clone();
                    tmp.xor(_factors.get(j));
                    String key = keys.get(index++);
                    if (data.get(key) == null) {
                        _data2Save.add(key);
                    }
                }
            }
            index = 0;
            for (int j = k; j <= i; j += 2) if (recDP(i - j, j + 2, (shift + 1 - j + 4 * i) % 4) == 1) {
                BitSet tmp = (BitSet) bitset.clone();
                tmp.xor(_factors.get(j));
                String key = keys.get(index++);
                BigInteger value = data.get(key);
                if (value == null) {
                    value = recCounter(i - j, j + 2, (shift + 1 - j + 4 * i) % 4, tmp, (depthSave + 1) % DEPTH_SAVE, depth + 1);
                }
                result = result.add(value);
            }
        }
        String key = getKey(i, k, shift, bitset);
        if ((depthSave == 1 || depth == 1) && (_data2Save.contains(key) || getData(i, k, shift, bitset) == null)) {
            saveData(i, k, shift, bitset, result);
            _data2Save.remove(key);
        }
        _cache.put(key, result);
        return result;
    }

    private static int recDP(int n, int k, int shift) {
        if (n == 0) {
            if (shift == 0) return 1; else return -1;
        }
        if (k > n) return -1;
        if (_dp[n][k][shift] != 0) return _dp[n][k][shift];
        int res = -1;
        if (recDP(n - k, k + 2, (shift + 1 - k + 4 * 1000) % 4) == 1 || recDP(n, k + 2, shift) == 1) res = 1;
        _dp[n][k][shift] = res;
        return res;
    }

    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/alternating_ranks", "postgres", "1234");
            connection.setAutoCommit(true);
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
    }

    protected static BigInteger getData(int i, int k, int shift, BitSet bitset) throws Exception {
        String key = getKey(i, k, shift, bitset);
        if (_cache.containsKey(key)) return _cache.get(key);
        if (_writeBuffer.containsKey(key)) return (BigInteger) _writeBuffer.get(key)[2];
        BigInteger value = null;
        PreparedStatement psmt = _connection.prepareStatement("SELECT bvalue FROM tbl_data_new_new where key = ?");
        psmt.setString(1, key);
        ResultSet rs = psmt.executeQuery();
        if (rs.next()) {
            value = new BigInteger(rs.getString(1));
            _cache.put(key, value);
        }
        rs.close();
        psmt.close();
        return value;
    }

    private static long _readMS = 0;

    private static long _readLines = 0;

    private static long _writeMS = 0;

    private static long _readOps = 0;

    private static long _writeOps = 0;

    protected static Map<String, BigInteger> getData(List<String> keys, int depthSave, int depth) throws Exception {
        Map<String, BigInteger> result = Maps.newHashMap();
        ArrayList<String> filteredKeys = Lists.newArrayList();
        for (String key : keys) {
            if (_cache.containsKey(key)) {
                result.put(key, _cache.get(key));
            } else if (_writeBuffer.containsKey(key)) {
                result.put(key, (BigInteger) _writeBuffer.get(key)[1]);
            } else {
                filteredKeys.add(key);
            }
        }
        if (!filteredKeys.isEmpty() && (depthSave == 0 || depth == 0)) {
            Date start = new Date();
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < filteredKeys.size(); index++) {
                builder.append((index == 0) ? "(" : ",");
                builder.append("?");
            }
            PreparedStatement psmt = _connection.prepareStatement("SELECT key, bvalue FROM tbl_data_new_new where key in " + builder.toString() + ")");
            for (int index = 0; index < filteredKeys.size(); index++) {
                psmt.setString(index + 1, filteredKeys.get(index));
            }
            ResultSet rs = psmt.executeQuery();
            while (rs.next()) {
                String key = rs.getString(1);
                BigInteger value = new BigInteger(rs.getString(2));
                result.put(key, value);
                _cache.put(key, value);
            }
            rs.close();
            psmt.close();
            _readMS += (new Date().getTime() - start.getTime());
            _readOps++;
            _readLines += filteredKeys.size();
        }
        return result;
    }

    protected static int getIndex(int i, int k, int shift) {
        return i * 10000 * 10 + k * 10 + shift;
    }

    private static String DELIMITER = "_";

    protected static String getKey(int i, int k, int shift, BitSet bitset) {
        return new StringBuilder().append(i).append(DELIMITER).append(k).append(DELIMITER).append(shift).append(DELIMITER).append(bitset).toString();
    }

    private static Map<String, Object[]> _writeBuffer = Maps.newHashMap();

    private static int BATCH_SIZE = 10000;

    protected static void saveData(int i, int k, int shift, BitSet bitset, BigInteger value) throws Exception {
        String key = i + "_" + k + "_" + shift + "_" + bitset;
        _writeBuffer.put(key, new Object[] { key, value });
        _cache.put(key, value);
        if (_writeBuffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    protected static void flush() throws Exception {
        if (_writeBuffer.isEmpty()) {
            return;
        }
        Date start = new Date();
        Connection writeConnection = getConnection();
        PreparedStatement psmt = writeConnection.prepareStatement("insert into tbl_data_new_new(key,bvalue) values (?,?)");
        for (Object[] objs : _writeBuffer.values()) {
            psmt.setString(1, objs[0].toString());
            psmt.setString(2, objs[1].toString());
            psmt.addBatch();
        }
        psmt.executeBatch();
        psmt.close();
        _writeBuffer.clear();
        _writeMS += (new Date().getTime() - start.getTime());
        _writeOps++;
        writeConnection.commit();
        writeConnection.close();
    }
}
