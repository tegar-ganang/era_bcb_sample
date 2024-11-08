package es.caib.regweb;

import java.util.*;
import java.text.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.naming.*;
import java.rmi.*;
import javax.ejb.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Bean que gestiona la modificació del registre d'entrada
 * @author  FJMARTINEZ
 * @version 1.0
 */
public class RegistroModificadoEntradaBean implements SessionBean {

    private SessionContext sessioEjb;

    private int anoEntrada;

    private static final String TIPO_REGISTRO = "E";

    private String entidad1;

    private String entidad1Catalan;

    private int entidad2;

    private String extracto;

    private String usuarioModificacion;

    private String usuarioVisado;

    private String indVisExtracto;

    private String indVisRemitente;

    private int numeroRegistro;

    private String remitente;

    private String motivo;

    private String fechaModificacion;

    private String horaModificacion;

    private boolean leido = false;

    private boolean hayVisadoRemitente = false;

    private boolean hayVisadoExtracto = false;

    private String idiomaExtracto = "";

    private int fechaDocumento = 0;

    private String tipoDocumento = "";

    private int fechaRegistro = 0;

    private int fzacagge = 0;

    private int destinatario = 0;

    private String idioma = "";

    private String fora = "";

    private String comentario = "";

    private String altres = "";

    private String entidad1Old = "";

    private int entidad2Old = 0;

    private String password = "";

    /**
	 * Holds value of property oficina.
	 */
    private int oficina;

    public void setAnoEntrada(int anoEntrada) {
        this.anoEntrada = anoEntrada;
    }

    public void setOficina(int oficina) {
        this.oficina = oficina;
    }

    public void fijaPasswordUser(String password) {
        this.password = password;
    }

    public void setEntidad1(String entidad1) {
        this.entidad1 = entidad1;
    }

    public void setExtracto(String extracto) {
        this.extracto = extracto;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public void setUsuarioVisado(String usuarioVisado) {
        this.usuarioVisado = usuarioVisado;
    }

    public void setIndVisExtracto(String indVisExtracto) {
        this.indVisExtracto = indVisExtracto;
    }

    public void setIndVisRemitente(String indVisRemitente) {
        this.indVisRemitente = indVisRemitente;
    }

    public void setEntidad2(int entidad2) {
        this.entidad2 = entidad2;
    }

    public void setNumeroRegistro(int numeroRegistro) {
        this.numeroRegistro = numeroRegistro;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public void setFechaModificacion(String fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public void setHoraModificacion(String horaModificacion) {
        this.horaModificacion = horaModificacion;
    }

    public void setVisarRemitente(boolean hayVisadoRemitente) {
        this.hayVisadoRemitente = hayVisadoRemitente;
    }

    public void setVisarExtracto(boolean hayVisadoExtracto) {
        this.hayVisadoExtracto = hayVisadoExtracto;
    }

    public RegistroModificadoEntradaBean() {
    }

    public void leer() {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = ToolsBD.getConn();
            String sentenciaSql = "SELECT * FROM BZMODIF WHERE FZJCENSA='E' AND FZJCAGCO=? AND FZJNUMEN=? AND FZJANOEN=? AND" + " FZJFMODI=? AND FZJHMODI=?";
            ps = conn.prepareStatement(sentenciaSql);
            ps.setInt(1, oficina);
            ps.setInt(2, numeroRegistro);
            ps.setInt(3, anoEntrada);
            ps.setString(4, fechaModificacion);
            ps.setString(5, horaModificacion);
            rs = ps.executeQuery();
            if (rs.next()) {
                leido = true;
                anoEntrada = rs.getInt("FZJANOEN");
                entidad1 = rs.getString("FZJCENTI");
                entidad2 = rs.getInt("FZJNENTI");
                entidad1Catalan = ToolsBD.convierteEntidad(entidad1, conn);
                extracto = rs.getString("FZJCONEN");
                usuarioModificacion = rs.getString("FZJCUSMO");
                usuarioVisado = rs.getString("FZJCUSVI");
                indVisRemitente = rs.getString("FZJIREMI");
                indVisExtracto = rs.getString("FZJIEXTR");
                numeroRegistro = rs.getInt("FZJNUMEN");
                remitente = rs.getString("FZJREMIT");
                motivo = rs.getString("FZJTEXTO");
                oficina = rs.getInt("FZJCAGCO");
                if (!(rs.getString("FZJIREMI").equals(" ") || rs.getString("FZJIREMI").equals(""))) {
                    entidad1 = "";
                    entidad1Catalan = "";
                    entidad2 = 0;
                    remitente = "";
                }
                if (!(rs.getString("FZJIEXTR").equals(" ") || rs.getString("FZJIEXTR").equals(""))) {
                    extracto = "";
                }
                Date fechaSystem = new Date();
                DateFormat aaaammdd = new SimpleDateFormat("yyyyMMdd");
                DateFormat hhmmss = new SimpleDateFormat("HHmmss");
                DateFormat sss = new SimpleDateFormat("S");
                String ss = sss.format(fechaSystem);
                int fzahsis = Integer.parseInt(hhmmss.format(fechaSystem) + ss);
                String Stringsss = sss.format(fechaSystem);
                switch(Stringsss.length()) {
                    case (1):
                        Stringsss = "00" + Stringsss;
                        break;
                    case (2):
                        Stringsss = "0" + Stringsss;
                        break;
                }
                int horamili = Integer.parseInt(hhmmss.format(fechaSystem) + Stringsss);
                int fzafsis = Integer.parseInt(aaaammdd.format(fechaSystem));
                logLopdBZMODIF("SELECT", sessioEjb.getCallerPrincipal().getName().toUpperCase(), fzafsis, horamili, 'E', numeroRegistro, anoEntrada, oficina, Integer.parseInt(fechaModificacion), Integer.parseInt(horaModificacion));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            leido = false;
        } finally {
            ToolsBD.closeConn(conn, ps, rs);
        }
    }

    public boolean visar() throws SQLException, ClassNotFoundException, Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        Date fechaSystem = new Date();
        DateFormat aaaammdd = new SimpleDateFormat("yyyyMMdd");
        DateFormat hhmmss = new SimpleDateFormat("HHmmss");
        DateFormat sss = new SimpleDateFormat("S");
        String ss = sss.format(fechaSystem);
        if (ss.length() > 2) {
            ss = ss.substring(0, 2);
        }
        boolean visado = false;
        try {
            conn = ToolsBD.getConn();
            conn.setAutoCommit(false);
            String sentenciaSql = "UPDATE BZMODIF SET FZJCUSVI=?, FZJFVISA=?, FZJHVISA=?" + ((hayVisadoExtracto) ? ", FZJIEXTR=?" : "") + ((hayVisadoRemitente) ? ", FZJIREMI=?" : "") + " WHERE FZJCENSA='E' AND FZJCAGCO=? AND FZJNUMEN=? AND FZJANOEN=? AND FZJFMODI=? AND FZJHMODI=?";
            ps = conn.prepareStatement(sentenciaSql);
            ps.setString(1, usuarioVisado);
            ps.setInt(2, Integer.parseInt(aaaammdd.format(fechaSystem)));
            ps.setInt(3, Integer.parseInt(hhmmss.format(fechaSystem) + ss));
            int contador = 4;
            if (hayVisadoExtracto) {
                ps.setString(contador++, "X");
            }
            if (hayVisadoRemitente) {
                ps.setString(contador++, "X");
            }
            ps.setInt(contador++, oficina);
            ps.setInt(contador++, numeroRegistro);
            ps.setInt(contador++, anoEntrada);
            ps.setString(contador++, fechaModificacion);
            ps.setString(contador++, horaModificacion);
            int registrosAfectados = ps.executeUpdate();
            if (registrosAfectados > 0 && !hayVisadoExtracto && !hayVisadoRemitente) {
                visado = true;
            }
            if (registrosAfectados > 0 && (hayVisadoExtracto || hayVisadoRemitente)) {
                boolean generado = generarBZVISAD(conn, Integer.parseInt(aaaammdd.format(fechaSystem)), Integer.parseInt(hhmmss.format(fechaSystem) + ss));
                if (generado) {
                    visado = actualizarBZENTRA(conn);
                }
                String rem = "";
                String com = "";
                if (hayVisadoRemitente) {
                    if (!remitente.trim().equals("")) {
                        rem = remitente;
                    } else {
                        javax.naming.InitialContext contexto = new javax.naming.InitialContext();
                        Object ref = contexto.lookup("es.caib.regweb.ValoresHome");
                        ValoresHome home = (ValoresHome) javax.rmi.PortableRemoteObject.narrow(ref, ValoresHome.class);
                        Valores valor = home.create();
                        rem = valor.recuperaRemitenteCastellano(entidad1, entidad2 + "");
                        valor.remove();
                    }
                } else {
                    if (!altres.trim().equals("")) {
                        rem = remitente;
                    } else {
                        javax.naming.InitialContext contexto = new javax.naming.InitialContext();
                        Object ref = contexto.lookup("es.caib.regweb.ValoresHome");
                        ValoresHome home = (ValoresHome) javax.rmi.PortableRemoteObject.narrow(ref, ValoresHome.class);
                        Valores valor = home.create();
                        rem = valor.recuperaRemitenteCastellano(entidad1Old, entidad2Old + "");
                        valor.remove();
                    }
                }
                if (hayVisadoExtracto) {
                    com = extracto;
                } else {
                    com = comentario;
                }
                try {
                    Class t = Class.forName("es.caib.regweb.module.PluginHook");
                    Class[] partypes = { String.class, Integer.class, Integer.class, Integer.class, Integer.class, String.class, String.class, String.class, Integer.class, Integer.class, String.class, Integer.class, String.class, String.class, Integer.class, Integer.class, Integer.class, String.class, String.class, String.class };
                    Object[] params = { "V", new Integer(anoEntrada), new Integer(numeroRegistro), new Integer(oficina), new Integer(fechaDocumento), rem, com, tipoDocumento, new Integer(fechaRegistro), new Integer(fzacagge), fora, new Integer(destinatario), idioma, null, null, null, null, null, null, null };
                    java.lang.reflect.Method metodo = t.getMethod("entrada", partypes);
                    metodo.invoke(null, params);
                } catch (IllegalAccessException iae) {
                } catch (IllegalArgumentException iae) {
                } catch (InvocationTargetException ite) {
                } catch (NullPointerException npe) {
                } catch (ExceptionInInitializerError eiie) {
                } catch (NoSuchMethodException nsme) {
                } catch (SecurityException se) {
                } catch (LinkageError le) {
                } catch (ClassNotFoundException le) {
                }
            }
            conn.commit();
            int fzahsis = Integer.parseInt(hhmmss.format(fechaSystem) + ss);
            String Stringsss = sss.format(fechaSystem);
            switch(Stringsss.length()) {
                case (1):
                    Stringsss = "00" + Stringsss;
                    break;
                case (2):
                    Stringsss = "0" + Stringsss;
                    break;
            }
            int horamili = Integer.parseInt(hhmmss.format(fechaSystem) + Stringsss);
            int fzafsis = Integer.parseInt(aaaammdd.format(fechaSystem));
            logLopdBZMODIF("UPDATE", sessioEjb.getCallerPrincipal().getName().toUpperCase(), fzafsis, horamili, 'E', numeroRegistro, anoEntrada, oficina, Integer.parseInt(fechaModificacion), Integer.parseInt(horaModificacion));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            visado = false;
            try {
                if (conn != null) conn.rollback(); else System.out.println("ERROR: No es pot fer rollback sense connexió!");
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        } finally {
            ToolsBD.closeConn(conn, ps, null);
        }
        return visado;
    }

    private boolean actualizarBZENTRA(Connection conn) throws SQLException, ClassNotFoundException, Exception {
        boolean generado = false;
        PreparedStatement ps = null;
        Date fechaSystem = new Date();
        DateFormat hhmmss = new SimpleDateFormat("HHmmss");
        DateFormat aaaammdd = new SimpleDateFormat("yyyyMMdd");
        String entidad1Valor = "";
        int entidad2Valor = 0;
        String remitenteValor = "";
        if (hayVisadoRemitente) {
            if (entidad1.trim().equals("")) {
                remitenteValor = remitente;
                entidad1Valor = " ";
                entidad2Valor = 0;
            } else {
                remitenteValor = "";
                entidad1Valor = entidad1;
                entidad2Valor = entidad2;
            }
        }
        String actualizaBZENTRA = "UPDATE BZENTRA SET FZAFACTU=? " + ((hayVisadoExtracto && idiomaExtracto.equals("1")) ? ", FZACONEN=?" : "") + ((hayVisadoExtracto && !idiomaExtracto.equals("1")) ? ", FZACONE2=?" : "") + ((hayVisadoRemitente) ? ", FZAREMIT=?, FZACENTI=?, FZANENTI=?" : "") + " WHERE FZANUMEN=? AND FZACAGCO=? AND FZAANOEN=?";
        try {
            ps = conn.prepareStatement(actualizaBZENTRA);
            int contador = 1;
            ps.setInt(contador++, Integer.parseInt(aaaammdd.format(fechaSystem)));
            if (hayVisadoExtracto) {
                ps.setString(contador++, extracto);
            }
            if (hayVisadoRemitente) {
                ps.setString(contador++, remitenteValor);
                ps.setString(contador++, entidad1Valor);
                ps.setInt(contador++, entidad2Valor);
            }
            ps.setInt(contador++, numeroRegistro);
            ps.setInt(contador++, oficina);
            ps.setInt(contador++, anoEntrada);
            int registrosAfectados = ps.executeUpdate();
            ps.close();
            generado = (registrosAfectados > 0) ? true : false;
            DateFormat sss = new SimpleDateFormat("S");
            String ss = sss.format(fechaSystem);
            int fzahsis = Integer.parseInt(hhmmss.format(fechaSystem) + ss);
            String Stringsss = sss.format(fechaSystem);
            switch(Stringsss.length()) {
                case (1):
                    Stringsss = "00" + Stringsss;
                    break;
                case (2):
                    Stringsss = "0" + Stringsss;
                    break;
            }
            int horamili = Integer.parseInt(hhmmss.format(fechaSystem) + Stringsss);
            int fzafsis = Integer.parseInt(aaaammdd.format(fechaSystem));
            logLopdBZENTRA("UPDATE", usuarioModificacion, fzafsis, horamili, numeroRegistro, anoEntrada, oficina);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            generado = false;
            e.printStackTrace();
            throw new Exception("S'ha produ\357t un error actualizant BZENTRA");
        }
        return generado;
    }

    private boolean generarBZVISAD(Connection conn, int fecha, int hora) throws SQLException, ClassNotFoundException, Exception {
        boolean generado = false;
        PreparedStatement ps = null;
        String insertBZVISAD = "INSERT INTO BZVISAD (FZKANOEN, FZKCAGCO, FZKCENSA, FZKCENTF, FZKCENTI, FZKNENTF, FZKNENTI, " + "FZKREMIF, FZKREMII, FZKCONEF, FZKCONEI, FZKCUSVI, FZKFENTF, FZKFENTI, FZKFVISA, FZKHVISA,  FZKNUMEN, " + "FZKTEXTO) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            javax.naming.InitialContext contexto = new javax.naming.InitialContext();
            Object ref = contexto.lookup("es.caib.regweb.RegistroEntradaHome");
            RegistroEntradaHome home = (RegistroEntradaHome) javax.rmi.PortableRemoteObject.narrow(ref, RegistroEntradaHome.class);
            RegistroEntrada registro = home.create();
            registro.fijaUsuario(usuarioVisado);
            registro.setoficina(oficina + "");
            registro.setNumeroEntrada(numeroRegistro + "");
            registro.setAnoEntrada(anoEntrada + "");
            registro.leer();
            if (!registro.getLeido()) {
                throw new Exception("S'ha produ\357t un error i no s'han pogut crear el objecte RegistroEntrada");
            }
            if (registro.getData().equals("0")) {
                fechaDocumento = 0;
            } else {
                java.util.Date fechaTest = null;
                DateFormat yyyymmdd = new SimpleDateFormat("yyyyMMdd");
                DateFormat ddmmyyyy = new SimpleDateFormat("dd/MM/yyyy");
                fechaTest = ddmmyyyy.parse(registro.getData());
                fechaDocumento = Integer.parseInt(yyyymmdd.format(fechaTest));
            }
            if (registro.getDataEntrada().equals("0")) {
                fechaRegistro = 0;
            } else {
                java.util.Date fechaTest = null;
                DateFormat yyyymmdd = new SimpleDateFormat("yyyyMMdd");
                DateFormat ddmmyyyy = new SimpleDateFormat("dd/MM/yyyy");
                fechaTest = ddmmyyyy.parse(registro.getDataEntrada());
                fechaRegistro = Integer.parseInt(yyyymmdd.format(fechaTest));
            }
            tipoDocumento = registro.getTipo();
            fzacagge = Integer.parseInt(registro.getBalears());
            idiomaExtracto = registro.getIdioex();
            fora = registro.getFora();
            destinatario = Integer.parseInt(registro.getDestinatari());
            comentario = registro.getComentario();
            idioma = registro.getIdioma();
            altres = registro.getAltres();
            entidad1Old = registro.getEntidad1();
            entidad2Old = Integer.parseInt(registro.getEntidad2());
            ps = conn.prepareStatement(insertBZVISAD);
            ps.setInt(1, anoEntrada);
            ps.setInt(2, oficina);
            ps.setString(3, TIPO_REGISTRO);
            ps.setString(4, (hayVisadoRemitente) ? entidad1 : " ");
            ps.setString(5, (hayVisadoRemitente) ? registro.getEntidad1Grabada() : " ");
            ps.setInt(6, (hayVisadoRemitente) ? entidad2 : 0);
            ps.setInt(7, (hayVisadoRemitente) ? Integer.parseInt(registro.getEntidad2()) : 0);
            ps.setString(8, (hayVisadoRemitente) ? remitente : " ");
            ps.setString(9, (hayVisadoRemitente) ? registro.getAltres() : " ");
            ps.setString(10, (hayVisadoExtracto) ? extracto : " ");
            ps.setString(11, (hayVisadoExtracto) ? registro.getComentario() : " ");
            ps.setString(12, usuarioVisado);
            ps.setInt(13, 0);
            ps.setInt(14, 0);
            ps.setInt(15, fecha);
            ps.setInt(16, hora);
            ps.setInt(17, numeroRegistro);
            ps.setString(18, motivo);
            ps.execute();
            generado = true;
            registro.remove();
            ps.close();
        } catch (Exception e) {
            generado = false;
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("S'ha produ\357t un error insert BZVISAD");
        }
        return generado;
    }

    public List recuperarRegistros(String oficina, String usuario) {
        List registros = new ArrayList();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sentenciaSql = "";
        String fecha = "";
        java.util.Date fechaDocumento = null;
        DateFormat yyyymmdd = new SimpleDateFormat("yyyyMMdd");
        DateFormat ddmmyyyy = new SimpleDateFormat("dd/MM/yyyy");
        if (oficina.equals("00")) {
            sentenciaSql = "SELECT * FROM BZMODIF WHERE (FZJIEXTR=' ' OR FZJIREMI=' ' OR FZJIEXTR='' OR FZJIREMI='') AND FZJFVISA=0 AND FZJCAGCO " + "IN (SELECT FZHCAGCO FROM BZAUTOR WHERE FZHCUSU=? AND FZHCAUT=?) AND FZJCENSA='E' ORDER BY " + "FZJCAGCO, FZJANOEN, FZJNUMEN, FZJFMODI, FZJHMODI";
        } else {
            sentenciaSql = "SELECT * FROM BZMODIF WHERE FZJCAGCO=? AND FZJFVISA=0 AND (FZJIEXTR=' ' OR FZJIREMI=' ' OR FZJIEXTR='' OR FZJIREMI='') AND FZJCENSA='E' ORDER BY " + "FZJCAGCO, FZJANOEN, FZJNUMEN, FZJFMODI, FZJHMODI";
        }
        try {
            conn = ToolsBD.getConn();
            ps = conn.prepareStatement(sentenciaSql);
            if (oficina.equals("00")) {
                ps.setString(1, usuario);
                ps.setString(2, "VE");
            } else {
                ps.setInt(1, Integer.parseInt(oficina));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                RegistroModificadoSeleccionado reg = new RegistroModificadoSeleccionado();
                reg.setNumeroOficina(rs.getInt("FZJCAGCO"));
                reg.setNumeroRegistro(rs.getInt("FZJNUMEN"));
                reg.setAnoRegistro(rs.getInt("FZJANOEN"));
                if ((!rs.getString("FZJCENTI").trim().equals("") || !rs.getString("FZJREMIT").trim().equals("")) && rs.getString("FZJIREMI").equals(" ")) {
                    reg.setVisadoR("*");
                } else {
                    reg.setVisadoR("");
                }
                if (!rs.getString("FZJCONEN").trim().equals("") && rs.getString("FZJIEXTR").equals(" ")) {
                    reg.setVisadoC("*");
                } else {
                    reg.setVisadoC("");
                }
                fecha = String.valueOf(rs.getInt("FZJFMODI"));
                reg.setFechaModif(rs.getInt("FZJFMODI"));
                reg.setHoraModif(rs.getInt("FZJHMODI"));
                try {
                    fechaDocumento = yyyymmdd.parse(fecha);
                    reg.setFechaModificacion(ddmmyyyy.format(fechaDocumento));
                } catch (Exception e) {
                    reg.setFechaModificacion(fecha);
                }
                reg.setMotivoCambio(rs.getString("FZJTEXTO"));
                if ((rs.getString("FZJIREMI").equals(" ") && (!rs.getString("FZJCENTI").trim().equals("") || !rs.getString("FZJREMIT").trim().equals(""))) || (rs.getString("FZJIEXTR").equals(" ") && !rs.getString("FZJCONEN").trim().equals(""))) {
                    registros.add(reg);
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ToolsBD.closeConn(conn, ps, rs);
        }
        return registros;
    }

    public boolean generarModificacion(Connection conn) {
        boolean generado = false;
        PreparedStatement ps = null;
        String insertBZMODIF = "INSERT INTO BZMODIF (FZJANOEN, FZJCAGCO, FZJCENSA, FZJCENTI, FZJCONEN, FZJCUSMO, " + "FZJCUSVI, FZJFMODI, FZJFVISA, FZJHMODI, FZJHVISA, FZJIEXTR, FZJIREMI, FZJNENTI, FZJNUMEN, FZJREMIT, " + "FZJTEXTO) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Date fechaSystem = new Date();
        DateFormat aaaammdd = new SimpleDateFormat("yyyyMMdd");
        DateFormat hhmmss = new SimpleDateFormat("HHmmss");
        DateFormat sss = new SimpleDateFormat("S");
        String ss = sss.format(fechaSystem);
        if (ss.length() > 2) {
            ss = ss.substring(0, 2);
        }
        try {
            ps = conn.prepareStatement(insertBZMODIF);
            ps.setInt(1, anoEntrada);
            ps.setInt(2, oficina);
            ps.setString(3, TIPO_REGISTRO);
            ps.setString(4, entidad1);
            ps.setString(5, extracto);
            ps.setString(6, usuarioModificacion);
            ps.setString(7, "");
            ps.setInt(8, Integer.parseInt(aaaammdd.format(fechaSystem)));
            ps.setInt(9, 0);
            ps.setInt(10, Integer.parseInt(hhmmss.format(fechaSystem) + ss));
            ps.setInt(11, 0);
            ps.setString(12, "");
            ps.setString(13, "");
            ps.setInt(14, entidad2);
            ps.setInt(15, numeroRegistro);
            ps.setString(16, remitente);
            ps.setString(17, motivo);
            ps.execute();
            String Stringsss = sss.format(fechaSystem);
            switch(Stringsss.length()) {
                case (1):
                    Stringsss = "00" + Stringsss;
                    break;
                case (2):
                    Stringsss = "0" + Stringsss;
                    break;
            }
            int horamili = Integer.parseInt(hhmmss.format(fechaSystem) + Stringsss);
            logLopdBZMODIF("INSERT", usuarioModificacion, Integer.parseInt(aaaammdd.format(fechaSystem)), horamili, 'E', numeroRegistro, anoEntrada, oficina, Integer.parseInt(aaaammdd.format(fechaSystem)), Integer.parseInt(hhmmss.format(fechaSystem) + ss));
            generado = true;
        } catch (Exception e) {
            System.out.println("RegistroModificadoEntradaBean: Excepción al generar modificacion " + e.getMessage());
            e.printStackTrace();
            generado = false;
        } finally {
            ToolsBD.closeConn(null, ps, null);
        }
        return generado;
    }

    public boolean getLeido() {
        return leido;
    }

    public int getNumeroRegistro() {
        return numeroRegistro;
    }

    public int getAnoEntrada() {
        return anoEntrada;
    }

    public int getOficina() {
        return oficina;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getEntidad1Catalan() {
        return entidad1Catalan;
    }

    public String getEntidad1() {
        return entidad1;
    }

    public int getEntidad2() {
        return entidad2;
    }

    public String getRemitente() {
        return remitente;
    }

    public String getExtracto() {
        return extracto;
    }

    public void ejbCreate() throws CreateException {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void ejbRemove() {
    }

    public void setSessionContext(SessionContext ctx) {
        sessioEjb = ctx;
    }

    /**
	 * Emplena la taula de control d'accés complint la llei LOPD per la taula BZMODIF 
	 * @param tipusAcces <code>String</code> tipus d'accés a la taula
	 * @param usuari <code>String</code> codi de l'usuari que fa l'acció.
	 * @param data <code>Intr</code> data d'accés en format numèric (ddmmyyyy)
	 * @param hora <code>Int</code> hora d'accés en format numèric (hhmissmis, hora (2 posicions), minut (2 posicions), segons (2 posicions), milisegons (3 posicions)
	 * @param entrsal <code>char</code> Caràcter que indica si és una entrada o una sortida.
	 * @param nombreRegistre <code>Int</code> nombre de registre
	 * @param any <code>Int</code> any del registre
	 * @param oficina <code>Int</code> oficina on s'ha registrat
	 * @author Sebastià Matas Riera (bitel)
	 */
    private void logLopdBZMODIF(String tipusAcces, String usuari, int data, int hora, char entrsal, int nombreRegistre, int any, int oficina, int dataModif, int horaModif) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = ToolsBD.getConn();
            String sentenciaSql = "INSERT INTO BZMOLPD (FZVTIPAC, FZVCUSU, " + "FZVDATAC, FZVHORAC, FZVCENSA, " + "FZVNUMEN, FZVANOEN, FZVCAGCO, FZVFMODI, FZVHMODI ) " + "VALUES (?,?,?,?,?,?,?,?,?,?)";
            ps = conn.prepareStatement(sentenciaSql);
            ps.setString(1, tipusAcces);
            ps.setString(2, usuari);
            ps.setInt(3, data);
            ps.setInt(4, hora);
            ps.setString(5, "" + entrsal);
            ps.setInt(6, nombreRegistre);
            ps.setInt(7, any);
            ps.setInt(8, oficina);
            ps.setInt(9, dataModif);
            ps.setInt(10, horaModif);
            ps.execute();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: S'ha produ\357t un error a logLopdBZENTRA.");
        } finally {
            ToolsBD.closeConn(conn, ps, rs);
        }
    }

    /**
     * Emplena la taula de control d'accés complint la llei LOPD per la taula BZENTRA
     * @param tipusAcces <code>String</code> tipus d'accés a la taula
     * @param usuari <code>String</code> codi de l'usuari que fa l'acció.
     * @param data <code>Intr</code> data d'accés en format numèric (ddmmyyyy)
     * @param hora <code>Int</code> hora d'accés en format numèric (hhmissmis, hora (2 posicions), minut (2 posicions), segons (2 posicions), milisegons (3 posicions)
     * @param nombreRegistre <code>Int</code> nombre de registre
     * @param any <code>Int</code> any del registre
     * @param oficina <code>Int</code> oficina on s'ha registrat
     * @author Sebastià Matas Riera (bitel)
     */
    private void logLopdBZENTRA(String tipusAcces, String usuari, int data, int hora, int nombreRegistre, int any, int oficina) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = ToolsBD.getConn();
            String sentenciaSql = "INSERT INTO BZENLPD (FZTTIPAC, FZTCUSU, FZTDATAC, FZTHORAC, FZTNUMEN, FZTANOEN," + " FZTCAGCO) VALUES (?,?,?,?,?,?,?)";
            ps = conn.prepareStatement(sentenciaSql);
            ps.setString(1, tipusAcces);
            ps.setString(2, usuari);
            ps.setInt(3, data);
            ps.setInt(4, hora);
            ps.setInt(5, nombreRegistre);
            ps.setInt(6, any);
            ps.setInt(7, oficina);
            ps.execute();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("RegistroModificadoEntradaBean: ERROR: S'ha produ\357t un error a logLopdBZENTRA");
        } finally {
            ToolsBD.closeConn(conn, ps, rs);
        }
    }
}
