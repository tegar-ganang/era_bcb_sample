package util;

import it.gallo.snarli.wrapper.Approx;
import it.gallo.snarli.wrapper.MLPApprox;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import net.updater.HistoricalTables;
import org.jfree.data.time.TimeSeries;

public class NeuralProcedures {

    private static final String insertNet = "INSERT INTO NEURAL(net, azione_id, indexes,output, da, a, descrizione ) VALUES(?,?,?,?,?,?,?)";

    private static final String insertNetIndex = "INSERT INTO NEURAL_INDICI(indici_id,neural_id) VALUES(?,?)";

    private static final String selectByQuote = "SELECT net,indexes FROM NEURAL WHERE azione_id=? and output=?";

    public static void insert(Connection c, MLPApprox net, int azioneId, String descrizione, int[] indiciID, int output, Date from, Date to) throws SQLException {
        try {
            PreparedStatement ps = c.prepareStatement(insertNet, PreparedStatement.RETURN_GENERATED_KEYS);
            ArrayList<Integer> indexes = new ArrayList<Integer>(indiciID.length);
            for (int i = 0; i < indiciID.length; i++) indexes.add(indiciID[i]);
            ps.setObject(1, net);
            ps.setInt(2, azioneId);
            ps.setObject(3, indexes);
            ps.setInt(4, output);
            ps.setDate(5, from);
            ps.setDate(6, to);
            ps.setString(7, descrizione);
            ps.executeUpdate();
            ResultSet key = ps.getGeneratedKeys();
            if (key.next()) {
                int id = key.getInt(1);
                for (int i = 0; i < indiciID.length; i++) {
                    PreparedStatement psIndex = c.prepareStatement(insertNetIndex);
                    psIndex.setInt(1, indiciID[i]);
                    psIndex.setInt(2, id);
                    psIndex.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                c.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
                throw e1;
            }
            throw e;
        }
    }

    public static NeuralQuote[] getByQuote(Connection c, int azioneID, int forward) throws SQLException {
        ArrayList<NeuralQuote> list = new ArrayList<NeuralQuote>();
        PreparedStatement ps = c.prepareStatement(selectByQuote);
        ps.setInt(1, azioneID);
        ps.setInt(2, forward);
        ResultSet rs = ps.executeQuery();
        ObjectInputStream ois;
        while (rs.next()) {
            NeuralQuote nq = new NeuralQuote();
            try {
                ois = new ObjectInputStream(rs.getBlob(1).getBinaryStream());
                nq.setNet((MLPApprox) ois.readObject());
                ois = new ObjectInputStream(rs.getBlob(2).getBinaryStream());
                nq.setIndexes((ArrayList<Integer>) ois.readObject());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            list.add(nq);
        }
        NeuralQuote[] out = new NeuralQuote[list.size()];
        return list.toArray(out);
    }

    public static HashMap<Date, Double>[] getSeries(Connection c, Approx net, ArrayList<Integer> indexes, String azioneNome, Date min, Date max) throws SQLException {
        HashMap<Date, Double> azSeries = StoricoProcedures.series(c, HistoricalTables.STORICO, azioneNome, min, max);
        HashMap<Date, Double>[] indici = new HashMap[indexes.size()];
        for (int i = 0; i < indici.length; i++) {
            String nome = IndiciProcedures.getNome(c, indexes.get(i));
            indici[i] = IndiciProcedures.series(c, nome, min, max);
        }
        Date[] keys = new Date[indici[0].keySet().size()];
        keys = indici[0].keySet().toArray(keys);
        Arrays.sort(keys);
        int i = 0;
        while (!azSeries.containsKey(keys[i])) {
            System.err.println(keys[i]);
            i++;
        }
        double[] in = new double[indexes.size() + 1];
        Arrays.fill(in, 0.0);
        HashMap<Date, Double>[] out = new HashMap[net.test(in).length];
        for (int j = 0; j < out.length; j++) {
            out[j] = new HashMap<Date, Double>();
        }
        for (; i < keys.length; i++) {
            in = new double[indexes.size() + 1];
            for (int j = 0; j < in.length - 1; j++) {
                in[j] = indici[j].get(keys[i]);
            }
            in[in.length - 1] = azSeries.get(keys[i]);
            if (azSeries.containsKey(keys[i])) {
                for (int j = 0; j < out.length; j++) {
                    if (i + 1 + j < keys.length) out[j].put(keys[i + j + 1], net.test(in)[j]); else out[j].put(new Date(keys[i].getTime() + (j + 1) * DateUtil.DAY), net.test(in)[j]);
                }
            }
        }
        return out;
    }

    public static TimeSeries[] previsioni(Connection c, String azione, NeuralQuote nq, Date from) throws SQLException {
        int azioneID = GeneralProcedures.getAzioneID(c, azione);
        ArrayList<Integer> list = nq.getIndexes();
        Date max = new Date(System.currentTimeMillis());
        HashMap<Date, Double>[] map = NeuralProcedures.getSeries(c, nq.getNet(), nq.getIndexes(), azione, from, max);
        TimeSeries[] out = new TimeSeries[map.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = DateUtil.createTimeSeries("previsione di " + azione + " + " + (i + 1) + " giorni", map[i]);
        }
        return out;
    }
}
