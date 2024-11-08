package com.ecomponentes.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import javax.activation.MimetypesFileTypeMap;
import com.ecomponentes.formularios.anexo.to.AnexoTO;

public final class Tools {

    static final ResourceBundle rb = ResourceBundle.getBundle("com.ecomponentes.util.RBFilesPath");

    public static final String uploadDiretorio = rb.getString("upload.diretorio");

    public static String formatoReal(Object o) {
        DecimalFormat meuFormato = new DecimalFormat("0.00");
        return meuFormato.format(Float.parseFloat(o.toString()));
    }

    public static String formatoReal(float o) {
        DecimalFormat meuFormato = new DecimalFormat("0.00");
        return meuFormato.format(o);
    }

    /**
	 * RETORNA A DATA ATUAL EM FORMATO dd/mm/aaaa
	 * @return java.lang.String
	 */
    public static String getData() {
        return new SimpleDateFormat("dd/MM/yyyy").format(new Date()).toString();
    }

    /**
	 * RETORNA A DATA ATUAL POR EXTENSO
	 * @return java.lang.String
	 */
    public static String getDataPorExtenso(Date d) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, new Locale("pt", "BR"));
        return df.format(d).toString();
    }

    /**
	 * RETORNA A DATA ATUAL EM FORMATO dd/mm/aaaa
	 * @return java.lang.String
	 */
    public static String getData(Date d) {
        return new SimpleDateFormat("dd/MM/yyyy").format(d).toString();
    }

    /**
	 * RETORNA A DATA ATUAL EM FORMATO aaaammdd
	 * @return java.lang.Integer
	 */
    public static String getDataString(Date d) {
        return new SimpleDateFormat("yyyyMMdd").format(d).toString();
    }

    /**
	 * RETORNA A DATA ATUAL NO PADR�O USA
	 * @return java.lang.String
	 */
    public static String getDataPadraoUS(String data) {
        String mes = new String("");
        String dia = new String("");
        String ano = new String("");
        dia = (data.substring(0, 2));
        mes = (data.substring(3, 5));
        ano = (data.substring(6, 10));
        if (mes.equals("01")) mes = "Jan"; else if (mes.equals("02")) mes = "Fev"; else if (mes.equals("03")) mes = "Mar"; else if (mes.equals("04")) mes = "Abr"; else if (mes.equals("05")) mes = "Mai"; else if (mes.equals("06")) mes = "Jun"; else if (mes.equals("07")) mes = "Jul"; else if (mes.equals("08")) mes = "Ago"; else if (mes.equals("09")) mes = "Set"; else if (mes.equals("10")) mes = "Out"; else if (mes.equals("11")) mes = "Nov"; else if (mes.equals("12")) mes = "Dez";
        return mes + " " + dia + ", " + ano;
    }

    /**
	 * RETORNA A DATA ATUAL NO PADR�O Brazil
	 * @return java.lang.String
	 */
    public static String getDataPadraoBR(String data) {
        String mes = new String("");
        String dia = new String("");
        String ano = new String("");
        dia = (data.substring(0, 2));
        mes = (data.substring(3, 5));
        ano = (data.substring(6, 10));
        if (mes.equals("01")) mes = "Janeiro"; else if (mes.equals("02")) mes = "Fevereiro"; else if (mes.equals("03")) mes = "Mar�o"; else if (mes.equals("04")) mes = "Abril"; else if (mes.equals("05")) mes = "Maio"; else if (mes.equals("06")) mes = "Junho"; else if (mes.equals("07")) mes = "Jullho"; else if (mes.equals("08")) mes = "Agosto"; else if (mes.equals("09")) mes = "Setembro"; else if (mes.equals("10")) mes = "Outubro"; else if (mes.equals("11")) mes = "Novembro"; else if (mes.equals("12")) mes = "Dezembro";
        return dia + " de " + mes + " de " + ano;
    }

    /**
	 *	Lista arquivos de um diretorio 
	 */
    public Vector listarArquivos(String path) {
        File dir = new File(Tools.uploadDiretorio + path);
        Vector list = new Vector();
        File listAux[] = dir.listFiles();
        for (int i = 0; i < listAux.length; i++) {
            if (listAux[i].isFile() && !listAux[i].isHidden()) {
                list.add(listAux[i].getName());
            }
        }
        return list;
    }

    /**
	 *	Lista arquivos de um diretorio 
	 */
    public Vector listarArquivosNoPath(String path) {
        File dir = new File(path);
        Vector list = new Vector();
        File listAux[] = dir.listFiles();
        for (int i = 0; i < listAux.length; i++) {
            if (listAux[i].isFile() && !listAux[i].isHidden()) {
                list.add(listAux[i].getName());
            }
        }
        return list;
    }

    /**
	 *	Lista arquivos de um diretorio 
	 */
    public Vector listarDiretorio(String path) {
        File dir = new File(path);
        Vector list = new Vector();
        File listAux[] = dir.listFiles();
        if (listAux != null) for (int i = 0; i < listAux.length; i++) {
            if (listAux[i].isFile() && !listAux[i].isHidden()) {
                list.add(listAux[i].getName());
            }
        }
        return list;
    }

    /**
	 *	Lista arquivos de um diretorio e retorna um array de arquivos 
	 */
    public File[] listarArquivosDiretorio(String path) {
        File dir = new File(Tools.uploadDiretorio + path);
        return dir.listFiles();
    }

    /**
	 *	Lista arquivos de um diretorio 
	 */
    public Vector listar(String path) throws Exception {
        File dir = new File(path);
        Vector list = new Vector();
        if (dir.length() > 0) {
            File listAux[] = dir.listFiles();
            for (int i = 0; i < listAux.length; i++) {
                if (listAux[i].isFile() && !listAux[i].isHidden()) {
                    list.add(listAux[i].getName());
                }
            }
        }
        return list;
    }

    /**
	 *	Deletar Arquivos 
	 */
    public boolean deletarArquivo(String arquivo) throws Exception {
        return new File(Tools.uploadDiretorio + arquivo).delete();
    }

    public boolean deletaArquivo(String arquivo) throws Exception {
        return new File(arquivo).delete();
    }

    /**
	 *	Apaga um diretorio 
	 */
    public static boolean apagarDiretorio(String dir) {
        return new File(dir).delete();
    }

    /**
	 *	Cria diretorio: Todos os diret�rios ancestrais que n�o existam ser�o criados automaticamente. 
	 */
    public static boolean criarDiretorio(String dir) {
        return new File(dir).mkdir();
    }

    public static void verificaCriaDiretorio(String dir) {
        try {
            if (!(new File(uploadDiretorio.concat(dir)).exists())) {
                new File(uploadDiretorio.concat(dir)).mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copiaAnexos(String from, String to, AnexoTO[] anexoTO) {
        FileChannel in = null, out = null;
        for (int i = 0; i < anexoTO.length; i++) {
            try {
                in = new FileInputStream(new File((uploadDiretorio.concat(from)).concat(File.separator + anexoTO[i].getNome()))).getChannel();
                out = new FileOutputStream(new File((uploadDiretorio.concat(to)).concat(File.separator + anexoTO[i].getNome()))).getChannel();
                long size = in.size();
                MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
                out.write(buf);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (out != null) try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getMimeType(String fileName) {
        File f = new File(fileName);
        return new MimetypesFileTypeMap().getContentType(f);
    }
}
