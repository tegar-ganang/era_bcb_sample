package org.zkoss.zss.engine.fn;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import org.jfree.date.DateUtilities;
import org.jfree.date.SerialDate;
import org.jfree.date.SerialDateUtilities;
import org.zkoss.xel.XelContext;
import org.zkoss.xel.fn.CommonFns;
import org.zkoss.zss.engine.xel.SSError;
import org.zkoss.zss.engine.xel.SSErrorXelException;

/**
 * Spread Sheet FinancialFns functions
 * 
 * @author ivancheng
 * 
 */
public class FinancialFns {

    protected FinancialFns() {
    }

    /**
	 * Returns the accrued interest for a security that pays periodic interest.
	 * This function implements the ACCRINT(issue,first_interest,settlement,rate,par,frequency,basis,calc_method) spreadsheet function.
	 * @param args the arguments; only args[0](issue), args[1](first_interest), args[2](settlement), args[3](rate), args[4](par), args[5](frequency), args[6](basis), args[7](method) are significant.
	 * @param ctx the function context.
	 * @return the accrued interest for a security that pays periodic interest.
	 */
    public static Object financialAccrint(Object[] args, XelContext ctx) {
        Date issue = UtilFns.stringToDate(args[0].toString());
        Date firI = UtilFns.stringToDate(args[1].toString());
        Date settlement = UtilFns.stringToDate(args[2].toString());
        if (settlement.before(issue)) {
            throw new SSErrorXelException(SSError.NUM);
        }
        double rate = CommonFns.toNumber(args[3]).doubleValue();
        double Par;
        int freq;
        try {
            Par = CommonFns.toNumber(args[4]).doubleValue();
        } catch (ClassCastException e) {
            Par = 10E+3;
        }
        try {
            freq = CommonFns.toNumber(args[5]).intValue();
        } catch (ClassCastException e) {
            throw new SSErrorXelException(SSError.NUM);
        }
        int basis = 0;
        boolean method = true;
        if (args.length == 7) {
            try {
                basis = basis(CommonFns.toNumber(args[6]).intValue());
            } catch (ClassCastException e) {
                basis = 0;
            }
        } else if (args.length == 8) {
            try {
                basis = basis(CommonFns.toNumber(args[6]).intValue());
            } catch (ClassCastException e) {
                basis = 0;
            }
            try {
                method = CommonFns.toBoolean(args[7]);
            } catch (ClassCastException e) {
                method = true;
            }
        }
        if (!(freq == 1 || freq == 2 || freq == 4)) {
            throw new SSErrorXelException(SSError.NUM);
        }
        if (rate <= 0 || Par <= 0) {
            throw new SSErrorXelException(SSError.NUM);
        }
        double result = 0;
        if (method) {
            result = Par * rate * getYearDiff(issue, settlement, basis);
        } else {
            result = Par * rate * getYearDiff(firI, settlement, basis);
        }
        return new Double(result);
    }

    private static double getYearDiff(Date firstDate, Date secondDate, int basis) {
        SerialDate startDate = SerialDate.createInstance(firstDate);
        SerialDate endDate = SerialDate.createInstance(secondDate);
        double result = 0;
        int diff = 0;
        switch(basis) {
            case 0:
                diff = SerialDateUtilities.dayCount30(startDate, endDate);
                if (startDate.getYYYY() == endDate.getYYYY() && startDate.getMonth() == 2 && endDate.getMonth() != 2) {
                    if (SerialDate.isLeapYear(startDate.getYYYY())) {
                        diff -= 1;
                    } else {
                        diff -= 2;
                    }
                }
                result = diff / 360.0;
                break;
            case 1:
                diff = SerialDateUtilities.dayCountActual(startDate, endDate);
                if (SerialDate.isLeapYear(startDate.getYYYY())) {
                    result = diff / 366.0;
                } else {
                    result = (double) diff / 365.0;
                }
                break;
            case 2:
                diff = SerialDateUtilities.dayCountActual(startDate, endDate);
                result = diff / 360.0;
                break;
            case 3:
                diff = SerialDateUtilities.dayCountActual(startDate, endDate);
                result = diff / 365.0;
                break;
            case 4:
                diff = SerialDateUtilities.dayCount30E(startDate, endDate);
                result = diff / 360.0;
                break;
        }
        return result;
    }

    private static int frequency(int frequency) {
        if (frequency != 1 && frequency != 2 && frequency != 4) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            return 12 / frequency;
        }
    }

    private static int basis(int basis) {
        if (basis > 4 || basis < 0) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            return basis;
        }
    }

    /**
	 * Returns the accrued interest for a security that pays interest at maturity.
	 * This function implements the ACCRINTM(issue, settlement, rate, par, basis) spreadsheet function.
	 * @param args the arguments; only args[0](issue), args[1](settlement), args[2](rate), args[3](par), args[4](basis) are significant.
	 * @param ctx the function context.
	 * @return the accrued interest for a security that pays interest at maturity.
	 */
    public static Object financialAccrintm(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[2]).doubleValue();
        double par = CommonFns.toNumber(args[3]).doubleValue();
        if (rate <= 0 || par <= 0) {
            throw new SSErrorXelException(SSError.NUM);
        }
        Date issue = UtilFns.stringToDate(args[0].toString());
        Date settle = UtilFns.stringToDate(args[1].toString());
        int basis = 0;
        if (args.length == 5) {
            basis = basis(CommonFns.toNumber(args[4]).intValue());
        }
        int A = UtilFns.dsm(issue, settle, basis);
        double D = UtilFns.basisToDouble(basis, issue, settle, A);
        double accrintm = par * rate * (A / D);
        return new Double(accrintm);
    }

    /**
	 * Returns the depreciation for each accounting period by using a depreciation coefficient.
	 * This function implements the AMORDEGRC(cost,date_purchased,first_period,salvage,period,rate,basis) spreadsheet function.
	 * @param args the arguments; only args[0](cost), args[1](date_purchased), args[2](first_period), args[3](salvage), args[4](period), args[5](rate), args[6](basis) are significant.
	 * @param ctx the function context.
	 * @return the depreciation for each accounting period by using a depreciation coefficient.
	 */
    public static Object financialAmordegrc(Object[] args, XelContext ctx) {
        double cost = CommonFns.toNumber(args[0]).doubleValue();
        Date purDate = UtilFns.stringToDate(args[1].toString());
        Date firDate = UtilFns.stringToDate(args[2].toString());
        double salvage = CommonFns.toNumber(args[3]).doubleValue();
        int period = CommonFns.toNumber(args[4]).intValue();
        double rate = CommonFns.toNumber(args[5]).doubleValue();
        int basis = 0;
        if (args.length == 7) {
            basis = basis(CommonFns.toNumber(args[6]).intValue());
        }
        double usePeriod = 1.0 / rate;
        double coeff;
        if (usePeriod < 3) {
            coeff = 1.0;
        } else if (usePeriod < 5) {
            coeff = 1.5;
        } else if (usePeriod <= 6) {
            coeff = 2.0;
        } else {
            coeff = 2.5;
        }
        rate *= coeff;
        double nRate = Math.round(yearFraction(purDate, firDate, basis) * rate * cost);
        cost -= nRate;
        double rest = cost - salvage;
        for (int n = 0; n < period; n++) {
            nRate = Math.round(rate * cost);
            rest -= nRate;
            if (rest < 0.0) {
                switch(period - n) {
                    case 0:
                    case 1:
                        return new Double(Math.round(cost * 0.5));
                    default:
                        return new Double(0.0);
                }
            }
            cost -= nRate;
        }
        return new Double(nRate);
    }

    private static double yearFraction(Date d0, Date d1, int basis) {
        SerialDate startDate = SerialDate.createInstance(d0);
        SerialDate endDate = SerialDate.createInstance(d1);
        double result = 0;
        int diff = 0;
        switch(basis) {
            case 0:
                diff = SerialDateUtilities.dayCount30(startDate, endDate);
                result = diff / 360.0;
                break;
            case 1:
                int nYears, nFeb29s;
                double peryear;
                SerialDate tempDate1 = SerialDate.addYears(1, startDate);
                if (endDate.compare(tempDate1) > 0) {
                    nYears = endDate.getYYYY() - startDate.getYYYY() + 1;
                    tempDate1 = SerialDate.createInstance(1, 1, endDate.getYYYY() + 1);
                    SerialDate tempDate2 = SerialDate.createInstance(1, 1, startDate.getYYYY());
                    nFeb29s = tempDate1.compare(tempDate2) - 365 * nYears;
                } else {
                    nYears = 1;
                    if ((SerialDate.isLeapYear(startDate.getYYYY()) && startDate.getMonth() < 3) || SerialDate.isLeapYear(endDate.getYYYY()) && endDate.compare(SerialDate.createInstance(29, 2, endDate.getYYYY())) >= 0) {
                        nFeb29s = 1;
                    } else nFeb29s = 0;
                }
                peryear = 365.0 + (double) nFeb29s / (double) nYears;
                result = (double) endDate.compare(startDate) / peryear;
                break;
            case 2:
                diff = SerialDateUtilities.dayCountActual(startDate, endDate);
                result = diff / 360.0;
                break;
            case 3:
                diff = SerialDateUtilities.dayCountActual(startDate, endDate);
                result = diff / 365.0;
                break;
            case 4:
                diff = SerialDateUtilities.dayCount30E(startDate, endDate);
                result = diff / 360.0;
                break;
        }
        return result;
    }

    /**
	 * Returns the depreciation for each accounting period.
	 * This function implements the AMORLINC(cost,date_purchased,first_period,salvage,period,rate,basis) spreadsheet function.
	 * @param args the arguments; only args[0](cost), args[1](date_purchased), args[2](first_period), args[3](salvage), args[4](period), args[5](rate), args[6](basis) are significant.
	 * @param ctx the function context.
	 * @return the depreciation for each accounting period.
	 */
    public static Object financialAmorlinc(Object[] args, XelContext ctx) {
        double cost = CommonFns.toNumber(args[0]).doubleValue();
        Date purDate = UtilFns.stringToDate(args[1].toString());
        Date firDate = UtilFns.stringToDate(args[2].toString());
        double salvage = CommonFns.toNumber(args[3]).doubleValue();
        int period = CommonFns.toNumber(args[4]).intValue();
        double rate = CommonFns.toNumber(args[5]).doubleValue();
        int basis = 0;
        if (args.length == 7) {
            basis = basis(CommonFns.toNumber(args[6]).intValue());
        }
        double T0 = cost * rate;
        double T1 = yearFraction(purDate, firDate, basis) * T0;
        int numPeriods = (int) (((cost - salvage) - T1) / T0);
        if (period == 0) {
            return new Double(T1);
        } else if (period <= numPeriods) {
            return new Double(T0);
        } else if (period == numPeriods + 1) {
            return new Double((cost - salvage) - T0 * numPeriods - T1);
        } else return new Double(0);
    }

    /**
	 * Returns the number of days from the beginning of the coupon period to the settlement date.
	 * This function implements the COUPDAYBS(settlement,maturity,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](frequency), args[3](basis) are significant.
	 * @param ctx the function context.
	 * @return the number of days from the beginning of the coupon period to the settlement date.
	 */
    public static Object financialCoupdaybs(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        int frequency = frequency(CommonFns.toNumber(args[2]).intValue());
        int basis = 0;
        if (args.length == 4) {
            basis = basis(CommonFns.toNumber(args[3]).intValue());
        }
        return new Double(coupdaybs(settle, maturi, frequency, basis));
    }

    private static double coupdaybs(Date settle, Date maturi, int freq, int basis) {
        Date coupday = couppcd(settle, maturi, freq, basis);
        return UtilFns.dsm(coupday, settle, basis);
    }

    /**
	 * Returns the number of days in the coupon period that contains the settlement date.
	 * This function implements the COUPDAYS(settlement,maturity,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity, args[2](frequency), args[3](basis) are significant.
	 * @param ctx the function context.
	 * @return the number of days in the coupon period that contains the settlement date.
	 */
    public static Object financialCoupdays(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        int frequency = frequency(CommonFns.toNumber(args[2]).intValue());
        int basis = 0;
        if (args.length == 4) {
            basis = basis(CommonFns.toNumber(args[3]).intValue());
        }
        return new Double(coupdays(settle, maturi, frequency, basis));
    }

    private static double coupdays(Date settle, Date maturi, int freq, int basis) {
        Date couppcd = couppcd(settle, maturi, freq, basis);
        Date coupncd = coupncd(couppcd, freq);
        return UtilFns.dsm(couppcd, coupncd, basis);
    }

    /**
	 * Returns the number of days from the settlement date to the next coupon date.
	 * This function implements the COUPDAYSNC(settlement,maturity,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement),args[1](maturity), args[2](frequency), args[3](basis) are significant.
	 * @param ctx the function context.
	 * @return the number of days from the settlement date to the next coupon date.
	 */
    public static Object financialCoupdaysnc(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        int frequency = frequency(CommonFns.toNumber(args[2]).intValue());
        int basis = 0;
        if (args.length == 4) {
            basis = basis(CommonFns.toNumber(args[3]).intValue());
        }
        return new Double(coupdaysnc(settle, maturi, frequency, basis));
    }

    private static double coupdaysnc(Date settle, Date maturi, int freq, int basis) {
        Date couppcd = couppcd(settle, maturi, freq, basis);
        Date coupncd = coupncd(couppcd, freq);
        double coupdaysnc = UtilFns.dsm(settle, coupncd, basis);
        return coupdaysnc;
    }

    /**
	 * Returns a number that represents the next coupon date after the settlement date.
	 * This function implements the COUPNCD(settlement,maturity,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement),args[1](maturity), args[2](frequency), args[3](basis) are significant.
	 * @param ctx the function context.
	 * @return a number that represents the next coupon date after the settlement date.
	 */
    public static Object financialCoupncd(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        int frequency = frequency(CommonFns.toNumber(args[2]).intValue());
        int basis = 0;
        if (args.length == 4) {
            basis = basis(CommonFns.toNumber(args[3]).intValue());
        }
        Date couppcd = couppcd(settle, maturi, frequency, basis);
        Date coupncd = coupncd(couppcd, frequency);
        return coupncd;
    }

    private static Date coupncd(Date coupday, int frequency) {
        GregorianCalendar coupncd = new GregorianCalendar();
        coupncd.setTime(coupday);
        coupncd.add(Calendar.MONTH, frequency);
        return coupncd.getTime();
    }

    /**
	 * Returns the number of coupons payable between the settlement date and maturity date, rounded up to the nearest whole coupon.
	 * This function implements the COUPNUM(settlement,maturity,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement),args[1](maturity), args[2](frequency), args[3](basis) are significant.
	 * @param ctx the function context.
	 * @return the number of coupons payable between the settlement date and maturity date, rounded up to the nearest whole coupon.
	 */
    public static Object financialCoupnum(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        int frequency = frequency(CommonFns.toNumber(args[2]).intValue());
        int basis = 0;
        if (args.length == 4) {
            basis = basis(CommonFns.toNumber(args[3]).intValue());
        }
        return new Double(coupnum(settle, maturi, frequency, basis));
    }

    private static double coupnum(Date settle, Date maturi, int freq, int basis) {
        Date couppcd = couppcd(settle, maturi, freq, basis);
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(couppcd);
        int couppcdY = gc.get(Calendar.YEAR);
        int couppcdM = gc.get(Calendar.MONTH);
        gc.setTime(maturi);
        int maturiY = gc.get(Calendar.YEAR);
        int maturiM = gc.get(Calendar.MONTH);
        return Math.floor(((maturiY - couppcdY) * 12 + maturiM - couppcdM) / freq);
    }

    /**
	 * Returns a number that represents the previous coupon date before the settlement date.
	 * This function implements the COUPPCD(settlement,maturity,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement),args[1](maturity), args[2](frequency), args[3](basis) are significant.
	 * @param ctx the function context.
	 * @return a number that represents the previous coupon date before the settlement date.
	 */
    public static Object financialCouppcd(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        if (settle.after(maturi)) {
            throw new SSErrorXelException(SSError.NUM);
        }
        int frequency = frequency(CommonFns.toNumber(args[2]).intValue());
        int basis = 0;
        if (args.length == 4) {
            basis = basis(CommonFns.toNumber(args[3]).intValue());
        }
        return couppcd(settle, maturi, frequency, basis);
    }

    private static Date couppcd(Date settle, Date maturi, int frequency, int basis) {
        GregorianCalendar couppcd = new GregorianCalendar();
        GregorianCalendar settleD = new GregorianCalendar();
        settleD.setTime(settle);
        couppcd.setTime(maturi);
        couppcd.set(Calendar.YEAR, settleD.get(Calendar.YEAR));
        if (couppcd.after(settleD)) {
            couppcd.add(Calendar.YEAR, -1);
        }
        while (!couppcd.after(settleD)) {
            couppcd.add(Calendar.MONTH, frequency);
        }
        couppcd.add(Calendar.MONTH, -1 * frequency);
        return couppcd.getTime();
    }

    /**
	 * Returns the cumulative interest paid on a loan between start_period and end_period.
	 * This function implements the CUMIPMT((rate,nper,pv,start_period,end_period,type)) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](nper), args[2](pv), args[3](start_period), args[4](end_period), args[5](type) are significant.
	 * @param ctx the function context.
	 * @return the cumulative interest paid on a loan between start_period and end_period.
	 */
    public static Object financialCumipmt(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        int nper = (int) Math.floor(CommonFns.toNumber(args[1]).doubleValue());
        double pv = CommonFns.toNumber(args[2]).doubleValue();
        int startPer = (int) Math.floor(CommonFns.toNumber(args[3]).doubleValue());
        int endPer = (int) Math.floor(CommonFns.toNumber(args[4]).doubleValue());
        if (rate == 0 || nper == 0 || pv == 0 || startPer < 1 || endPer < 1 || startPer > endPer) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            int type = 0;
            if (args.length > 5) {
                type = CommonFns.toNumber(args[5]).intValue();
            }
            if (type != 0 && type != 1) {
                throw new SSErrorXelException(SSError.NUM);
            }
            double cumipmt = 0.0;
            double pmt = pmt(rate, nper, pv, 0.0, type);
            if (startPer == 1) {
                if (type <= 0) {
                    cumipmt = -pv;
                }
                startPer++;
            }
            for (int i = startPer; i <= endPer; i++) {
                if (type > 0) cumipmt += GetZw(rate, (i - 2), pmt, pv, 1) - pv; else cumipmt += GetZw(rate, (i - 1), pmt, pv, 0);
            }
            cumipmt *= rate;
            return new Double(cumipmt);
        }
    }

    /**
	 * Returns the cumulative principal paid on a loan between start_period and end_period.
	 * This function implements the CUMPRINC(rate,nper,pv,start_period,end_period,type) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](nper), args[2](pv), args[3](start_period), args[4](end_period), args[5](type) are significant.
	 * @param ctx the function context.
	 * @return the cumulative principal paid on a loan between start_period and end_period.
	 */
    public static Object financialCumprinc(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        double nper = Math.floor(CommonFns.toNumber(args[1]).doubleValue());
        double pv = CommonFns.toNumber(args[2]).doubleValue();
        int startPer = (int) Math.floor(CommonFns.toNumber(args[3]).doubleValue());
        int endPer = (int) Math.floor(CommonFns.toNumber(args[4]).doubleValue());
        if (rate == 0 || nper == 0 || pv == 0 || startPer < 1 || endPer < 1 || startPer > endPer) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            int type = 0;
            if (args.length > 5) {
                type = CommonFns.toNumber(args[5]).intValue();
            }
            if (type != 0 && type != 1) {
                throw new SSErrorXelException(SSError.NUM);
            }
            double cumppmt = 0.0;
            double pmt = pmt(rate, nper, pv, 0.0, type);
            if (startPer == 1) {
                if (type <= 0) cumppmt = pmt + pv * rate; else cumppmt = pmt;
                startPer++;
            }
            for (int i = startPer++; i <= endPer; i++) {
                if (type > 0) cumppmt += pmt - (GetZw(rate, (i - 2), pmt, pv, 1) - pmt) * rate; else cumppmt += pmt - GetZw(rate, (i - 1), pmt, pv, 0) * rate;
            }
            return new Double(cumppmt);
        }
    }

    /**
	 * Returns the depreciation of an asset for a specified period using the fixed-declining balance method.
	 * This function implements the DB(cost,salvage,life,period,month) spreadsheet function.
	 * @param args the arguments; only args[0](cost), args[1](salvage), args[2](life), args[3](period), args[4](month) are significant.
	 * @param ctx the function context.
	 * @return the depreciation of an asset for a specified period using the fixed-declining balance method.
	 */
    public static Object financialDb(Object[] args, XelContext ctx) {
        double cost = 0;
        double salvage = 0;
        if (args[0] != null) {
            cost = CommonFns.toNumber(args[0]).doubleValue();
            if (cost < 0) {
                throw new SSErrorXelException(SSError.NUM);
            }
        }
        if (args[1] != null) {
            salvage = CommonFns.toNumber(args[1]).doubleValue();
            if (salvage < 0) {
                throw new SSErrorXelException(SSError.NUM);
            }
        }
        if (args[2] == null || args[3] == null) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            double life = CommonFns.toNumber(args[2]).doubleValue();
            double period = CommonFns.toNumber(args[3]).doubleValue();
            if (life < 0 || period < 0) {
                throw new SSErrorXelException(SSError.NUM);
            }
            if (period - 1 > life) {
                throw new SSErrorXelException(SSError.NUM);
            } else {
                double month = 12.0;
                if (args.length == 5) {
                    month = CommonFns.toNumber(args[4]).doubleValue();
                }
                double rate = CommonFns.toNumber(new BigDecimal(1 - Math.pow((salvage / cost), 1 / life)).setScale(3, BigDecimal.ROUND_HALF_UP)).doubleValue();
                return new Double(db(cost, salvage, life, period, month, rate));
            }
        }
    }

    private static double db(double cost, double salvage, double life, double period, double month, double rate) {
        double db = cost * rate * month / 12;
        double d = cost * rate * month / 12;
        if (period > 1) {
            for (int i = 1; i <= period - 1; i++) {
                if (i < life) {
                    db = (cost - d) * rate;
                    d = d + db;
                } else {
                    db = (cost - d) * rate * (12 - month) / 12;
                }
            }
        }
        return db;
    }

    /**
	 * Returns the depreciation of an asset for a specified period using the double-declining balance method or some other method you specify.
	 * This function implements the DDB(cost,salvage,life,period,factor) spreadsheet function.
	 * @param args the arguments; only args[0](cost), args[1](salvage), args[2](life), args[3](period), args[4](factor) are significant.
	 * @param ctx the function context.
	 * @return the depreciation of an asset for a specified period using the double-declining balance method or some other method you specify.
	 */
    public static Object financialDdb(Object[] args, XelContext ctx) {
        double cost = 0;
        double salvage = 0;
        if (args[0] != null) {
            cost = CommonFns.toNumber(args[0]).doubleValue();
            if (cost < 0) {
                throw new SSErrorXelException(SSError.NUM);
            }
        }
        if (args[1] != null) {
            salvage = CommonFns.toNumber(args[1]).doubleValue();
            if (salvage < 0) {
                throw new SSErrorXelException(SSError.NUM);
            }
        }
        if (args[2] == null || args[3] == null) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            double life = CommonFns.toNumber(args[2]).doubleValue();
            double period = CommonFns.toNumber(args[3]).doubleValue();
            if (life < 0 || period < 0) {
                throw new SSErrorXelException(SSError.NUM);
            }
            if (period > life) {
                throw new SSErrorXelException(SSError.NUM);
            } else {
                double factor = 2;
                if (args.length == 5) {
                    factor = CommonFns.toNumber(args[4]).doubleValue();
                }
                return new Double(ddb(cost, salvage, life, period, factor));
            }
        }
    }

    private static double ddb(double cost, double salvage, double life, double period, double factor) {
        double d1 = factor * cost * Math.pow((life - factor) / life, period - 1) / life;
        double d2 = (factor * cost * Math.pow((life - factor) / life, period - 1) / life) - ((1 - Math.pow((life - factor) / life, period)) * cost - (cost - salvage));
        return Math.min(d1, d2);
    }

    /**
	 * Returns the discount rate for a security.
	 * This function implements the DISC(settlement,maturity,pr,redemption,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](pr), args[3](redemption), args[4](basis) are significant.
	 * @param ctx the function context.
	 * @return the discount rate for a security.
	 */
    public static Object financialDisc(Object[] args, XelContext ctx) {
        double pr = CommonFns.toNumber(args[2]).doubleValue();
        double redemption = CommonFns.toNumber(args[3]).doubleValue();
        if (pr <= 0 || redemption <= 0) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            Date settlement = UtilFns.stringToDate(args[0].toString());
            Date maturity = UtilFns.stringToDate(args[1].toString());
            int basis = 0;
            if (args.length == 5) {
                basis = basis(CommonFns.toNumber(args[4]).intValue());
            }
            int DSM = UtilFns.dsm(settlement, maturity, basis);
            double B = UtilFns.basisToDouble(basis, settlement, maturity, DSM);
            double disc = ((redemption - pr) / redemption) * (B / DSM);
            return new Double(disc);
        }
    }

    /**
	 * Converts a dollar price, expressed as a fraction, into a dollar price, expressed as a decimal number.
	 * This function implements the DOLLARDE(dollar,fraction) spreadsheet function.
	 * @param args the arguments; only args[0](dollar), args[1](fraction) are significant.
	 * @param ctx the function context.
	 * @return a decimal number.
	 */
    public static Object financialDollarde(Object[] args, XelContext ctx) {
        double dollar = CommonFns.toNumber(args[0]).doubleValue();
        double fraction = CommonFns.toNumber(args[1]).doubleValue();
        double dollarde = Math.floor(dollar) + (dollar % 1 * 100) / fraction;
        return new Double(dollarde);
    }

    /**
	 * Converts a dollar price expressed as a decimal number into a dollar price expressed as a fraction.
	 * This function implements the DOLLARFR(dollar,fraction) spreadsheet function.
	 * @param args the arguments; only args[0](dollar), args[1](fraction) are significant.
	 * @param ctx the function context.
	 * @return a fraction.
	 */
    public static Object financialDollarfr(Object[] args, XelContext ctx) {
        double dollar = CommonFns.toNumber(args[0]).doubleValue();
        double fraction = CommonFns.toNumber(args[1]).doubleValue();
        double dollarfr = Math.floor(dollar) + (dollar % 1 * fraction) / 100;
        return new Double(dollarfr);
    }

    /**
	 * Returns the annual duration of a security with periodic interest payments.
	 * This function implements the DURATION(settlement,maturity,coupon,yld,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](coupon), args[3](yld), args[4](frequency), args[5](basis) are significant.
	 * @param ctx the function context.
	 * @return the annual duration of a security with periodic interest payments.
	 */
    public static Object financialDuration(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        if (settle.after(maturi)) {
            throw new SSErrorXelException(SSError.NUM);
        }
        double coupon = CommonFns.toNumber(args[2]).doubleValue();
        double yld = CommonFns.toNumber(args[3]).doubleValue();
        if (!(yld > 0 || coupon > 0)) {
            throw new SSErrorXelException(SSError.NUM);
        }
        int freq = 1;
        try {
            freq = CommonFns.toNumber(args[4]).intValue();
        } catch (ClassCastException e) {
            throw new SSErrorXelException(SSError.NUM);
        }
        if (freq != 1 && freq != 2 && freq != 4) {
            throw new SSErrorXelException(SSError.NUM);
        }
        int basis = 0;
        if (args.length == 6) {
            try {
                basis = basis(CommonFns.toNumber(args[5]).intValue());
            } catch (ClassCastException e) {
                basis = 0;
            }
        }
        double yearFrac = yearFraction(settle, maturi, basis);
        double coupNs = coupnum(settle, maturi, 12 / freq, basis);
        double dura = 0.0;
        coupon *= 100.0 / freq;
        yld /= freq;
        yld += 1.0;
        double nDiff = yearFrac * freq - coupNs;
        for (int t = 1; t < coupNs; t++) {
            dura += (t + nDiff) * (coupon) / Math.pow(yld, t + nDiff);
        }
        dura += (coupNs + nDiff) * (coupon + 100.0) / Math.pow(yld, coupNs + nDiff);
        double p = 0.0;
        for (int t = 1; t < coupNs; t++) {
            p += coupon / Math.pow(yld, t + nDiff);
        }
        p += (coupon + 100.0) / Math.pow(yld, coupNs + nDiff);
        dura /= p;
        dura /= (double) freq;
        return new Double(dura);
    }

    /**
	 * Returns the effective annual interest rate.
	 * This function implements the EFFECT(rate,npery) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](npery) are significant.
	 * @param ctx the function context.
	 * @return the effective annual interest rate.
	 */
    public static Object financialEffect(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        double npery = Math.floor(CommonFns.toNumber(args[1]).doubleValue());
        if (rate <= 0 || npery < 1) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            double effect = Math.pow(1 + rate / npery, npery) - 1;
            return new Double(effect);
        }
    }

    /**
	 * Returns the future value of an investment.
	 * This function implements the FV(rate,nper,pmt,pv,type) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](nper), args[2](pmt), args[3](pv), args[4](type) are significant.
	 * @param ctx the function context.
	 * @return the future value of an investment.
	 */
    public static Object financialFv(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        int nper = CommonFns.toNumber(args[1]).intValue() - 1;
        double pmt = 0.0;
        if (args[2] != null) {
            pmt = CommonFns.toNumber(args[2]).doubleValue();
        }
        double pv = 0.0;
        int type = 0;
        if (args.length > 3 && args[3] != null) {
            pv = CommonFns.toNumber(args[3]).doubleValue();
        }
        if (args.length == 5) {
            type = CommonFns.toNumber(args[4]).intValue();
        }
        return UtilFns.validateNumber(fv(rate, nper, pmt, pv, type));
    }

    private static double fv(double rate, int nper, double pmt, double pv, int type) {
        double fv = -(pv + pmt) * Math.pow(1 + rate, nper + type);
        for (int i = nper; i > 0; i--) {
            double fv1 = -pmt * Math.pow(1 + rate, i + type - 1);
            fv = fv + fv1;
        }
        return fv;
    }

    /**
	 * Returns the future value of an initial principal after applying a series of compound interest rates.
	 * This function implements the FVSCHEDULE(principal,schedule) spreadsheet function.
	 * @param args the arguments; only args[0](principal), args[1](schedule) are significant.
	 * @param ctx the function context.
	 * @return the future value of an initial principal after applying a series of compound interest rates.
	 */
    public static Object financialFvschedule(Object[] args, XelContext ctx) {
        double principal = CommonFns.toNumber(args[0]).doubleValue();
        double[] schedule = UtilFns.toDoubleArray(UtilFns.toList(args[1], ctx));
        for (int i = 0; i < schedule.length; i++) {
            principal = principal * (1 + schedule[i]);
        }
        return new Double(principal);
    }

    /**
	 * Returns the interest rate for a fully invested security.
	 * This function implements the INTRATE(settlement,maturity,investment,redemption,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](investment), args[3](redemption), args[4](basis) are significant.
	 * @param ctx the function context.
	 * @return the interest rate for a fully invested security.
	 */
    public static Object financialIntrate(Object[] args, XelContext ctx) {
        double investment = CommonFns.toNumber(args[2]).doubleValue();
        double redemption = CommonFns.toNumber(args[3]).doubleValue();
        if (investment <= 0 || redemption <= 0) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            Date settlement = UtilFns.stringToDate(args[0].toString());
            Date maturity = UtilFns.stringToDate(args[1].toString());
            int basis = 0;
            if (args.length == 5) {
                basis = basis(CommonFns.toNumber(args[4]).intValue());
            }
            int dsm = UtilFns.dsm(settlement, maturity, basis);
            double B = UtilFns.basisToDouble(basis, settlement, maturity, dsm);
            double intrate = ((redemption - investment) / investment) * (B / dsm);
            return new Double(intrate);
        }
    }

    /**
	 * Returns the interest payment for an investment for a given period.
	 * This function implements the IPMT(rate,per,nper,pv,fv,type) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](per), args[2](nper), args[3](pv), args[4](fv), args[5](type) are significant.
	 * @param ctx the function context.
	 * @return the interest payment for an investment for a given period.
	 */
    public static Object financialIpmt(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        double per = CommonFns.toNumber(args[1]).doubleValue();
        double nper = CommonFns.toNumber(args[2]).doubleValue();
        if (per > nper) {
            throw new SSErrorXelException(SSError.NUM);
        }
        double pv = CommonFns.toNumber(args[3]).doubleValue();
        double fv = 0.0;
        if (args.length > 4 && args[4] != null) {
            fv = CommonFns.toNumber(args[4]).doubleValue();
        }
        int type = 0;
        if (args.length > 5) {
            type = CommonFns.toNumber(args[5]).intValue();
        }
        return new Double(ipmt(rate, per, nper, pv, fv, type));
    }

    private static double GetZw(double rate, double nper, double pmt, double pv, int type) {
        double Zw = 0;
        if (rate == 0.0) Zw = pv + pmt * nper; else {
            double term = Math.pow(1.0 + rate, nper);
            if (type > 0.0) Zw = pv * term + pmt * (1.0 + rate) * (term - 1.0) / rate; else Zw = pv * term + pmt * (term - 1.0) / rate;
        }
        return -Zw;
    }

    private static double ipmt(double rate, double per, double nper, double pv, double fv, int type) {
        double ipmt = 0;
        if (per == 1) {
            if (type == 0) ipmt = -pv; else ipmt = 0;
        } else {
            double pmt = pmt(rate, nper, pv, fv, type);
            if (type == 0) {
                ipmt = GetZw(rate, per - 1.0, pmt, pv, 0);
            } else if (type == 1) {
                ipmt = (GetZw(rate, per - 2, pmt, pv, 1) - pmt);
            }
        }
        return ipmt * rate;
    }

    /**
	 * Returns the internal rate of return for a series of cash flows.
	 * This function implements the IRR(values,guess) spreadsheet function.
	 * @param args the arguments; only args[0](values), args[1](guess) are significant.
	 * @param ctx the function context.
	 * @return the internal rate of return for a series of cash flows.
	 */
    public static Object financialIrr(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Calculates the interest paid during a specific period of an investment.
	 * This function implements the ISPMT(rate,per,nper,pv) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](per), args[2](nper), args[3](pv) are significant.
	 * @param ctx the function context.
	 * @return the interest paid.
	 */
    public static Object financialIspmt(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * In regression analysis, calculates an exponential curve that fits your data and returns an array of values that describes the curve.
	 * This function implements the LOGEST(y,x,const,stats) spreadsheet function.
	 * @param args the arguments; only args[0](y), args[1](x), args[2](const), args[3](stats) are significant.
	 * @param ctx the function context.
	 * @return an array of values that describes the curve.
	 */
    public static Object financialLogest(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the Macauley modified duration for a security with an assumed par value of $100.
	 * This function implements the MDURATION(settlement,maturity,coupon,yld,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](coupon), args[3](yld), args[4](frequency), args[5](basis) are significant.
	 * @param ctx the function context.
	 * @return the Macauley modified duration for a security with an assumed par value of $100.
	 */
    public static Object financialMduration(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the internal rate of return where positive and negative cash flows are financed at different rates.
	 * This function implements the MIRR(values,finance_rate,reinvest_rate) spreadsheet function.
	 * @param args the arguments; only args[0](values), args[1](finance_rate), args[2](reinvest_rate) are significant.
	 * @param ctx the function context.
	 * @return the internal rate of return where positive and negative cash flows are financed at different rates.
	 */
    public static Object financialMirr(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the annual nominal interest rate.
	 * This function implements the NOMINAL(rate,npery) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](npery) are significant.
	 * @param ctx the function context.
	 * @return the annual nominal interest rate.
	 */
    public static Object financialNominal(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        double npery = Math.floor(CommonFns.toNumber(args[1]).doubleValue());
        if (rate <= 0 || npery < 1) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            double nominal = (Math.pow(rate + 1, 1 / npery) - 1) * npery;
            return new Double(nominal);
        }
    }

    /**
	 * Returns the number of periods for an investment.
	 * This function implements the MPER(rate, pmt, pv, fv, type) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](pmt), args[2](pv), args[3](fv), args[4](type) are significant.
	 * @param ctx the function context.
	 * @return the number of periods for an investment.
	 */
    public static Object financialNper(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        double pmt = CommonFns.toNumber(args[1]).doubleValue();
        double pv = CommonFns.toNumber(args[2]).doubleValue();
        double fv = 0.0;
        int type = 0;
        if (args.length > 3 && args[3] != null) {
            fv = CommonFns.toNumber(args[3]).doubleValue();
        }
        if (args.length == 5) {
            type = CommonFns.toNumber(args[4]).intValue();
        }
        return new Double(nper(rate, pmt, pv, fv, type));
    }

    private static double nper(double rate, double pmt, double pv, double fv, int type) {
        double nper = 0.0;
        if (rate == 0) {
            nper = (fv - pv) / pmt;
        } else {
            nper = Math.log(-((fv * rate - pmt * (1 + rate * type)) / (pmt + pmt * rate * type + pv * rate))) / Math.log(1 + rate);
        }
        return nper;
    }

    /**
	 * Returns the net present value of an investment based on a series of periodic cash flows and a discount rate.
	 * This function implements the NPV(rate,arg1,arg2, ...) spreadsheet function.
	 * @param args the arguments; only args[0](rate) are significant.
	 * @param ctx the function context.
	 * @return the net present value of an investment based on a series of periodic cash flows and a discount rate.
	 */
    public static Object financialNpv(Object[] args, XelContext ctx) {
        double[] d = UtilFns.toDoubleArray(UtilFns.toList(args, ctx));
        double[] values = new double[d.length - 1];
        for (int i = 0; i < values.length; i++) {
            values[i] = d[i + 1];
        }
        double result = npv(d[0], values);
        return new Double(result);
    }

    private static double npv(double rate, double[] values) {
        double[] npv = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            npv[i] = values[i] / Math.pow((1 + rate), i + 1);
        }
        return UtilFns.getStats(npv).getSum();
    }

    /**
	 * Returns the price per $100 face value of a security with an odd first period.
	 * This function implements the ODDFPRICE(settlement,maturity,issue,coupon,rate,yld,redemption,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](issue), args[3](fcoupon), args[4](rate), args[5](yld), args[6](redemption), args[7](frequency), args[8](basis) are significant.
	 * @param ctx the function context.
	 * @return the price per $100 face value of a security with an odd first period.
	 */
    public static Object financialOddfprice(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the yield of a security with an odd first period.
	 * This function implements the ODDFYIELD(settlement,maturity,issue,coupon,rate,pr,redemption,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](issue), args[3](coupon), args[4](rate), args[5](pr), args[6](redemption), args[7](frequency), args[8](basis) are significant.
	 * @param ctx the function context.
	 * @return the yield of a security with an odd first period.
	 */
    public static Object financialOddfyield(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the price per $100 face value of a security with an odd last period.
	 * This function implements the ODDLPRICE(settlement,maturity,interest,rate,yld,redemption,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](interest), args[3](rate), args[4](yld), args[5](redemption), args[6](frequency), args[7](basis) are significant.
	 * @param ctx the function context.
	 * @return the price per $100 face value of a security with an odd last period.
	 */
    public static Object financialOddlprice(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the yield of a security with an odd last period.
	 * This function implements the ODDLYIELD(settlement,maturity,interest,rate,pr,redemption,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](interest), args[3](rate), args[4](pr), args[5](redemption), args[6](frequency), args[7](basis) are significant.
	 * @param ctx the function context.
	 * @return the yield of a security with an odd last period.
	 */
    public static Object financialOddlyield(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the periodic payment for an annuity.
	 * This function implements the PMT(rate,nper,pv,fv,type) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](nper), args[2](pv), args[3](fv), args[4](type) are significant.
	 * @param ctx the function context.
	 * @return the periodic payment for an annuity.
	 */
    public static Object financialPmt(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        double nper = CommonFns.toNumber(args[1]).doubleValue();
        double pv = CommonFns.toNumber(args[2]).doubleValue();
        double fv = 0.0;
        int type = 0;
        if (args.length > 3 && args[3] != null) {
            fv = CommonFns.toNumber(args[3]).doubleValue();
        }
        if (args.length == 5) {
            type = CommonFns.toNumber(args[4]).intValue();
        }
        return new Double(pmt(rate, nper, pv, fv, type));
    }

    private static double pmt(double rate, double nper, double pv, double fv, int type) {
        double pmt = 0.0;
        if (rate == 0) {
            pmt = (fv + pv) / nper;
        } else {
            double term = Math.pow(1.0 + rate, nper);
            if (type == 1) pmt = (fv * rate / (term - 1.0) + pv * rate / (1.0 - 1.0 / term)) / (1.0 + rate); else pmt = fv * rate / (term - 1.0) + pv * rate / (1.0 - 1.0 / term);
        }
        return -pmt;
    }

    private static double ppmt(double rate, double per, double nper, double pv, double fv, int type) {
        return pmt(rate, nper, pv, fv, type) - ipmt(rate, per, nper, pv, fv, type);
    }

    /**
	 * Returns the payment on the principal for an investment for a given period.
	 * This function implements the PPMT(rate,per,nper,pv,fv,type) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](per), args[2](nper), args[3](pv), args[4](fv), args[5](type) are significant.
	 * @param ctx the function context.
	 * @return the payment on the principal for an investment for a given period.
	 */
    public static Object financialPpmt(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        double per = CommonFns.toNumber(args[1]).doubleValue();
        double nper = CommonFns.toNumber(args[2]).doubleValue();
        if (per > nper) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            double pv = CommonFns.toNumber(args[3]).doubleValue();
            double fv = 0.0;
            int type = 0;
            if (args.length > 4 && args[4] != null) {
                fv = CommonFns.toNumber(args[4]).doubleValue();
            }
            if (args.length == 6) {
                type = CommonFns.toNumber(args[5]).intValue();
            }
            double ppmt = ppmt(rate, per, nper, pv, fv, type);
            return new Double(ppmt);
        }
    }

    /**
	 * Returns the price per $100 face value of a security that pays periodic interest.
	 * This function implements the PRICE(settlement,maturity,rate,yld,redemption,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](rate), args[3](yld), args[4](redemption), args[5](frequency), args[6](basis) are significant.
	 * @param ctx the function context.
	 * @return the price per $100 face value of a security that pays periodic interest.
	 */
    public static Object financialPrice(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        if (settle.after(maturi)) {
            throw new SSErrorXelException(SSError.NUM);
        }
        double rate = CommonFns.toNumber(args[2]).doubleValue();
        double yld = CommonFns.toNumber(args[3]).doubleValue();
        double redemption = CommonFns.toNumber(args[4]).doubleValue();
        if (!(yld > 0 || rate > 0 || redemption > 0)) {
            throw new SSErrorXelException(SSError.NUM);
        }
        int freq = 1;
        try {
            freq = CommonFns.toNumber(args[5]).intValue();
        } catch (ClassCastException e) {
            throw new SSErrorXelException(SSError.NUM);
        }
        if (freq != 1 && freq != 2 && freq != 4) {
            throw new SSErrorXelException(SSError.NUM);
        }
        int basis = 0;
        if (args.length == 7) {
            try {
                basis = basis(CommonFns.toNumber(args[6]).intValue());
            } catch (ClassCastException e) {
                basis = 0;
            }
        }
        return new Double(price(settle, maturi, rate, yld, redemption, freq, basis));
    }

    private static double price(Date settle, Date maturi, double rate, double yld, double redemp, int freq, int basis) {
        Date couppcd = couppcd(settle, maturi, frequency(freq), basis);
        Date coupncd = coupncd(couppcd, frequency(freq));
        double E = UtilFns.dsm(couppcd, coupncd, basis);
        double DSC = UtilFns.dsm(settle, coupncd, basis);
        double N = coupnum(settle, maturi, frequency(freq), basis);
        double A = UtilFns.dsm(couppcd, settle, basis);
        double price = redemp / Math.pow((1 + yld / freq), (N - 1 + DSC / E));
        double T1 = 100.0 * rate / freq;
        double T2 = 1.0 + yld / freq;
        double T3 = DSC / E;
        for (int i = 0; i < N; i++) {
            price += T1 / Math.pow(T2, i + T3);
        }
        price -= T1 * A / E;
        return price;
    }

    /**
	 * Returns the price per $100 face value of a discounted security.
	 * This function implements the PRICEDISC(settlement,maturity,discount,redemption,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](discount), args[3](redemption), args[4](basis) are significant.
	 * @param ctx the function context.
	 * @return the price per $100 face value of a discounted security.
	 */
    public static Object financialPricedisc(Object[] args, XelContext ctx) {
        double discount = CommonFns.toNumber(args[2]).doubleValue();
        double redemption = CommonFns.toNumber(args[3]).doubleValue();
        if (discount <= 0 || redemption <= 0) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            Date settlement = UtilFns.stringToDate(args[0].toString());
            Date maturity = UtilFns.stringToDate(args[1].toString());
            int basis = 0;
            if (args.length == 5) {
                basis = basis(CommonFns.toNumber(args[4]).intValue());
            }
            int dsm = UtilFns.dsm(settlement, maturity, basis);
            double B = UtilFns.basisToDouble(basis, settlement, maturity, dsm);
            double pricedisc = redemption - discount * redemption * (dsm / B);
            return new Double(pricedisc);
        }
    }

    /**
	 * Returns the price per $100 face value of a security that pays interest at maturity.
	 * This function implements the PRICEMAT(settlement,maturity,issue,rate,yld,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](issue), args[3](rate), args[4](yld), args[5](basis) are significant.
	 * @param ctx the function context.
	 * @return the price per $100 face value of a security that pays interest at maturity.
	 */
    public static Object financialPricemat(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        Date issue = UtilFns.stringToDate(args[2].toString());
        double rate = CommonFns.toNumber(args[3]).doubleValue();
        double yld = CommonFns.toNumber(args[4]).doubleValue();
        if (rate < 0 || yld < 0) {
            throw new SSErrorXelException(SSError.NUM);
        }
        int basis = 0;
        if (args.length == 6) {
            basis = basis(CommonFns.toNumber(args[5]).intValue());
        }
        int dsm = UtilFns.dsm(settle, maturi, basis);
        int dim = UtilFns.dsm(issue, maturi, basis);
        int A = UtilFns.dsm(issue, settle, basis);
        double B = UtilFns.basisToDouble(basis, issue, settle, A);
        double pricemat = ((100 + ((dim / B) * rate * 100)) / (1 + ((dsm / B) * yld))) - ((A / B) * rate * 100);
        return new Double(pricemat);
    }

    /**
	 * Returns the present value of an investment.
	 * This function implements the PV(rate,nper,pmt,fv,type) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](nper), args[2](pmt), args[3](fv), args[4](type) are significant.
	 * @param ctx the function context.
	 * @return the present value of an investment.
	 */
    public static Object financialPv(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        int nper = CommonFns.toNumber(args[1]).intValue();
        double pmt = 0.0;
        if (args[2] != null) {
            pmt = CommonFns.toNumber(args[2]).doubleValue();
        }
        double fv = 0.0;
        if (args.length > 3 && args[3] != null) {
            fv = CommonFns.toNumber(args[3]).doubleValue();
        }
        int type = 0;
        if (args.length == 5) {
            type = CommonFns.toNumber(args[4]).intValue();
        }
        return new Double(pv(rate, nper, pmt, fv, type));
    }

    private static double pv(double rate, int nper, double pmt, double fv, int type) {
        double pv = 0.0;
        if (rate == 0) {
            pv = -(fv + (pmt * nper));
        } else {
            pv = -(fv + (pmt * (1 + rate * type) * (Math.pow(1 + rate, nper) - 1) / rate) / Math.pow(1 + rate, nper));
        }
        return pv;
    }

    /**
	 * Returns the interest rate per period of an annuity.
	 * This function implements the RATE(nper,pmt,pv,fv,type,guess) spreadsheet function.
	 * @param args the arguments; only args[0](nper), args[1](pmt), args[2](pv), args[3](fv), args[4](type), args[5](guess) are significant.
	 * @param ctx the function context.
	 * @return the interest rate per period of an annuity.
	 */
    public static Object financialRate(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the amount received at maturity for a fully invested security.
	 * This function implements the RECEIVED(settlement,maturity,investment,discount,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](investment), args[3](discount), args[4](basis) are significant.
	 * @param ctx the function context.
	 * @return the amount received at maturity for a fully invested security.
	 */
    public static Object financialReceived(Object[] args, XelContext ctx) {
        double investment = CommonFns.toNumber(args[2]).doubleValue();
        double discount = CommonFns.toNumber(args[3]).doubleValue();
        if (investment <= 0 || discount <= 0) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            Date settle = UtilFns.stringToDate(args[0].toString());
            Date maturi = UtilFns.stringToDate(args[1].toString());
            int basis = 0;
            if (args.length == 5) {
                basis = basis(CommonFns.toNumber(args[4]).intValue());
            }
            int dim = UtilFns.dsm(settle, maturi, basis);
            double B = UtilFns.basisToDouble(basis, settle, maturi, dim);
            double received = investment / (1 - (discount * (dim / B)));
            return new Double(received);
        }
    }

    /**
	 * Returns the straight-line depreciation of an asset for one period.
	 * This function implements the SLN(cost,salvage,life) spreadsheet function.
	 * @param args the arguments; only args[0](cost), args[1](salvage), args[2](life) are significant.
	 * @param ctx the function context.
	 * @return the straight-line depreciation of an asset for one period.
	 */
    public static Object financialSln(Object[] args, XelContext ctx) {
        double cost = 0;
        double salvage = 0;
        double life = 0;
        if (args[0] != null) cost = CommonFns.toNumber(args[0]).doubleValue();
        if (args[1] != null) salvage = CommonFns.toNumber(args[1]).doubleValue();
        if (args[2] != null) life = CommonFns.toNumber(args[2]).doubleValue();
        return new Double(sln(cost, salvage, life));
    }

    private static double sln(double cost, double salvage, double life) {
        if (life == 0) {
            throw new SSErrorXelException(SSError.DIV0);
        }
        return (cost - salvage) / life;
    }

    /**
	 * Returns the sum-of-years' digits depreciation of an asset for a specified period.
	 * This function implements the SYD(cost,salvage,life,per) spreadsheet function.
	 * @param args the arguments; only args[0](cost), args[1](salvage), args[2](life), args[3](per) are significant.
	 * @param ctx the function context.
	 * @return the sum-of-years' digits depreciation of an asset for a specified period.
	 */
    public static Object financialSyd(Object[] args, XelContext ctx) {
        double cost = 0;
        double salvage = 0;
        if (args[0] != null) cost = CommonFns.toNumber(args[0]).doubleValue();
        if (args[1] != null) salvage = CommonFns.toNumber(args[1]).doubleValue();
        if (args[2] == null || args[3] == null) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            double life = CommonFns.toNumber(args[2]).doubleValue();
            double per = CommonFns.toNumber(args[3]).doubleValue();
            if (life <= 0 || per <= 0) {
                throw new SSErrorXelException(SSError.NUM);
            } else {
                return new Double(syd(cost, salvage, life, per));
            }
        }
    }

    private static double syd(double cost, double salvage, double life, double per) {
        return ((cost - salvage) * (life - per + 1) * 2) / (life * (life + 1));
    }

    /**
	 * Returns the bond-equivalent yield for a Treasury bill.
	 * This function implements the TBILLEQ(settlement,maturity,discount) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](discount) are significant.
	 * @param ctx the function context.
	 * @return the bond-equivalent yield for a Treasury bill.
	 */
    public static Object financialTbilleq(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        double discount = CommonFns.toNumber(args[2]).doubleValue();
        if (discount <= 0) {
            throw new SSErrorXelException(SSError.NUM);
        }
        double DSM = UtilFns.dsm(settle, maturi, 2);
        double tbilleq = (365 * discount) / (360 - (discount * DSM));
        return new Double(tbilleq);
    }

    /**
	 * Returns the price per $100 face value for a Treasury bill.
	 * This function implements the TBILLPRICE(settlement,maturity,discount) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](discount) are significant.
	 * @param ctx the function context.
	 * @return the price per $100 face value for a Treasury bill.
	 */
    public static Object financialTbillprice(Object[] args, XelContext ctx) {
        Date settlement = UtilFns.stringToDate(args[0].toString());
        Date maturity = UtilFns.stringToDate(args[1].toString());
        double discount = CommonFns.toNumber(args[2]).doubleValue();
        if (discount <= 0) {
            throw new SSErrorXelException(SSError.NUM);
        }
        double DSM = UtilFns.dsm(settlement, maturity, 2);
        if (DSM > 365) {
            throw new SSErrorXelException(SSError.NUM);
        }
        double tbillprice = 100 * (1 - (discount * DSM) / 360);
        return new Double(tbillprice);
    }

    /**
	 * Returns the yield for a Treasury bill.
	 * This function implements the TBILLYIELD(settlement,maturity,pr) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](pr) are significant.
	 * @param ctx the function context.
	 * @return the yield for a Treasury bill.
	 */
    public static Object financialTbillyield(Object[] args, XelContext ctx) {
        Date settlement = UtilFns.stringToDate(args[0].toString());
        Date maturity = UtilFns.stringToDate(args[1].toString());
        double pr = CommonFns.toNumber(args[2]).doubleValue();
        if (pr <= 0) {
            throw new SSErrorXelException(SSError.NUM);
        }
        double DSM = UtilFns.dsm(settlement, maturity, 2);
        double tbillyield = ((100 - pr) / pr) * (360 / DSM);
        return new Double(tbillyield);
    }

    /**
	 * Returns the depreciation of an asset for a specified or partial period by using a declining balance method.
	 * This function implements the VDB(cost,salvage,life,start,end,factor,no_switch) spreadsheet function.
	 * @param args the arguments; only args[0](cost), args[1](salvage), args[2](life), args[3](start), args[4](end), args[5](factor), args[6](no_switch) are significant.
	 * @param ctx the function context.
	 * @return the depreciation of an asset for a specified or partial period by using a declining balance method.
	 */
    public static Object financialVdb(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the internal rate of return for a schedule of cash flows that is not necessarily periodic.
	 * This function implements the XIRR(values,dates,guess) spreadsheet function.
	 * @param args the arguments; only args[0](values), args[1](dates), args[2](guess) are significant.
	 * @param ctx the function context.
	 * @return the internal rate of return for a schedule of cash flows that is not necessarily periodic.
	 */
    public static Object financialXirr(Object[] args, XelContext ctx) {
        return null;
    }

    /**
	 * Returns the net present value for a schedule of cash flows that is not necessarily periodic.
	 * This function implements the XNPV(rate,values,dates) spreadsheet function.
	 * @param args the arguments; only args[0](rate), args[1](values), args[2](dates) are significant.
	 * @param ctx the function context.
	 * @return the net present value for a schedule of cash flows that is not necessarily periodic.
	 */
    public static Object financialXnpv(Object[] args, XelContext ctx) {
        double rate = CommonFns.toNumber(args[0]).doubleValue();
        double[] values = new double[UtilFns.toList(args[1], ctx).size()];
        values = UtilFns.toDoubleArray(UtilFns.toList(args[1], ctx));
        List ls = UtilFns.toList(args[2], ctx);
        if (values.length != ls.size()) {
            throw new SSErrorXelException(SSError.NUM);
        } else {
            Date[] dates = UtilFns.toDateArray(ls);
            double xnpv = 0.0;
            for (int i = 0; i < dates.length; i++) {
                double di = UtilFns.dateToDouble(dates[i]).doubleValue();
                double d1 = UtilFns.dateToDouble(dates[0]).doubleValue();
                xnpv += values[i] / Math.pow(1 + rate, (di * d1) / 365);
            }
            return new Double(xnpv);
        }
    }

    /**
	 * Returns the yield on a security that pays periodic interest.
	 * This function implements the YIELD(settlement,maturity,rate,pr,redemption,frequency,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](rate), args[3](pr), args[4](redemption), args[5](frequency), args[6](basis) are significant.
	 * @param ctx the function context.
	 * @return the yield on a security that pays periodic interest.
	 */
    public static Object financialYield(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        double rate = CommonFns.toNumber(args[2]).doubleValue();
        double pr = CommonFns.toNumber(args[3]).doubleValue();
        double redemp = CommonFns.toNumber(args[4]).doubleValue();
        int freq = CommonFns.toNumber(args[5]).intValue();
        int basis = 0;
        if (args.length == 7) {
            try {
                basis = basis(CommonFns.toNumber(args[6]).intValue());
            } catch (ClassCastException e) {
                basis = 0;
            }
        }
        double result;
        if (coupnum(settle, maturi, frequency(freq), basis) <= 1.0) {
            double A = coupdaybs(settle, maturi, frequency(freq), basis);
            double DSR = coupdaysnc(settle, maturi, frequency(freq), basis);
            double E = coupdays(settle, maturi, frequency(freq), basis);
            double term = rate / freq;
            double coeff = freq * E / DSR;
            double num = (redemp / 100.0 + term) - (pr / 100.0 + (A / E * term));
            double den = pr / 100.0 + (A / E * term);
            result = num / den * coeff;
        } else {
            result = getYld(settle, maturi, rate, pr, redemp, freq, basis);
        }
        return new Double(result);
    }

    private static double getYld(Date set, Date mat, double rate, double fPrice, double redemp, int freq, int basis) throws ArithmeticException {
        double fPriceN = 0.0;
        double fYield1 = 0.0;
        double fYield2 = 1.0;
        double fPrice1 = price(set, mat, rate, fYield1, redemp, freq, basis);
        double fPrice2 = price(set, mat, rate, fYield2, redemp, freq, basis);
        double fYieldN = (fYield2 - fYield1) * 0.5;
        for (int nIter = 0; nIter < 100 && fPriceN != fPrice; nIter++) {
            fPriceN = price(set, mat, rate, fYieldN, redemp, freq, basis);
            if (fPrice == fPrice1) return fYield1; else if (fPrice == fPrice2) return fYield2; else if (fPrice == fPriceN) return fYieldN; else if (fPrice < fPrice2) {
                fYield2 *= 2.0;
                fPrice2 = price(set, mat, rate, fYield2, redemp, freq, basis);
                fYieldN = (fYield2 - fYield1) * 0.5;
            } else {
                if (fPrice < fPriceN) {
                    fYield1 = fYieldN;
                    fPrice1 = fPriceN;
                } else {
                    fYield2 = fYieldN;
                    fPrice2 = fPriceN;
                }
                fYieldN = fYield2 - (fYield2 - fYield1) * ((fPrice - fPrice2) / (fPrice1 - fPrice2));
            }
        }
        if (Math.abs(fPrice - fPriceN) > fPrice / 100.0) throw new ArithmeticException();
        return fYieldN;
    }

    /**
	 * Returns the annual yield for a discounted security; for example, a Treasury bill.
	 * This function implements the YIELDDISC(settlement,maturity,pr,redemption,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](pr), args[3](redemption), args[4](basis) are significant.
	 * @param ctx the function context.
	 * @return the annual yield for a discounted security; for example, a Treasury bill.
	 */
    public static Object financialYielddisc(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        double price = CommonFns.toNumber(args[2]).doubleValue();
        double redemp = CommonFns.toNumber(args[3]).doubleValue();
        int basis = 0;
        if (args.length == 5) {
            try {
                basis = basis(CommonFns.toNumber(args[4]).intValue());
            } catch (ClassCastException e) {
                basis = 0;
            }
        }
        double result = (redemp / price) - 1.0;
        result /= yearFraction(settle, maturi, basis);
        return new Double(result);
    }

    /**
	 * Returns the annual yield of a security that pays interest at maturity.
	 * This function implements the YIELDMAT(settlement,maturity,issue,rate,pr,basis) spreadsheet function.
	 * @param args the arguments; only args[0](settlement), args[1](maturity), args[2](issue), args[3](rate), args[4](pr), args[5](basis) are significant.
	 * @param ctx the function context.
	 * @return the annual yield of a security that pays interest at maturity.
	 */
    public static Object financialYieldmat(Object[] args, XelContext ctx) {
        Date settle = UtilFns.stringToDate(args[0].toString());
        Date maturi = UtilFns.stringToDate(args[1].toString());
        Date issue = UtilFns.stringToDate(args[2].toString());
        double rate = CommonFns.toNumber(args[3]).doubleValue();
        double price = CommonFns.toNumber(args[4]).doubleValue();
        int basis = 0;
        if (args.length == 6) {
            try {
                basis = basis(CommonFns.toNumber(args[5]).intValue());
            } catch (ClassCastException e) {
                basis = 0;
            }
        }
        double fIssMat = yearFraction(issue, maturi, basis);
        double fIssSet = yearFraction(issue, settle, basis);
        double fSetMat = yearFraction(settle, maturi, basis);
        double y = 1.0 + fIssMat * rate;
        y /= price / 100.0 + fIssSet * rate;
        y--;
        y /= fSetMat;
        return new Double(y);
    }
}
