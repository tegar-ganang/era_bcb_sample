package es.caib.regweb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.text.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.naming.*;
import java.rmi.*;
import javax.ejb.*;
import javax.ejb.SessionContext;
import java.util.Vector;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Bean que gestiona el registre d'entrada
 * @author  FJMARTINEZ
 * @version 1.0
 */
public class RegistroEntradaBean implements SessionBean {

    private SessionContext context;

    private String dataVisado = "";

    private String dataentrada = "";

    private String hora = "";

    private String oficina = "";

    private String oficinafisica = "0";

    private String data = "";

    private String tipo = "";

    private String idioma = "";

    private String entidad1 = "";

    private String entidad1Grabada = "";

    private String entidad2 = "";

    private String altres = "";

    private String balears = "";

    private String fora = "";

    private String salida1 = "";

    private String salida2 = "";

    private String destinatari = "";

    private String idioex = "";

    private String disquet = "";

    private String comentario = "";

    private String usuario;

    private int fzanume = 0;

    private String correo = "";

    private String registroAnulado = "";

    private boolean actualizacion = false;

    private boolean leidos = false;

    private String motivo = "";

    private String entidad1Nuevo = "";

    private String entidad2Nuevo = "";

    private String altresNuevo = "";

    private String comentarioNuevo = "";

    private String password = "";

    private String municipi060 = "";

    private String descripcioMunicipi060 = "";

    private int numeroRegistros060 = 0;

    private SessionContext contextoSesion;

    private boolean error = false;

    private boolean validado = false;

    private boolean registroGrabado = false;

    private boolean registroActualizado = false;

    private Hashtable errores = new Hashtable();

    private DateFormat dateF = new SimpleDateFormat("dd/MM/yyyy");

    private Date fechaTest = null;

    private DateFormat horaF = new SimpleDateFormat("HH:mm");

    private Date horaTest = null;

    private String entidadCastellano;

    private String SENTENCIA = "INSERT INTO BZENTRA (" + "FZAANOEN, FZANUMEN, FZACAGCO, FZAFDOCU, FZAREMIT, FZACONEN, FZACTIPE, FZACEDIE, FZAENULA," + "FZAPROCE, FZAFENTR, FZACTAGG, FZACAGGE, FZACORGA, FZAFACTU, FZACENTI, FZANENTI, FZAHORA," + "FZACIDIO, FZACONE2, FZANLOC, FZAALOC, FZANDIS, FZAFSIS, FZAHSIS, FZACUSU, FZACIDI" + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private String anoEntrada = null;

    private String numeroEntrada = null;

    private String descripcionOficina = null;

    private String descripcionOficinaFisica = null;

    private String descripcionRemitente = null;

    private String descripcionOrganismoDestinatario = null;

    private String descripcionDocumento = null;

    private String descripcionIdiomaDocumento = null;

    private String procedenciaGeografica = null;

    private String idiomaExtracto = null;

    private String BOIBdata = "";

    private int BOIBnumeroBOCAIB = 0;

    private int BOIBpagina = 0;

    private int BOIBlineas = 0;

    private String BOIBtexto = "";

    private String BOIBobservaciones = "";

    public RegistroEntradaBean() {
    }

    public void setBOIBdata(String BOIBdata) {
        this.BOIBdata = BOIBdata;
    }

    public void setBOIBnumeroBOCAIB(int BOIBnumeroBOCAIB) {
        this.BOIBnumeroBOCAIB = BOIBnumeroBOCAIB;
    }

    public void setBOIBpagina(int BOIBpagina) {
        this.BOIBpagina = BOIBpagina;
    }

    public void setBOIBlineas(int BOIBlineas) {
        this.BOIBlineas = BOIBlineas;
    }

    public void setBOIBtexto(String BOIBtexto) {
        this.BOIBtexto = BOIBtexto;
    }

    public void setBOIBobservaciones(String BOIBobservaciones) {
        this.BOIBobservaciones = BOIBobservaciones;
    }

    /**
     * @param anoEntrada
     */
    public void setAnoEntrada(String anoEntrada) {
        this.anoEntrada = anoEntrada;
    }

    public void setActualizacion(boolean actualizacion) {
        this.actualizacion = actualizacion;
    }

    /**
     * @param correo
     */
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    /**
     * @param registroAnulado
     */
    public void setRegistroAnulado(String registroAnulado) {
        this.registroAnulado = registroAnulado;
    }

    /**
     * @param numeroEntrada
     */
    public void setNumeroEntrada(String numeroEntrada) {
        this.numeroEntrada = numeroEntrada;
    }

    /**
     * @param descripcionOficina
     */
    public void setDescripcionOficina(String descripcionOficina) {
        this.descripcionOficina = descripcionOficina;
    }

    /**
     * @param descripcionOficinaFisica
     */
    public void setDescripcionOficinaFisica(String descripcionOficinaFisica) {
        this.descripcionOficinaFisica = descripcionOficinaFisica;
    }

    /**
     * @param descripcionRemitente
     */
    public void setDescripcionRemitente(String descripcionRemitente) {
        this.descripcionRemitente = descripcionRemitente;
    }

    /**
     * @param descripcionOrganismoDestinatario
     */
    public void setDescripcionOrganismoDestinatario(String descripcionOrganismoDestinatario) {
        this.descripcionOrganismoDestinatario = descripcionOrganismoDestinatario;
    }

    /**
     * @param descripcionDocumento
     */
    public void setDescripcionDocumento(String descripcionDocumento) {
        this.descripcionDocumento = descripcionDocumento;
    }

    /**
     * @param descripcionIdiomaDocumento
     */
    public void setDescripcionIdiomaDocumento(String descripcionIdiomaDocumento) {
        this.descripcionIdiomaDocumento = descripcionIdiomaDocumento;
    }

    /**
     * @param procedenciaGeografica
     */
    public void setProcedenciaGeografica(String procedenciaGeografica) {
        this.procedenciaGeografica = procedenciaGeografica;
    }

    /**
     * @param idiomaExtracto
     */
    public void setIdiomaExtracto(String idiomaExtracto) {
        this.idiomaExtracto = idiomaExtracto;
    }

    /**
     * @param dataentrada
     */
    public void setdataentrada(String dataentrada) {
        this.dataentrada = dataentrada;
    }

    /**
     * @param dataVisado
     */
    public void setDataVisado(String dataVisado) {
        this.dataVisado = dataVisado;
    }

    /**
     * @param hora
     */
    public void sethora(String hora) {
        this.hora = hora;
    }

    /**
     * @param oficina
     */
    public void setoficina(String oficina) {
        this.oficina = oficina;
    }

    /**
     * @param oficinafisica
     */
    public void setoficinafisica(String oficinafisica) {
        this.oficinafisica = oficinafisica;
    }

    /**
     * @param data
     */
    public void setdata(String data) {
        this.data = data;
    }

    /**
     * @param tipo
     */
    public void settipo(String tipo) {
        this.tipo = tipo.toUpperCase();
    }

    /**
     * @param idioma
     */
    public void setidioma(String idioma) {
        this.idioma = idioma;
    }

    /**
     * @param entidad1
     */
    public void setentidad1(String entidad1) {
        this.entidad1 = entidad1.toUpperCase();
    }

    /**
     * @param entidad2
     */
    public void setentidad2(String entidad2) {
        this.entidad2 = entidad2;
    }

    /**
     * @param altres
     */
    public void setaltres(String altres) {
        this.altres = altres;
    }

    /**
     * @param balears
     */
    public void setbalears(String balears) {
        this.balears = balears;
    }

    /**
     * @param fora
     */
    public void setfora(String fora) {
        this.fora = fora;
    }

    /**
     * @param salida1
     */
    public void setsalida1(String salida1) {
        this.salida1 = salida1;
    }

    /**
     * @param salida2
     */
    public void setsalida2(String salida2) {
        this.salida2 = salida2;
    }

    /**
     * @param destinatari
     */
    public void setdestinatari(String destinatari) {
        this.destinatari = destinatari;
    }

    /**
     * @param idioex
     */
    public void setidioex(String idioex) {
        this.idioex = idioex;
    }

    /**
     * @param disquet
     */
    public void setdisquet(String disquet) {
        this.disquet = disquet;
    }

    /**
     * @param comentario
     */
    public void setcomentario(String comentarioEntero) {
        if (comentarioEntero.length() > 160) {
            comentarioEntero = comentarioEntero.substring(0, 160);
        }
        this.comentario = comentarioEntero;
    }

    /**
     * @param motivo
     */
    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    /**
     * @param entidad1Nuevo
     */
    public void setEntidad1Nuevo(String entidad1Nuevo) {
        this.entidad1Nuevo = entidad1Nuevo;
    }

    /**
     * @param entidad1Nuevo
     */
    public void setEntidad2Nuevo(String entidad2Nuevo) {
        this.entidad2Nuevo = entidad2Nuevo;
    }

    /**
     * @param altresNuevo
     */
    public void setAltresNuevo(String altresNuevo) {
        this.altresNuevo = altresNuevo;
    }

    /**
     * @param municipi060
     */
    public void setMunicipi060(String municipi060) {
        this.municipi060 = municipi060;
    }

    /**
     * @param comentarioNuevo
     */
    public void setComentarioNuevo(String comentarioNuevoEntero) {
        if (comentarioNuevoEntero.length() > 160) {
            comentarioNuevoEntero = comentarioNuevoEntero.substring(0, 160);
        }
        this.comentarioNuevo = comentarioNuevoEntero;
    }

    /**
     * @param usuario
     */
    public void fijaUsuario(String usuario) {
        this.usuario = usuario.toUpperCase();
    }

    public void fijaPasswordUser(String password) {
        this.password = password;
    }

    /**
     * @return
     */
    public boolean validar() {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        validado = false;
        entidadCastellano = null;
        errores.clear();
        try {
            conn = ToolsBD.getConn();
            validarFecha(dataentrada);
            if (error) {
                errores.put("dataentrada", "Data d'entrada no es lògica");
            }
            Date fechaHoy = new Date();
            fechaTest = dateF.parse(dataentrada);
            if (fechaTest.after(fechaHoy)) {
                errores.put("dataentrada", "Data d'entrada posterior a la del dia");
            }
            if (hora == null) {
                errores.put("hora", "Hora d'entrada no es logica");
            } else {
                try {
                    horaF.setLenient(false);
                    horaTest = horaF.parse(hora);
                } catch (ParseException ex) {
                    errores.put("hora", "Hora d'entrada no es lògica");
                    errores.put("hora", hora);
                }
            }
            if (!oficina.equals("")) {
                try {
                    String sentenciaSql = "SELECT * FROM BZAUTOR WHERE FZHCUSU=? AND FZHCAUT=? AND FZHCAGCO IN " + "(SELECT FAACAGCO FROM BAGECOM WHERE FAAFBAJA=0 AND FAACAGCO=?)";
                    ps = conn.prepareStatement(sentenciaSql);
                    ps.setString(1, usuario.toUpperCase());
                    ps.setString(2, "AE");
                    ps.setInt(3, Integer.parseInt(oficina));
                    rs = ps.executeQuery();
                    if (rs.next()) {
                    } else {
                        errores.put("oficina", "Oficina: " + oficina + " no vàlida per a l'usuari: " + usuario);
                    }
                } catch (Exception e) {
                    System.out.println(usuario + ": Error en validar l'oficina " + e.getMessage());
                    e.printStackTrace();
                    errores.put("oficina", "Error en validar l'oficina: " + oficina + " de l'usuari: " + usuario + ": " + e.getClass() + "->" + e.getMessage());
                } finally {
                    ToolsBD.closeConn(null, ps, rs);
                }
            } else {
                errores.put("oficina", "Oficina: " + oficina + " no vàlida per a l'usuari: " + usuario);
            }
            if (data == null) {
                data = dataentrada;
            }
            validarFecha(data);
            if (error) {
                errores.put("data", "Data document, no es lògica");
            }
            try {
                String sentenciaSql = "SELECT * FROM BZTDOCU WHERE FZICTIPE=? AND FZIFBAJA=0";
                ps = conn.prepareStatement(sentenciaSql);
                ps.setString(1, tipo);
                rs = ps.executeQuery();
                if (rs.next()) {
                } else {
                    errores.put("tipo", "Tipus de document : " + tipo + " no vàlid");
                }
            } catch (Exception e) {
                System.out.println(usuario + ": Error en validar el tipus de document" + e.getMessage());
                e.printStackTrace();
                errores.put("tipo", "Error en validar el tipus de document : " + tipo + ": " + e.getClass() + "->" + e.getMessage());
            } finally {
                ToolsBD.closeConn(null, ps, rs);
            }
            try {
                String sentenciaSql = "SELECT * FROM BZIDIOM WHERE FZMCIDI=?";
                ps = conn.prepareStatement(sentenciaSql);
                ps.setString(1, idioma);
                rs = ps.executeQuery();
                if (rs.next()) {
                } else {
                    errores.put("idioma", "Idioma del document : " + idioma + " no vàlid");
                }
            } catch (Exception e) {
                System.out.println(usuario + ": Error en validar l'idioma del document" + e.getMessage());
                e.printStackTrace();
                errores.put("idioma", "Error en validar l'idioma del document: " + idioma + ": " + e.getClass() + "->" + e.getMessage());
            } finally {
                ToolsBD.closeConn(null, ps, rs);
            }
            if (entidad1.trim().equals("") && altres.trim().equals("")) {
                errores.put("entidad1", "És obligatori introduir el remitent");
            } else if (!entidad1.trim().equals("") && !altres.trim().equals("")) {
                errores.put("entidad1", "Heu d'introduir: Entitat o Altres");
            } else if (!entidad1.equals("")) {
                if (entidad2.equals("")) {
                    entidad2 = "0";
                }
                try {
                    String sentenciaSql = "SELECT * FROM BZENTID WHERE FZGCENT2=? AND FZGNENTI=? AND FZGFBAJA=0";
                    ps = conn.prepareStatement(sentenciaSql);
                    ps.setString(1, entidad1);
                    ps.setInt(2, Integer.parseInt(entidad2));
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        entidadCastellano = rs.getString("FZGCENTI");
                    } else {
                        errores.put("entidad1", "Entitat Remitent : " + entidad1 + "-" + entidad2 + " no vàlid");
                        System.out.println(usuario + ": ERROR: en validar l'entitat remitent : " + entidad1 + "-" + entidad2 + " no vàlid");
                    }
                } catch (Exception e) {
                    System.out.println(usuario + ": Error en validar l'entitat remitent " + e.getMessage());
                    e.printStackTrace();
                    errores.put("entidad1", "Error en validar l'entitat remitent: " + entidad1 + "-" + entidad2 + ": " + e.getClass() + "->" + e.getMessage());
                } finally {
                    ToolsBD.closeConn(null, ps, rs);
                }
            }
            if (!oficina.equals("32") && !correo.trim().equals("")) {
                errores.put("correo", "El valor nombre de correu només es pot introduir per l'Oficina 32 (BOIB)");
                System.out.println(usuario + ": ERROR: El valor nombre de correu només es pot introduir per l'Oficina 32 (BOIB)");
            }
            if ((balears.equals("") || balears == null) && (fora.equals("") || fora == null)) {
                errores.put("balears", "Obligatori introduir Procedència Geogràfica");
            } else if (!balears.equals("") && !fora.equals("")) {
                errores.put("balears", "Heu d'introduir Balears o Fora de Balears");
            } else if (!balears.equals("")) {
                try {
                    String sentenciaSql = "SELECT * FROM BAGRUGE WHERE FABCTAGG=90 AND FABCAGGE=? AND FABFBAJA=0";
                    ps = conn.prepareStatement(sentenciaSql);
                    ps.setInt(1, Integer.parseInt(balears));
                    rs = ps.executeQuery();
                    if (rs.next()) {
                    } else {
                        errores.put("balears", "Procedència geogràfica de Balears : " + balears + " no vàlid");
                        System.out.println(usuario + ": ERROR: Procedència geogràfica de Balears : " + balears + " no vàlid");
                    }
                } catch (Exception e) {
                    System.out.println(usuario + ": Error en validar la procedència geogràfica de Balears " + e.getMessage());
                    e.printStackTrace();
                    errores.put("balears", "Error en validar la procedència geogràfica de Balears : " + balears + ": " + e.getClass() + "->" + e.getMessage());
                } finally {
                    ToolsBD.closeConn(null, ps, rs);
                }
            }
            if (salida1.equals("") && salida2.equals("")) {
            } else {
                int chk1 = 0;
                int chk2 = 0;
                try {
                    chk1 = Integer.parseInt(salida1);
                    chk2 = Integer.parseInt(salida2);
                } catch (Exception e) {
                    errores.put("salida1", "Ambdós camps de numero de sortida han de ser numèrics");
                    System.out.println(usuario + ": ERROR: Ambdós camps de numero de sortida han de ser numèrics");
                }
                if (chk2 < 1990 || chk2 > 2050) {
                    errores.put("salida1", "Any de sortida, incorrecte");
                }
            }
            try {
                String sentenciaSql = "SELECT * FROM BZOFIOR WHERE FZFCAGCO=? AND FZFCORGA=?";
                ps = conn.prepareStatement(sentenciaSql);
                ps.setInt(1, Integer.parseInt(oficina));
                ps.setInt(2, Integer.parseInt(destinatari));
                rs = ps.executeQuery();
                if (rs.next()) {
                } else {
                    errores.put("destinatari", "Organisme destinatari : " + destinatari + " no vàlid");
                }
            } catch (NumberFormatException e1) {
                errores.put("destinatari", "Organisme destinatari : " + destinatari + " codi no numèric");
            } catch (Exception e) {
                System.out.println(usuario + ": Error en validar l'organisme destinatari " + e.getMessage());
                e.printStackTrace();
                errores.put("destinatari", "Error en validar l'organisme destinatari : " + destinatari + ": " + e.getClass() + "->" + e.getMessage());
            } finally {
                ToolsBD.closeConn(null, ps, rs);
            }
            if (!idioex.equals("1") && !idioex.equals("2")) {
                errores.put("idioex", "L'idioma ha de ser 1 o 2, idioma=" + idioex);
            }
            try {
                if (!disquet.equals("")) {
                    int chk1 = Integer.parseInt(disquet);
                }
            } catch (Exception e) {
                errores.put("disquet", "Numero de disquet no vàlid");
            }
            if (comentario.equals("")) {
                errores.put("comentario", "Heu d'introduir un extracte del document ");
            }
            if (actualizacion && !motivo.trim().equals("")) {
                if (comentarioNuevo.equals("")) {
                    errores.put("comentario", "Heu d'introduir un extracte del document ");
                }
                if (entidad1Nuevo.equals("") && altresNuevo.equals("")) {
                    errores.put("entidad1", "Obligatori introduir remitent");
                } else if (!entidad1Nuevo.equals("") && !altresNuevo.equals("")) {
                    errores.put("entidad1", "Heu d'introduir: Entitat o Altres");
                } else if (!entidad1Nuevo.equals("")) {
                    if (entidad2Nuevo.equals("")) {
                        entidad2Nuevo = "0";
                    }
                    try {
                        String sentenciaSql = "SELECT * FROM BZENTID WHERE FZGCENT2=? AND FZGNENTI=? AND FZGFBAJA=0";
                        ps = conn.prepareStatement(sentenciaSql);
                        ps.setString(1, entidad1Nuevo);
                        ps.setInt(2, Integer.parseInt(entidad2Nuevo));
                        rs = ps.executeQuery();
                        if (rs.next()) {
                        } else {
                            errores.put("entidad1", "Entitat Remitent : " + entidad1 + "-" + entidad2 + " no vàlid");
                            System.out.println("Error en validar l'entitat Remitent " + entidad1 + "-" + entidad2 + " no vàlid");
                        }
                    } catch (Exception e) {
                        System.out.println(usuario + ": Error en validar l'entitat Remitent " + e.getMessage());
                        e.printStackTrace();
                        errores.put("entidad1", "Error en validar l'entitat Remitent : " + entidad1 + "-" + entidad2 + ": " + e.getClass() + "->" + e.getMessage());
                    } finally {
                        ToolsBD.closeConn(null, ps, rs);
                    }
                }
            }
            if (!oficina.equals("") && !actualizacion) {
                try {
                    String sentenciaSql = "SELECT MAX(FZAFENTR) FROM BZENTRA WHERE FZAANOEN=? AND FZACAGCO=?";
                    fechaTest = dateF.parse(dataentrada);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(fechaTest);
                    DateFormat date1 = new SimpleDateFormat("yyyyMMdd");
                    ps = conn.prepareStatement(sentenciaSql);
                    ps.setInt(1, cal.get(Calendar.YEAR));
                    ps.setInt(2, Integer.parseInt(oficina));
                    rs = ps.executeQuery();
                    int ultimaFecha = 0;
                    if (rs.next()) {
                        ultimaFecha = rs.getInt(1);
                        if (ultimaFecha > Integer.parseInt(date1.format(fechaTest))) {
                            errores.put("dataentrada", "Data inferior a la darrera entrada");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errores.put("dataentrada", "Error inesperat a data d'entrada");
                    System.out.println(usuario + ": Error inesperat a la data d'entrada" + ": " + e.getClass() + "->" + e.getMessage());
                } finally {
                    ToolsBD.closeConn(null, ps, rs);
                }
            }
        } catch (Exception e) {
            validado = false;
        } finally {
            try {
                ToolsBD.closeConn(conn, null, null);
            } catch (Exception e) {
                System.out.println("Excepció en tancar la connexió: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (errores.size() == 0) {
            validado = true;
        } else {
            validado = false;
        }
        return validado;
    }

    /**
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    public void grabar() throws SQLException, ClassNotFoundException, Exception {
        Connection conn = null;
        try {
            conn = ToolsBD.getConn();
            conn.setAutoCommit(false);
            cargar(conn);
            conn.commit();
        } catch (Exception ex) {
            System.out.println(usuario + ": Excepció: " + ex.getMessage());
            ex.printStackTrace();
            registroGrabado = false;
            errores.put("", "Error inesperat, no s'ha desat el registre" + ": " + ex.getClass() + "->" + ex.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException sqle) {
                throw new RemoteException(usuario + ": S'ha produït un error i no s'han pogut tornar enrere els canvis efectuats", sqle);
            }
            throw new RemoteException("Error inesperat: No s'ha desat el registre", ex);
        } finally {
            ToolsBD.closeConn(conn, null, null);
        }
    }

    /**
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    private void cargarMunicipio060(Connection conn, int fzaanoe, int fzanume, int fzacagc, String codimun, int numreg_060) throws SQLException, ClassNotFoundException, Exception {
        PreparedStatement ms = null;
        try {
            String insertBZNCORR = "insert into BZENTRA060 (ENT_ANY, ENT_OFI, ENT_NUM, ENT_CODIMUN,ENT_NUMREG)" + "values (?,?,?,?,?)";
            ms = conn.prepareStatement(insertBZNCORR);
            ms.setInt(1, fzaanoe);
            ms.setInt(2, fzacagc);
            ms.setInt(3, fzanume);
            ms.setString(4, codimun);
            ms.setInt(5, numreg_060);
            ms.execute();
        } catch (Exception e) {
            throw e;
        } finally {
            ms.close();
        }
    }

    /**
      * @throws SQLException
      * @throws ClassNotFoundException
      * @throws Exception
      */
    private void cargar(Connection conn) throws SQLException, ClassNotFoundException, Exception {
        PreparedStatement ms = null;
        if (!validado) {
            validado = validar();
        }
        if (!validado) {
            throw new Exception("No s'ha realitzat la validació de les dades del registre ");
        }
        registroGrabado = false;
        int fzaanoe;
        String campo;
        fechaTest = dateF.parse(dataentrada);
        Calendar cal = Calendar.getInstance();
        cal.setTime(fechaTest);
        DateFormat date1 = new SimpleDateFormat("yyyyMMdd");
        fzaanoe = cal.get(Calendar.YEAR);
        anoEntrada = String.valueOf(fzaanoe);
        int fzafent = Integer.parseInt(date1.format(fechaTest));
        int fzanume = ToolsBD.RecogerNumeroEntrada(conn, fzaanoe, oficina, errores);
        setNumeroEntrada(Integer.toString(fzanume));
        int fzacagc = Integer.parseInt(oficina);
        int off_codi = Integer.parseInt(oficinafisica);
        fechaTest = dateF.parse(data);
        cal.setTime(fechaTest);
        int fzafdoc = Integer.parseInt(date1.format(fechaTest));
        String fzacone, fzacone2;
        if (idioex.equals("1")) {
            fzacone = comentario;
            fzacone2 = "";
        } else {
            fzacone = "";
            fzacone2 = comentario;
        }
        String fzaproce;
        int fzactagg, fzacagge;
        if (fora.equals("")) {
            fzactagg = 90;
            fzacagge = Integer.parseInt(balears);
            fzaproce = "";
        } else {
            fzaproce = fora;
            fzactagg = 0;
            fzacagge = 0;
        }
        int ceros = 0;
        int fzacorg = Integer.parseInt(destinatari);
        int fzanent;
        String fzacent;
        if (altres.equals("")) {
            altres = "";
            fzanent = Integer.parseInt(entidad2);
            fzacent = entidadCastellano;
        } else {
            fzanent = 0;
            fzacent = "";
        }
        int fzacidi = Integer.parseInt(idioex);
        horaTest = horaF.parse(hora);
        cal.setTime(horaTest);
        DateFormat hhmm = new SimpleDateFormat("HHmm");
        int fzahora = Integer.parseInt(hhmm.format(horaTest));
        if (salida1.equals("")) {
            salida1 = "0";
        }
        if (salida2.equals("")) {
            salida2 = "0";
        }
        int fzanloc = Integer.parseInt(salida1);
        int fzaaloc = Integer.parseInt(salida2);
        if (disquet.equals("")) {
            disquet = "0";
        }
        int fzandis = Integer.parseInt(disquet);
        if (fzandis > 0) {
            ToolsBD.actualizaDisqueteEntrada(conn, fzandis, oficina, anoEntrada, errores);
        }
        Date fechaSystem = new Date();
        DateFormat aaaammdd = new SimpleDateFormat("yyyyMMdd");
        int fzafsis = Integer.parseInt(aaaammdd.format(fechaSystem));
        DateFormat hhmmss = new SimpleDateFormat("HHmmss");
        DateFormat sss = new SimpleDateFormat("S");
        String ss = sss.format(fechaSystem);
        if (ss.length() > 2) {
            ss = ss.substring(0, 2);
        }
        int fzahsis = Integer.parseInt(hhmmss.format(fechaSystem) + ss);
        if (correo != null && !correo.equals("")) {
            String insertBZNCORR = "INSERT INTO BZNCORR (FZPCENSA, FZPCAGCO, FZPANOEN, FZPNUMEN, FZPNCORR)" + "VALUES (?,?,?,?,?)";
            ms = conn.prepareStatement(insertBZNCORR);
            ms.setString(1, "E");
            ms.setInt(2, fzacagc);
            ms.setInt(3, fzaanoe);
            ms.setInt(4, fzanume);
            ms.setString(5, correo);
            ms.execute();
            ms.close();
        }
        String insertOfifis = "INSERT INTO BZENTOFF (FOEANOEN, FOENUMEN, FOECAGCO, OFE_CODI)" + "VALUES (?,?,?,?)";
        ms = conn.prepareStatement(insertOfifis);
        ms.setInt(1, fzaanoe);
        ms.setInt(2, fzanume);
        ms.setInt(3, fzacagc);
        ms.setInt(4, off_codi);
        ms.execute();
        ms.close();
        ms = conn.prepareStatement(SENTENCIA);
        ms.setInt(1, fzaanoe);
        ms.setInt(2, fzanume);
        ms.setInt(3, fzacagc);
        ms.setInt(4, fzafdoc);
        ms.setString(5, (altres.length() > 30) ? altres.substring(0, 30) : altres);
        ms.setString(6, (fzacone.length() > 160) ? fzacone.substring(0, 160) : fzacone);
        ms.setString(7, (tipo.length() > 2) ? tipo.substring(0, 2) : tipo);
        ms.setString(8, "N");
        ms.setString(9, "");
        ms.setString(10, (fzaproce.length() > 25) ? fzaproce.substring(0, 25) : fzaproce);
        ms.setInt(11, fzafent);
        ms.setInt(12, fzactagg);
        ms.setInt(13, fzacagge);
        ms.setInt(14, fzacorg);
        ms.setInt(15, ceros);
        ms.setString(16, (fzacent.length() > 7) ? fzacent.substring(0, 7) : fzacent);
        ms.setInt(17, fzanent);
        ms.setInt(18, fzahora);
        ms.setInt(19, fzacidi);
        ms.setString(20, (fzacone2.length() > 160) ? fzacone2.substring(0, 160) : fzacone2);
        ms.setInt(21, fzanloc);
        ms.setInt(22, fzaaloc);
        ms.setInt(23, fzandis);
        ms.setInt(24, fzafsis);
        ms.setInt(25, fzahsis);
        ms.setString(26, (usuario.toUpperCase().length() > 10) ? usuario.toUpperCase().substring(0, 10) : usuario.toUpperCase());
        ms.setString(27, idioma);
        registroGrabado = ms.execute();
        registroGrabado = true;
        if (!municipi060.equals("")) cargarMunicipio060(conn, fzaanoe, fzanume, fzacagc, municipi060, numeroRegistros060);
        String remitente = "";
        if (!altres.trim().equals("")) {
            remitente = altres;
        } else {
            javax.naming.InitialContext contexto = new javax.naming.InitialContext();
            Object ref = contexto.lookup("es.caib.regweb.ValoresHome");
            ValoresHome home = (ValoresHome) javax.rmi.PortableRemoteObject.narrow(ref, ValoresHome.class);
            Valores valor = home.create();
            remitente = valor.recuperaRemitenteCastellano(fzacent, fzanent + "");
            valor.remove();
        }
        try {
            Class t = Class.forName("es.caib.regweb.module.PluginHook");
            Class[] partypes = { String.class, Integer.class, Integer.class, Integer.class, Integer.class, String.class, String.class, String.class, Integer.class, Integer.class, String.class, Integer.class, String.class, String.class, Integer.class, Integer.class, Integer.class, String.class, String.class, String.class };
            Object[] params = { "A", new Integer(fzaanoe), new Integer(fzanume), new Integer(fzacagc), new Integer(fzafdoc), remitente, comentario, tipo, new Integer(fzafent), new Integer(fzacagge), fzaproce, new Integer(fzacorg), idioma, null, null, null, null, null, null, null };
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
        logLopdBZENTRA("INSERT", (usuario.length() > 10) ? usuario.substring(0, 10) : usuario, fzafsis, horamili, fzanume, fzaanoe, fzacagc);
        ms.close();
    }

    /**
     * Actualitza el registre d'entrada 060
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    private void actualizar060() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        int actualizados = 0;
        try {
            conn = ToolsBD.getConn();
            String sentenciaSql = "Update bzentra060 set ent_codimun=?, ent_numreg=?  where ent_any=? and ent_ofi=? and ent_num=?";
            ps = conn.prepareStatement(sentenciaSql);
            ps.setString(1, municipi060);
            ps.setInt(2, numeroRegistros060);
            ps.setString(3, anoEntrada);
            ps.setString(4, oficina);
            ps.setString(5, numeroEntrada);
            actualizados = ps.executeUpdate();
            if (actualizados != 0) {
            } else {
                cargarMunicipio060(conn, Integer.parseInt(anoEntrada), Integer.parseInt(numeroEntrada), Integer.parseInt(oficina), municipi060, numeroRegistros060);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ToolsBD.closeConn(conn, ps, null);
        }
    }

    /**
     * Elimina el registre d'entrada 060
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    private void eliminar060() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        int actualizados = 0;
        try {
            conn = ToolsBD.getConn();
            String sentenciaSql = "DELETE FROM BZENTRA060  WHERE ENT_ANY=? AND ENT_OFI=? AND ENT_NUM=?";
            ps = conn.prepareStatement(sentenciaSql);
            ps.setString(1, anoEntrada);
            ps.setString(2, oficina);
            ps.setString(3, numeroEntrada);
            actualizados = ps.executeUpdate();
            if (actualizados != 0) {
                System.out.println("Municipi 060 eliminat:" + municipi060);
            } else {
                System.out.println("Res per eliminar.");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ToolsBD.closeConn(conn, ps, null);
        }
    }

    /**
     * Actualitza el registre d'entrada
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    public void actualizar() throws SQLException, ClassNotFoundException, Exception {
        Connection conn = null;
        PreparedStatement ms = null;
        if (!validado) {
            validado = validar();
        }
        if (!validado) {
            throw new Exception("No s'ha realitzat la validació de les dades del registre ");
        }
        registroActualizado = false;
        try {
            int fzaanoe;
            String campo;
            fechaTest = dateF.parse(dataentrada);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaTest);
            DateFormat date1 = new SimpleDateFormat("yyyyMMdd");
            fzaanoe = Integer.parseInt(anoEntrada);
            int fzafent = Integer.parseInt(date1.format(fechaTest));
            conn = ToolsBD.getConn();
            conn.setAutoCommit(false);
            int fzanume = Integer.parseInt(numeroEntrada);
            int fzacagc = Integer.parseInt(oficina);
            int off_codi = 0;
            try {
                off_codi = Integer.parseInt(oficinafisica);
            } catch (Exception e) {
            }
            fechaTest = dateF.parse(data);
            cal.setTime(fechaTest);
            int fzafdoc = Integer.parseInt(date1.format(fechaTest));
            String fzacone, fzacone2;
            if (idioex.equals("1")) {
                fzacone = comentario;
                fzacone2 = "";
            } else {
                fzacone = "";
                fzacone2 = comentario;
            }
            String fzaproce;
            int fzactagg, fzacagge;
            if (fora.equals("")) {
                fzactagg = 90;
                fzacagge = Integer.parseInt(balears);
                fzaproce = "";
            } else {
                fzaproce = fora;
                fzactagg = 0;
                fzacagge = 0;
            }
            int ceros = 0;
            int fzacorg = Integer.parseInt(destinatari);
            int fzanent;
            String fzacent;
            if (altres.equals("")) {
                altres = "";
                fzanent = Integer.parseInt(entidad2);
                fzacent = entidadCastellano;
            } else {
                fzanent = 0;
                fzacent = "";
            }
            int fzacidi = Integer.parseInt(idioex);
            horaTest = horaF.parse(hora);
            cal.setTime(horaTest);
            DateFormat hhmm = new SimpleDateFormat("HHmm");
            int fzahora = Integer.parseInt(hhmm.format(horaTest));
            if (salida1.equals("")) {
                salida1 = "0";
            }
            if (salida2.equals("")) {
                salida2 = "0";
            }
            int fzanloc = Integer.parseInt(salida1);
            int fzaaloc = Integer.parseInt(salida2);
            if (disquet.equals("")) {
                disquet = "0";
            }
            int fzandis = Integer.parseInt(disquet);
            if (fzandis > 0) {
                ToolsBD.actualizaDisqueteEntrada(conn, fzandis, oficina, anoEntrada, errores);
            }
            Date fechaSystem = new Date();
            DateFormat aaaammdd = new SimpleDateFormat("yyyyMMdd");
            int fzafsis = Integer.parseInt(aaaammdd.format(fechaSystem));
            DateFormat hhmmss = new SimpleDateFormat("HHmmss");
            DateFormat sss = new SimpleDateFormat("S");
            String ss = sss.format(fechaSystem);
            if (ss.length() > 2) {
                ss = ss.substring(0, 2);
            }
            int fzahsis = Integer.parseInt(hhmmss.format(fechaSystem) + ss);
            String deleteOfifis = "DELETE FROM BZENTOFF WHERE FOEANOEN=? AND FOENUMEN=? AND FOECAGCO=?";
            ms = conn.prepareStatement(deleteOfifis);
            ms.setInt(1, fzaanoe);
            ms.setInt(2, fzanume);
            ms.setInt(3, fzacagc);
            ms.execute();
            ms.close();
            String insertOfifis = "INSERT INTO BZENTOFF (FOEANOEN, FOENUMEN, FOECAGCO, OFE_CODI)" + "VALUES (?,?,?,?)";
            ms = conn.prepareStatement(insertOfifis);
            ms.setInt(1, fzaanoe);
            ms.setInt(2, fzanume);
            ms.setInt(3, fzacagc);
            ms.setInt(4, off_codi);
            ms.execute();
            ms.close();
            ms = conn.prepareStatement("UPDATE BZENTRA SET FZAFDOCU=?, FZAREMIT=?, FZACONEN=?, FZACTIPE=?, " + "FZACEDIE=?, FZAENULA=?, FZAPROCE=?, FZAFENTR=?, FZACTAGG=?, FZACAGGE=?, FZACORGA=?, " + "FZACENTI=?, FZANENTI=?, FZAHORA=?, FZACIDIO=?, FZACONE2=?, FZANLOC=?, FZAALOC=?, FZANDIS=?, " + "FZACUSU=?, FZACIDI=? WHERE FZAANOEN=? AND FZANUMEN=? AND FZACAGCO=?");
            ms.setInt(1, fzafdoc);
            ms.setString(2, (altres.length() > 30) ? altres.substring(0, 30) : altres);
            ms.setString(3, (fzacone.length() > 160) ? fzacone.substring(0, 160) : fzacone);
            ms.setString(4, (tipo.length() > 2) ? tipo.substring(0, 1) : tipo);
            ms.setString(5, "N");
            ms.setString(6, (registroAnulado.length() > 1) ? registroAnulado.substring(0, 1) : registroAnulado);
            ms.setString(7, (fzaproce.length() > 25) ? fzaproce.substring(0, 25) : fzaproce);
            ms.setInt(8, fzafent);
            ms.setInt(9, fzactagg);
            ms.setInt(10, fzacagge);
            ms.setInt(11, fzacorg);
            ms.setString(12, (fzacent.length() > 7) ? fzacent.substring(0, 8) : fzacent);
            ms.setInt(13, fzanent);
            ms.setInt(14, fzahora);
            ms.setInt(15, fzacidi);
            ms.setString(16, (fzacone2.length() > 160) ? fzacone2.substring(0, 160) : fzacone2);
            ms.setInt(17, fzanloc);
            ms.setInt(18, fzaaloc);
            ms.setInt(19, fzandis);
            ms.setString(20, (usuario.toUpperCase().length() > 10) ? usuario.toUpperCase().substring(0, 10) : usuario.toUpperCase());
            ms.setString(21, idioma);
            ms.setInt(22, fzaanoe);
            ms.setInt(23, fzanume);
            ms.setInt(24, fzacagc);
            boolean modificado = false;
            if (!motivo.equals("")) {
                javax.naming.InitialContext contexto = new javax.naming.InitialContext();
                Object ref = contexto.lookup("es.caib.regweb.RegistroModificadoEntradaHome");
                RegistroModificadoEntradaHome home = (RegistroModificadoEntradaHome) javax.rmi.PortableRemoteObject.narrow(ref, RegistroModificadoEntradaHome.class);
                RegistroModificadoEntrada registroModificado = home.create();
                registroModificado.setAnoEntrada(fzaanoe);
                registroModificado.setOficina(fzacagc);
                if (!entidad1Nuevo.trim().equals("")) {
                    if (entidad2Nuevo.equals("")) {
                        entidad2Nuevo = "0";
                    }
                }
                int fzanentNuevo;
                String fzacentNuevo;
                if (altresNuevo.trim().equals("")) {
                    altresNuevo = "";
                    fzanentNuevo = Integer.parseInt(entidad2Nuevo);
                    fzacentNuevo = convierteEntidadCastellano(entidad1Nuevo, conn);
                } else {
                    fzanentNuevo = 0;
                    fzacentNuevo = "";
                }
                if (!fzacentNuevo.equals(fzacent) || fzanentNuevo != fzanent) {
                    registroModificado.setEntidad2(fzanentNuevo);
                    registroModificado.setEntidad1(fzacentNuevo);
                } else {
                    registroModificado.setEntidad2(0);
                    registroModificado.setEntidad1("");
                }
                if (!comentarioNuevo.trim().equals(comentario.trim())) {
                    registroModificado.setExtracto(comentarioNuevo);
                } else {
                    registroModificado.setExtracto("");
                }
                registroModificado.setUsuarioModificacion(usuario.toUpperCase());
                registroModificado.setNumeroRegistro(fzanume);
                if (altresNuevo.equals(altres)) {
                    registroModificado.setRemitente("");
                } else {
                    registroModificado.setRemitente(altresNuevo);
                }
                registroModificado.setMotivo(motivo);
                modificado = registroModificado.generarModificacion(conn);
                registroModificado.remove();
            }
            if ((modificado && !motivo.equals("")) || motivo.equals("")) {
                int afectados = ms.executeUpdate();
                if (afectados > 0) {
                    registroActualizado = true;
                } else {
                    registroActualizado = false;
                }
                String remitente = "";
                if (!altres.trim().equals("")) {
                    remitente = altres;
                } else {
                    javax.naming.InitialContext contexto = new javax.naming.InitialContext();
                    Object ref = contexto.lookup("es.caib.regweb.ValoresHome");
                    ValoresHome home = (ValoresHome) javax.rmi.PortableRemoteObject.narrow(ref, ValoresHome.class);
                    Valores valor = home.create();
                    remitente = valor.recuperaRemitenteCastellano(fzacent, fzanent + "");
                    valor.remove();
                }
                try {
                    Class t = Class.forName("es.caib.regweb.module.PluginHook");
                    Class[] partypes = { String.class, Integer.class, Integer.class, Integer.class, Integer.class, String.class, String.class, String.class, Integer.class, Integer.class, String.class, Integer.class, String.class, String.class, Integer.class, Integer.class, Integer.class, String.class, String.class, String.class };
                    Object[] params = { "M", new Integer(fzaanoe), new Integer(fzanume), new Integer(fzacagc), new Integer(fzafdoc), remitente, comentario, tipo, new Integer(fzafent), new Integer(fzacagge), fzaproce, new Integer(fzacorg), idioma, BOIBdata, new Integer(BOIBnumeroBOCAIB), new Integer(BOIBpagina), new Integer(BOIBlineas), BOIBtexto, BOIBobservaciones, correo };
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
                conn.commit();
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
                logLopdBZENTRA("UPDATE", (usuario.toUpperCase().length() > 10) ? usuario.toUpperCase().substring(0, 10) : usuario.toUpperCase(), fzafsis, horamili, fzanume, fzaanoe, fzacagc);
            } else {
                registroActualizado = false;
                errores.put("", "Error inesperat, no s'ha modificat el registre");
                throw new RemoteException("Error inesperat, no s'ha modifcat el registre");
            }
            System.out.println("Municipi codi: " + municipi060);
            if (municipi060.equals("000")) eliminar060(); else if (!municipi060.equals("")) actualizar060();
        } catch (Exception ex) {
            System.out.println("Error inesperat, no s'ha desat el registre: " + ex.getMessage());
            ex.printStackTrace();
            registroActualizado = false;
            errores.put("", "Error inesperat, no s'ha desat el registre" + ": " + ex.getClass() + "->" + ex.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException sqle) {
                throw new RemoteException("S'ha produït un error i no s'han pogut tornar enrere els canvis efectuats", sqle);
            }
            throw new RemoteException("Error inesperat, no s'ha modifcat el registre", ex);
        } finally {
            ToolsBD.closeConn(conn, ms, null);
        }
    }

    /**
     * Valida la data donada
     * @param fecha
     */
    private void validarFecha(String fecha) {
        try {
            dateF.setLenient(false);
            fechaTest = dateF.parse(fecha);
            error = false;
        } catch (Exception ex) {
            System.out.println("Error validant la data:" + ex.getMessage());
            ex.printStackTrace();
            error = true;
        }
    }

    /** 
     * Lee un registro del fichero BZENTRA060 (si existe), para ello le
     * deberemos pasar el codigo de oficina, el numero de registro de
     * entrada y el año de entrada.
     * @param oficina String
     * @param numeroEntrada String
     * @param anoEntrada String
     * @return void
     */
    private void leer060() throws Exception {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = ToolsBD.getConn();
            String sentenciaSql = "Select ent_codimun, mun_nom, ent_numreg from bzentra060,bzmun_060 where ent_any=? and ent_ofi=? and ent_num=? and ent_codimun=mun_codi";
            ps = conn.prepareStatement(sentenciaSql);
            ps.setString(1, anoEntrada);
            ps.setString(2, oficina);
            ps.setString(3, numeroEntrada);
            rs = ps.executeQuery();
            if (rs.next()) {
                municipi060 = rs.getString("ent_codimun");
                descripcioMunicipi060 = rs.getString("mun_nom");
                numeroRegistros060 = rs.getInt("ent_numreg");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ToolsBD.closeConn(conn, ps, rs);
        }
    }

    /** 
     * Lee un registro del fichero BZENTRA, para ello le
     * deberemos pasar el usuario, el codigo de oficina, el numero de registro de
     * entrada y el año de entrada.
     * @param usuario String
     * @param oficina String
     * @param numeroEntrada String
     * @param anoEntrada String
     * @return void
     */
    public void leer() {
        Connection conn = null;
        ResultSet rs = null;
        ResultSet rsHist = null;
        PreparedStatement ps = null;
        PreparedStatement psHist = null;
        DateFormat yyyymmdd = new SimpleDateFormat("yyyyMMdd");
        DateFormat ddmmyyyy = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date fechaDocumento = null;
        leidos = false;
        try {
            conn = ToolsBD.getConn();
            String sentenciaSql = "SELECT * FROM BZENTRA LEFT JOIN BAGECOM ON FAACAGCO=FZACAGCO " + "LEFT JOIN BZENTID ON FZACENTI=FZGCENTI AND FZGNENTI=FZANENTI " + "LEFT JOIN BORGANI ON FAXCORGA=FZACORGA " + "LEFT JOIN BZTDOCU ON FZICTIPE=FZACTIPE " + "LEFT JOIN BZIDIOM ON FZACIDI=FZMCIDI " + "LEFT JOIN BZENTOFF ON FZAANOEN=FOEANOEN AND FZANUMEN=FOENUMEN AND FZACAGCO=FOECAGCO " + "LEFT JOIN BZOFIFIS ON FZACAGCO=FZOCAGCO AND OFF_CODI=OFE_CODI " + "LEFT JOIN BAGRUGE ON FZACTAGG=FABCTAGG AND FZACAGGE=FABCAGGE " + "LEFT JOIN BZAUTOR ON FZHCUSU=? AND FZHCAGCO=FZACAGCO " + "LEFT JOIN BZNCORR ON FZPCENSA='E' AND FZPCAGCO=FZACAGCO AND FZPANOEN=FZAANOEN AND FZPNUMEN=FZANUMEN " + "WHERE FZAANOEN=? AND FZANUMEN=? AND FZACAGCO=? AND FZHCAUT=?";
            ps = conn.prepareStatement(sentenciaSql);
            ps.setString(1, usuario.toUpperCase());
            ps.setString(2, anoEntrada);
            ps.setString(3, numeroEntrada);
            ps.setString(4, oficina);
            ps.setString(5, "CE");
            rs = ps.executeQuery();
            if (rs.next()) {
                Date fechaSystem = new Date();
                DateFormat hhmmss = new SimpleDateFormat("HHmmss");
                DateFormat sss = new SimpleDateFormat("S");
                String ss = sss.format(fechaSystem);
                DateFormat aaaammdd = new SimpleDateFormat("yyyyMMdd");
                int fzafsis = Integer.parseInt(aaaammdd.format(fechaSystem));
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
                logLopdBZENTRA("SELECT", usuario, fzafsis, horamili, Integer.parseInt(numeroEntrada), Integer.parseInt(anoEntrada), Integer.parseInt(oficina));
                leidos = true;
                anoEntrada = String.valueOf(rs.getInt("FZAANOEN"));
                numeroEntrada = String.valueOf(rs.getInt("FZANUMEN"));
                oficina = String.valueOf(rs.getInt("FZACAGCO"));
                oficinafisica = String.valueOf(rs.getInt("OFF_CODI"));
                String textoOficinaFisica = null;
                textoOficinaFisica = rs.getString("OFF_NOM");
                if (rs.getString("OFF_CODI") == null) {
                    oficinafisica = "0";
                    String sentenciaSqlOfiFis = "SELECT * FROM BZOFIFIS WHERE FZOCAGCO=? AND OFF_CODI=0 ";
                    psHist = conn.prepareStatement(sentenciaSqlOfiFis);
                    psHist.setString(1, oficina);
                    rsHist = psHist.executeQuery();
                    if (rsHist.next()) {
                        textoOficinaFisica = rsHist.getString("OFF_NOM");
                    }
                    if (rsHist != null) rsHist.close();
                    if (psHist != null) psHist.close();
                }
                if (textoOficinaFisica == null) {
                    textoOficinaFisica = " ";
                }
                descripcionOficinaFisica = textoOficinaFisica;
                String fechaEntra = String.valueOf(rs.getInt("FZAFENTR"));
                try {
                    fechaDocumento = yyyymmdd.parse(fechaEntra);
                    dataentrada = (ddmmyyyy.format(fechaDocumento));
                } catch (Exception e) {
                    dataentrada = fechaEntra;
                }
                String textoOficina = null;
                String sentenciaSqlHistOfi = "SELECT * FROM BHAGECO01 WHERE FHACAGCO=? AND FHAFALTA<=? " + "AND ( (FHAFBAJA>= ? AND FHAFBAJA !=0) OR FHAFBAJA = 0)";
                psHist = conn.prepareStatement(sentenciaSqlHistOfi);
                psHist.setString(1, oficina);
                psHist.setString(2, fechaEntra);
                psHist.setString(3, fechaEntra);
                rsHist = psHist.executeQuery();
                if (rsHist.next()) {
                    textoOficina = rsHist.getString("FHADAGCO");
                } else {
                    textoOficina = rs.getString("FAADAGCO");
                    if (textoOficina == null) {
                        textoOficina = " ";
                    }
                }
                if (rsHist != null) rsHist.close();
                if (psHist != null) psHist.close();
                descripcionOficina = textoOficina;
                String fechaDocu = String.valueOf(rs.getInt("FZAFDOCU"));
                try {
                    fechaDocumento = yyyymmdd.parse(fechaDocu);
                    data = (ddmmyyyy.format(fechaDocumento));
                } catch (Exception e) {
                    data = fechaDocu;
                }
                String fechaVisado = String.valueOf(rs.getInt("FZAFACTU"));
                try {
                    fechaDocumento = yyyymmdd.parse(fechaVisado);
                    dataVisado = (ddmmyyyy.format(fechaDocumento));
                } catch (Exception e) {
                    dataVisado = fechaVisado;
                }
                hora = String.valueOf(rs.getInt("FZAHORA"));
                if (rs.getString("FZGCENTI") == null) {
                    descripcionRemitente = rs.getString("FZAREMIT");
                } else {
                    descripcionRemitente = rs.getString("FZGDENT2");
                }
                destinatari = String.valueOf(rs.getInt("FZACORGA"));
                String sentenciaSqlHistOrga = "SELECT * FROM BHORGAN01 WHERE FHXCORGA=? AND FHXFALTA<=? " + "AND ( (FHXFBAJA>= ? AND FHXFBAJA !=0) OR FHXFBAJA = 0)";
                psHist = conn.prepareStatement(sentenciaSqlHistOrga);
                psHist.setString(1, destinatari);
                psHist.setString(2, fechaEntra);
                psHist.setString(3, fechaEntra);
                rsHist = psHist.executeQuery();
                if (rsHist.next()) {
                    descripcionOrganismoDestinatario = rsHist.getString("FHXDORGT");
                } else {
                    descripcionOrganismoDestinatario = (rs.getString("FAXDORGT"));
                    if (descripcionOrganismoDestinatario == null) {
                        descripcionOrganismoDestinatario = " ";
                    }
                }
                if (rsHist != null) rsHist.close();
                if (psHist != null) psHist.close();
                if (rs.getString("FZIDTIPE") == null) descripcionDocumento = ""; else descripcionDocumento = rs.getString("FZIDTIPE");
                if (rs.getString("FZACTIPE") == null) tipo = ""; else tipo = rs.getString("FZACTIPE");
                idioma = rs.getString("FZACIDI");
                idioex = rs.getString("FZACIDIO");
                registroAnulado = rs.getString("FZAENULA");
                descripcionIdiomaDocumento = rs.getString("FZMDIDI");
                entidad1 = ToolsBD.convierteEntidad(rs.getString("FZACENTI"), conn);
                entidad1Grabada = rs.getString("FZACENTI");
                entidad2 = rs.getString("FZANENTI");
                altres = rs.getString("FZAREMIT");
                balears = rs.getString("FZACAGGE");
                fora = rs.getString("FZAPROCE");
                if (rs.getString("FABDAGGE") == null) {
                    procedenciaGeografica = rs.getString("FZAPROCE");
                } else {
                    procedenciaGeografica = rs.getString("FABDAGGE");
                }
                if (rs.getString("FZACIDIO").equals("1")) {
                    idiomaExtracto = "CASTELLÀ";
                    comentario = rs.getString("FZACONEN");
                } else {
                    idiomaExtracto = "CATALÀ";
                    comentario = rs.getString("FZACONE2");
                }
                if (rs.getString("FZANDIS").equals("0")) {
                    disquet = "";
                } else {
                    disquet = rs.getString("FZANDIS");
                }
                salida1 = String.valueOf(rs.getInt("FZANLOC"));
                salida2 = String.valueOf(rs.getInt("FZAALOC"));
                correo = rs.getString("FZPNCORR");
            }
            leer060();
        } catch (Exception e) {
            System.out.println("ERROR: Leer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ToolsBD.closeConn(conn, ps, rs);
        }
    }

    private String convierteEntidadCastellano(String entidadCatalan, Connection conn) {
        String eCastellano = "";
        ResultSet rs = null;
        PreparedStatement ps1 = null;
        try {
            String sentenciaSql = "SELECT * FROM BZENTID WHERE FZGCENT2=? AND FZGNENTI=? AND FZGFBAJA=0";
            ps1 = conn.prepareStatement(sentenciaSql);
            ps1.setString(1, entidadCatalan);
            ps1.setInt(2, Integer.parseInt(entidad2Nuevo));
            rs = ps1.executeQuery();
            if (rs.next()) {
                eCastellano = rs.getString("FZGCENTI");
            } else {
                eCastellano = "";
            }
        } catch (Exception e) {
            eCastellano = "";
        } finally {
            ToolsBD.closeConn(null, ps1, rs);
        }
        return eCastellano;
    }

    /**
     * @return
     */
    public boolean getValidado() {
        return validado;
    }

    /**
     * @return
     */
    public Hashtable getErrores() {
        return errores;
    }

    /**
     * @return
     */
    public boolean getGrabado() {
        return registroGrabado;
    }

    /**
     * @return
     */
    public boolean getActualizado() {
        return registroActualizado;
    }

    /**
     * @return
     */
    public String getOficina() {
        return oficina;
    }

    /**
     * @return
     */
    public String getOficinafisica() {
        return oficinafisica;
    }

    /**
     * @return
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * @return
     */
    public String getMunicipi060() {
        return municipi060;
    }

    /**
     * @return
     */
    public String getIdioma() {
        return idioma;
    }

    /**
     * @return
     */
    public String getRegistroAnulado() {
        return registroAnulado;
    }

    /**
     * @return
     */
    public String getEntidad1() {
        return entidad1;
    }

    /**
     * @return
     */
    public String getEntidad2() {
        return entidad2;
    }

    /**
     * @return
     */
    public String getAltres() {
        return altres;
    }

    /**
     * @return
     */
    public String getBalears() {
        return balears;
    }

    /**
     * @return
     */
    public String getFora() {
        return fora;
    }

    /**
     * @return
     */
    public String getSalida1() {
        return salida1;
    }

    /**
     * @return
     */
    public String getSalida2() {
        return salida2;
    }

    /**
     * @return
     */
    public String getDestinatari() {
        return destinatari;
    }

    /**
     * @return
     */
    public String getIdioex() {
        return idioex;
    }

    /**
     * @return
     */
    public String getDisquet() {
        return disquet;
    }

    /**
     * @return
     */
    public String getComentario() {
        return comentario;
    }

    /**
     * @return
     */
    public String getHora() {
        return hora;
    }

    /**
     * @return
     */
    public String getDataEntrada() {
        return dataentrada;
    }

    /**
     * @return
     */
    public String getDataVisado() {
        return dataVisado;
    }

    /**
     * @return
     */
    public String getData() {
        return data;
    }

    /**
     * @return
     */
    public String getNumero() {
        return String.valueOf(fzanume);
    }

    /**
     * @return
     */
    public String getAnoEntrada() {
        return anoEntrada;
    }

    /**
     * @return
     */
    public String getNumeroEntrada() {
        return numeroEntrada;
    }

    /**
     * @return
     */
    public String getDescripcionOficina() {
        return descripcionOficina;
    }

    /**
     * @return
     */
    public String getDescripcionOficinaFisica() {
        return descripcionOficinaFisica;
    }

    /**
     * @return
     */
    public String getDescripcionRemitente() {
        return descripcionRemitente;
    }

    /**
     * @return
     */
    public String getDescripcionMunicipi060() {
        return descripcioMunicipi060;
    }

    /**
     * @return
     */
    public String getDescripcionOrganismoDestinatario() {
        return descripcionOrganismoDestinatario;
    }

    /**
     * @return
     */
    public String getDescripcionDocumento() {
        return descripcionDocumento;
    }

    /**
     * @return
     */
    public String getDescripcionIdiomaDocumento() {
        return descripcionIdiomaDocumento;
    }

    /**
     * @return
     */
    public String getCorreo() {
        return correo;
    }

    /**
     * @return
     */
    public String getProcedenciaGeografica() {
        return procedenciaGeografica;
    }

    /**
     * @return
     */
    public String getIdiomaExtracto() {
        return idiomaExtracto;
    }

    public boolean getLeido() {
        return leidos;
    }

    public String getEntidad1Nuevo() {
        return entidad1Nuevo;
    }

    public String getEntidad2Nuevo() {
        return entidad2Nuevo;
    }

    public String getAltresNuevo() {
        return altresNuevo;
    }

    public String getComentarioNuevo() {
        return comentarioNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getEntidad1Grabada() {
        return entidad1Grabada;
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
        this.context = ctx;
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
            System.out.println("ERROR: S'ha produ\357t un error a logLopdBZENTRA");
        } finally {
            ToolsBD.closeConn(conn, ps, null);
        }
    }

    /**
 * @return the numeroRegistros060
   */
    public int getNumeroRegistros060() {
        return numeroRegistros060;
    }

    /**
 * @param numeroRegistros060 the numeroRegistros060 to set
 */
    public void setNumeroRegistros060(int numeroRegistros060) {
        this.numeroRegistros060 = numeroRegistros060;
    }
}
