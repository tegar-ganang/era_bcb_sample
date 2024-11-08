package pe.com.bn.sach.calculoCuota;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

/**
 * @author llavado
 *
 * TODO Para cambiar la plantilla de este comentario generado, vaya a
 * Ventana - Preferencias - Java - Estilo de c�digo - Plantillas de c�digo
 */
public class UtilCosto {

    public static int BUSQUEDA_UP = 1;

    public static int BUSQUEDA_DOWN = 2;

    public static int TRUE = 1;

    public static int FALSE = 2;

    public static double NUM_VARIANCE_DECIMAL = 0.00001;

    private static int NUM_DIGITOS_DECIMAL = 2;

    public static ArrayList CopyArray(ArrayList data) {
        ArrayList newArr = new ArrayList();
        for (int i = 0; i < data.size(); i++) {
            newArr.add(i, data.get(i));
        }
        return newArr;
    }

    public static Hashtable CalcularCuotaFactor(ArrayList cuotaSemilla, ArrayList arrFactor, int gracia) {
        if (cuotaSemilla == null || arrFactor == null) return null;
        if (cuotaSemilla.size() != arrFactor.size()) return null;
        double[] arrFactorCuot = new double[3];
        double totFactorCuota = 0.0;
        for (int i = 0; i < arrFactor.size(); i++) {
            double multFactCuota = 0.0;
            Amortizacion cuota = (Amortizacion) cuotaSemilla.get(i);
            BCostoEfectivo costo = (BCostoEfectivo) arrFactor.get(i);
            multFactCuota = Util.redondeoDouble(cuota.getCuotas() * costo.getFactores(), Util.NUM_DIGITOS_DECIMAL_8);
            totFactorCuota = Util.redondeoDouble(totFactorCuota + multFactCuota, Util.NUM_DIGITOS_DECIMAL_8);
            costo.setCuotaFactor(multFactCuota);
            costo.setCuota(cuota.getCuotas());
        }
        arrFactorCuot[0] = totFactorCuota;
        Amortizacion saldo = (Amortizacion) cuotaSemilla.get(0 + gracia);
        double saldoCapitalIni = saldo.getSaldo();
        arrFactorCuot[1] = saldoCapitalIni;
        double difeCuotaFactor = Util.redondeoDouble(saldoCapitalIni - totFactorCuota, Util.NUM_DIGITOS_DECIMAL_4);
        arrFactorCuot[2] = difeCuotaFactor;
        Hashtable dataFactorCuota = new Hashtable();
        dataFactorCuota.put("factorcuota", arrFactor);
        dataFactorCuota.put("totfactor", arrFactorCuot);
        return dataFactorCuota;
    }

    public static ArrayList CalculoGraciaCostoEfectivo(int mesesGracia, int yearPago, int monthPago, int dayPago, int diaPago) {
        ArrayList lista = null;
        lista = inicializaCostoEfectivo(yearPago, monthPago, dayPago);
        BNDate bnFecha = new BNDate();
        BCostoEfectivo dataIni = new BCostoEfectivo();
        dataIni = (BCostoEfectivo) lista.get(0);
        bnFecha.setFecha(dataIni.getFecha());
        for (int i = 1; i <= mesesGracia; i++) {
            bnFecha = Util.obtenerPeriodoSiguiente(bnFecha.getFecha(), diaPago);
            BCostoEfectivo dataGracia = new BCostoEfectivo();
            dataGracia.setItem(i);
            dataGracia.setFecha(bnFecha.getFecha());
            dataGracia.setPeriodoPago(bnFecha.getPeriodo());
            dataGracia.setPeriodoAcumulado(0);
            dataGracia.setFactores(0);
            lista.add(i, dataGracia);
        }
        return lista;
    }

    public static Hashtable factoresCostoEfectivo(double interesMensual, int year, int month, int day, int plazoPago, int diaPago, int mesesGracia, int dobleJulio, int dobleDiciembre, ArrayList cuotaGracia) {
        ArrayList lista = null;
        BNDate bnFecha = new BNDate();
        BCostoEfectivo dataIni = new BCostoEfectivo();
        if (mesesGracia == 0) {
            lista = inicializaCostoEfectivo(year, month, day);
            dataIni = (BCostoEfectivo) lista.get(mesesGracia);
            bnFecha.setFecha(dataIni.getFecha());
        } else {
            lista = Util.copyArray(cuotaGracia);
            dataIni = (BCostoEfectivo) lista.get(mesesGracia);
            bnFecha.setFecha(dataIni.getFecha());
        }
        long periodoAcumulado = 0;
        double sumatoriaFactor = 0;
        for (int i = 1 + mesesGracia; i <= plazoPago + mesesGracia; i++) {
            bnFecha = Util.obtenerPeriodoSiguiente(bnFecha.getFecha(), diaPago);
            boolean ejecutaJul = false;
            boolean ejecutaDic = false;
            if (dobleJulio == 1 && Util.cuotaDoble(bnFecha.getFecha(), BNDate.MES_JULIO)) {
                ejecutaJul = true;
            }
            if (dobleDiciembre == 1 && Util.cuotaDoble(bnFecha.getFecha(), BNDate.MES_DICIEMBRE)) {
                ejecutaDic = true;
            }
            periodoAcumulado += bnFecha.getPeriodo();
            double factor = Util.redondeoDouble(calculoFactor(interesMensual, periodoAcumulado), Util.NUM_DIGITOS_DECIMAL_8);
            sumatoriaFactor = Util.redondeoDouble(sumatoriaFactor + factor, Util.NUM_DIGITOS_DECIMAL_8);
            lista = addElementArrayCostoEfectivo(i, bnFecha, periodoAcumulado, factor, lista);
        }
        double[] amortiz = new double[1];
        amortiz[0] = sumatoriaFactor;
        Hashtable info = new Hashtable();
        info.put("lista", lista);
        info.put("amortiz", amortiz);
        return info;
    }

    private static ArrayList addElementArrayCostoEfectivo(int posicion, BNDate bnFecha, long periodoAcumulado, double factor, ArrayList lista) {
        BCostoEfectivo dataIni = new BCostoEfectivo();
        dataIni.setItem(posicion);
        dataIni.setFecha(bnFecha.getFecha());
        dataIni.setPeriodoPago(bnFecha.getPeriodo());
        dataIni.setPeriodoAcumulado(periodoAcumulado);
        dataIni.setFactores(factor);
        lista.add(posicion, dataIni);
        return lista;
    }

    private static ArrayList inicializaCostoEfectivo(int yearPago, int monthPago, int dayPago) {
        ArrayList lista = new ArrayList();
        Calendar fechaInicio = Calendar.getInstance();
        fechaInicio.set(yearPago, monthPago, dayPago);
        BNDate bnFecha = new BNDate();
        bnFecha.setFecha(fechaInicio);
        BCostoEfectivo dataIni = new BCostoEfectivo();
        dataIni.setItem(0);
        dataIni.setFecha(bnFecha.getFecha());
        dataIni.setPeriodoPago(0);
        dataIni.setPeriodoAcumulado(0);
        dataIni.setFactores(0);
        lista.add(0, dataIni);
        return lista;
    }

    private static double calculoFactorCostoEfectivo(double tasaInteres, long periodoAcMes) {
        double factor = 0.0;
        double periodoTrans = periodoAcMes / 30.0;
        factor = Math.pow((1.0 + tasaInteres), periodoTrans);
        double factor1 = 0.0;
        factor1 = (1.0 / factor);
        return factor1;
    }

    private static double calculoFactor(double tasaInteres, long periodoAcMes) {
        double factor = 0.0;
        double periodoTrans = periodoAcMes / 30.0;
        factor = Math.pow((1.0 + tasaInteres), periodoTrans);
        double factor1 = 0.0;
        factor1 = (1.0 / factor);
        return factor1;
    }

    public static void printFactores(ArrayList factores) {
        if (factores != null) {
            for (int i = 0; i < factores.size(); i++) {
                BCostoEfectivo amort = new BCostoEfectivo();
                amort = (BCostoEfectivo) factores.get(i);
                String fecha = "";
                if (amort.getFecha() != null) {
                    fecha = Util.getFormattedDate(amort.getFecha().getTime());
                }
            }
        }
    }

    public static Hashtable evaluarIteracionFactor(double[] totFactCuota, double[] initArray, double oldInteres) {
        int ejecutar = 0;
        if (initArray == null) {
            initArray = new double[1];
            initArray[0] = 0.0;
        }
        double[] dataArray = null;
        double diferencia = totFactCuota[2];
        double interes = oldInteres;
        double varianza = Math.abs(totFactCuota[2]);
        double[] newInteres = new double[1];
        if (varianza <= NUM_VARIANCE_DECIMAL) {
            ejecutar = FALSE;
        } else {
            if (diferencia < 0) {
                Hashtable infoBusqueda = new Hashtable();
                infoBusqueda = busquedaBinaria(interes, UtilCosto.BUSQUEDA_UP, initArray);
                dataArray = (double[]) infoBusqueda.get("initArray");
                newInteres[0] = ((Double) infoBusqueda.get("interes")).doubleValue();
            } else {
                Hashtable infoBusqueda = new Hashtable();
                infoBusqueda = busquedaBinaria(interes, UtilCosto.BUSQUEDA_DOWN, initArray);
                dataArray = (double[]) infoBusqueda.get("initArray");
                newInteres[0] = ((Double) infoBusqueda.get("interes")).doubleValue();
            }
            ejecutar = TRUE;
        }
        Hashtable resultado = new Hashtable();
        resultado.put("ejecutar", new Integer(ejecutar));
        resultado.put("initArray", dataArray);
        resultado.put("newinteres", newInteres);
        return resultado;
    }

    public static Hashtable busquedaBinaria(double valorEvaluar, int posicion, double[] amortList) {
        double number = 0.0;
        double ValorSup;
        double valorBase;
        double[] amortListNew = null;
        if (posicion == BUSQUEDA_UP) {
            double tmpAmort = amortList[amortList.length - 1];
            if (valorEvaluar >= tmpAmort) {
                number = roundDouble(valorEvaluar * 1.5, NUM_DIGITOS_DECIMAL);
            } else {
                for (int i = amortList.length - 1; i >= 0; i--) {
                    if (amortList[i] <= valorEvaluar) {
                        break;
                    }
                    tmpAmort = amortList[i];
                }
                ValorSup = tmpAmort;
                number = roundDouble((ValorSup + valorEvaluar) / 2.0, NUM_DIGITOS_DECIMAL);
            }
        }
        if (posicion == BUSQUEDA_DOWN) {
            double tmpAmort = 0.0;
            for (int i = 0; i < amortList.length; i++) {
                if (amortList[i] >= valorEvaluar) {
                    break;
                }
                tmpAmort = amortList[i];
            }
            valorBase = tmpAmort;
            number = roundDouble((valorEvaluar + valorBase) / 2.0, NUM_DIGITOS_DECIMAL);
        }
        amortListNew = AddArray(amortList, valorEvaluar);
        Hashtable infoBusqueda = new Hashtable();
        infoBusqueda.put("interes", new Double(number));
        infoBusqueda.put("initArray", amortListNew);
        return infoBusqueda;
    }

    public static double roundDouble(double d, int places) {
        double newNum = 0.0;
        newNum = Math.round(d * Math.pow(10, (double) places)) / Math.pow(10, (double) places);
        return newNum;
    }

    public static double[] AddArray(double[] data, double dataNew) {
        double[] newArr = new double[data.length + 1];
        int i;
        for (i = 0; i < data.length; i++) {
            newArr[i] = data[i];
        }
        newArr[i] = dataNew;
        bubble(newArr);
        return newArr;
    }

    public static void bubble(double[] a) {
        for (int i = a.length - 1; i > 0; i--) for (int j = 0; j < i; j++) if (a[j] > a[j + 1]) {
            double temp = a[j];
            a[j] = a[j + 1];
            a[j + 1] = temp;
        }
    }
}
