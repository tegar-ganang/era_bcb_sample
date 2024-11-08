package hoplugins.tsforecast;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import plugins.IHOMiniModel;

public class TrainerCurve extends Curve {

    public TrainerCurve(IHOMiniModel ihominimodel) throws SQLException {
        super(ihominimodel);
        readTrainer();
    }

    public double getLeadership(Date d) throws Exception {
        double dRet = -1;
        if (d != null) {
            for (Iterator<Point> i = m_clPoints.iterator(); i.hasNext(); ) {
                Point p = i.next();
                if (p.m_dDate.before(d)) dRet = p.m_dSpirit;
            }
        } else {
            throw new NullPointerException("Given date is null!");
        }
        if (dRet < 0) {
            ErrorLog.writeln("Trainer for " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(d) + " has no leadership!");
            dRet = 0;
        }
        return dRet;
    }

    public double getCurrentLeadership() {
        last();
        return getSpirit();
    }

    private void readTrainer() throws SQLException {
        GregorianCalendar gregoriancalendar = new GregorianCalendar();
        gregoriancalendar.setTime(m_clModel.getBasics().getDatum());
        gregoriancalendar.add(Calendar.WEEK_OF_YEAR, -WEEKS_BACK);
        Timestamp start = new Timestamp(gregoriancalendar.getTimeInMillis());
        int iLeadership = -1;
        int iLastLeadership = -1;
        int iID = -1;
        int iLastID = -1;
        ResultSet resultset = m_clJDBC.executeQuery("select SPIELERID, FUEHRUNG, DATUM from SPIELER " + "where TRAINERTYP <> -1 and DATUM <= '" + start + "' order by DATUM desc");
        try {
            boolean gotInitial = false;
            if (resultset.next()) {
                iLeadership = resultset.getInt("FUEHRUNG");
                iID = iLastID = resultset.getInt("SPIELERID");
                m_clPoints.add(new Point(resultset.getTimestamp("DATUM"), iLeadership, START_TRAINER_PT));
                gotInitial = true;
            }
            resultset = m_clJDBC.executeQuery("select SPIELERID, FUEHRUNG, DATUM from SPIELER " + "where TRAINERTYP <> -1 and DATUM > '" + start + "' and DATUM < '" + m_clModel.getBasics().getDatum() + "' order by DATUM");
            while (resultset.next()) {
                iLeadership = resultset.getInt("FUEHRUNG");
                iID = resultset.getInt("SPIELERID");
                if (!gotInitial) {
                    m_clPoints.add(new Point(resultset.getTimestamp("DATUM"), iLeadership, START_TRAINER_PT));
                    gotInitial = true;
                }
                if (iID != iLastID) {
                    m_clPoints.add(new Point(resultset.getTimestamp("DATUM"), iLeadership, NEW_TRAINER_PT));
                } else if (iLastLeadership != -1 && iLeadership != iLastLeadership) {
                    m_clPoints.add(new Point(resultset.getTimestamp("DATUM"), iLeadership, TRAINER_DOWN_PT));
                }
                iLastLeadership = iLeadership;
                iLastID = iID;
            }
        } catch (Exception e) {
            ErrorLog.writeln("Error reading trainer. Initial time: " + start);
            ErrorLog.write(e);
        }
    }
}
