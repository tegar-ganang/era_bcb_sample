package krowdix.control.conexion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;
import krowdix.control.Controlador;
import krowdix.modelo.RedSocial;
import krowdix.modelo.objetos.Contenido;
import krowdix.modelo.objetos.Entidad;
import krowdix.modelo.objetos.Relacion;
import krowdix.modelo.objetos.agentes.Nodo;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * El ConectorBD sirve de persistencia para la {@link RedSocial}. Todas las
 * operaciones que se realicen en la red se van realizando a la vez en una base
 * de datos temporal. Para hacer permanentes estos cambios, se debe hacer uso de
 * una llamada a {@link #guardarComo(String)}.
 * 
 * Por el modo en que está implementado el conector, no se puede tener más de
 * una instancia del mismo a la vez en un programa (uso del patrón Singleton),
 * pero además no se debe permitir la ejecución simultánea de dos instancias del
 * programa. Para evitarlo, antes de usar el conector es necesario llamar al
 * método {@link #comprobarAccesoExclusivo()}, que se encargará de comprobar si
 * es la actual la única instancia del programa.
 * 
 * @author Daniel Alonso Fernández
 */
public class ConectorBD {

    /**
	 * Archivo sobre el que haremos el cerrojo para que sólo haya una instancia
	 * del programa en ejecución.
	 */
    private static FileOutputStream cerrojo;

    /**
	 * Instancia única del conector.
	 */
    private static ConectorBD conectorBD;

    /**
	 * Nombre que tendrá la base de datos temporal que se usará para guardar los
	 * cambios que se realicen en la red. Al ser constante, no se permite la
	 * ejecución de más de una instancia del programa.
	 */
    private static final String nombreBDtemp = "krowdix.tmp";

    /**
	 * Comprueba si la actual es la única ejecución del programa, intentando
	 * crear un cerrojo. Si la creación del cerrojo tiene éxito, no hay más
	 * instancias en ejecución, pero si falla se lanza una excepción. Este
	 * método debería invocarse antes de realizar cualquier operación con el
	 * conector, para evitar que puedan producirse errores no recuperables. Si
	 * el método se ejecuta más de una vez, las llamadas posteriores a la
	 * primera se ignoran.
	 * 
	 * @throws Exception
	 *             Si no se pudo obtener acceso exclusivo
	 */
    public static void comprobarAccesoExclusivo() throws Exception {
        if (cerrojo == null) {
            File archivoCerrojo = new File(nombreBDtemp + ".lock");
            cerrojo = new FileOutputStream(archivoCerrojo);
            if (cerrojo.getChannel().tryLock() == null) {
                cerrojo.close();
                throw new IOException();
            }
        }
    }

    /**
	 * Devuelve una instancia del ConectorBD.
	 * 
	 * Todas las llamadas a este método durante la ejecución del programa
	 * devolverán el mismo ConectorBD, por lo que si éste tiene que ser usado
	 * por varios objetos, estos últimos pueden invocar directamente al método
	 * en lugar de guardar punteros.
	 * 
	 * @return una instancia única del ConectorBD
	 */
    public static ConectorBD getConectorBD() {
        if (conectorBD == null) {
            conectorBD = new ConectorBD();
        }
        return conectorBD;
    }

    /**
	 * Conexión SQL con la base de datos.
	 */
    private Connection conn;

    /**
	 * Identificador.
	 */
    private int ultimoIdNodos, ultimoIdContenidos, ultimoIdRelaciones;

    /**
	 * Constructor por defecto. Es privado porque para obtener la instancia del
	 * controlador hay que realizar una llamada a {@link #getConectorBD()}.
	 * 
	 * Durante la creación del objeto, se añade un shutdown hook a la máquina
	 * virtual de Java. Este shutdow hook se encargará de limpiar todos los
	 * archivos temporales creados por la base de datos cuando se vaya a salir
	 * de la aplicación.
	 * 
	 * @see ConectorBD#terminar()
	 */
    private ConectorBD() {
        init();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                ConectorBD.getConectorBD().terminar();
            }
        });
    }

    /**
	 * Borra la afición con el nombre especificado del contenido pasado como
	 * primer argumento. Si el contenido no tiene la afición especificada, no se
	 * produce ninguna excepción.
	 * 
	 * @see Controlador#borrarAficion(Entidad, String)
	 * @param contenido
	 *            Contenido del que borrar la afición
	 * @param nombre
	 *            Nombre de la afición
	 */
    public void borrarAficionContenido(Contenido contenido, String nombre) {
        try {
            PreparedStatement stat = conn.prepareStatement("delete from aficionescontenidos where idcontenido = ? and nombre = ?;");
            stat.setInt(1, contenido.dameId());
            stat.setString(2, nombre);
            stat.executeUpdate();
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Borra la afición con el nombre especificado del nodo pasado como primer
	 * argumento. Si el nodo no tiene la afición especificada, no se produce
	 * ninguna excepción.
	 * 
	 * @see Controlador#borrarAficion(Entidad, String)
	 * @param nodo
	 *            Nodo del que borrar la afición
	 * @param nombre
	 *            Nombre de la afición
	 */
    public void borrarAficionNodo(Nodo nodo, String nombre) {
        try {
            PreparedStatement stat = conn.prepareStatement("delete from aficionesnodos where idnodo = ? and nombre = ?;");
            stat.setInt(1, nodo.dameId());
            stat.setString(2, nombre);
            stat.executeUpdate();
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Borra de la red el contenido con el identificador especificado
	 * 
	 * @see Controlador#borrarContenido(Contenido)
	 * @param id
	 *            Id del contenido a borrar
	 */
    public void borrarContenido(int id) {
        try {
            Statement stat = conn.createStatement();
            stat.executeUpdate("delete from contenidos where id = " + id + ";");
            stat.executeUpdate("delete from aficionescontenidos where idcontenido = " + id + ";");
            stat.executeUpdate("delete from relaciones where origen = " + id + " or destino = " + id + ";");
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Borra de la red el nodo con el identificador especificado
	 * 
	 * @see Controlador#borrarNodo(Nodo)
	 * @param id
	 *            Id del nodo a borrar
	 */
    public void borrarNodo(int id) {
        try {
            Statement stat = conn.createStatement();
            stat.executeUpdate("delete from nodos where id = " + id + ";");
            stat.executeUpdate("delete from pesosnodos where id = " + id + ";");
            stat.executeUpdate("delete from aficionesnodos where idnodo = " + id + ";");
            stat.executeUpdate("delete from relaciones where origen = " + id + " or destino = " + id + ";");
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Borra de la red la relación con el identificador especificado
	 * 
	 * @see Controlador#borrarRelacion(Relacion)
	 * @param id
	 *            Id de la relación a borrar
	 */
    public void borrarRelacion(int id) {
        try {
            Statement stat = conn.createStatement();
            stat.executeUpdate("delete from relaciones where id = " + id + ";");
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Carga la red con el nombre especificado. Si se produce cualquier error al
	 * intentar abrir el archivo o recuperar los datos, el método devuelve
	 * <code>false</code>.
	 * 
	 * @see Controlador#cargar(String)
	 * @param nombreBD
	 *            Nombre de la red a cargar
	 * @param redSocial
	 *            Red social en la que dejar los elementos cargados
	 * @return <code>true</code> si la red se cargó con éxito,
	 *         <code>false</code> en caso contrario
	 */
    public boolean cargar(String nombreBD, RedSocial redSocial) {
        try {
            connectionClose();
            File definitivo = new File(nombreBD + ".sqlite");
            if (!definitivo.exists()) return false;
            File temporal = new File(nombreBDtemp);
            temporal.delete();
            temporal.createNewFile();
            temporal.deleteOnExit();
            FileInputStream fis = new FileInputStream(definitivo);
            FileOutputStream fos = new FileOutputStream(temporal);
            int byteLeido = fis.read();
            while (byteLeido != -1) {
                fos.write(byteLeido);
                byteLeido = fis.read();
            }
            fis.close();
            fos.close();
            connectionOpen();
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("select id from nodos limit 1 offset ((select count(*) from nodos) - 1);");
            if (rs.next()) {
                ultimoIdNodos = rs.getInt("id") + 1;
            } else {
                ultimoIdNodos = 0;
            }
            rs = stat.executeQuery("select id from relaciones limit 1 offset ((select count(*) from relaciones) - 1);");
            if (rs.next()) {
                ultimoIdRelaciones = rs.getInt("id") + 1;
            } else {
                ultimoIdRelaciones = 0;
            }
            rs = stat.executeQuery("select id from contenidos limit 1 offset ((select count(*) from contenidos) - 1);");
            if (rs.next()) {
                ultimoIdContenidos = rs.getInt("id") + 1;
            } else {
                ultimoIdContenidos = 0;
            }
            Hashtable<Integer, Nodo> nodos = new Hashtable<Integer, Nodo>();
            Hashtable<Integer, Contenido> contenidos = new Hashtable<Integer, Contenido>();
            redSocial.limpiar();
            rs = stat.executeQuery("select * from nodos;");
            while (rs.next()) {
                Nodo n = new Nodo(rs.getInt("id"));
                n.setObjetivo(new double[] { rs.getDouble("objetivo1"), rs.getDouble("objetivo2"), rs.getDouble("objetivo3"), rs.getDouble("objetivo4"), rs.getDouble("objetivo5"), rs.getDouble("objetivo6"), rs.getDouble("objetivo7"), rs.getDouble("objetivo8") });
                nodos.put(n.dameId(), n);
                redSocial.addVertex(n);
            }
            for (Nodo n : nodos.values()) {
                Vector<double[]> pesos = new Vector<double[]>();
                int i = 0;
                boolean continuar = true;
                while (continuar) {
                    rs = stat.executeQuery("select * from pesosnodos where id = " + n.dameId() + " and numpeso = " + i + ";");
                    if (rs.next()) {
                        pesos.add(new double[] { rs.getDouble("objetivo1"), rs.getDouble("objetivo2"), rs.getDouble("objetivo3"), rs.getDouble("objetivo4"), rs.getDouble("objetivo5"), rs.getDouble("objetivo6"), rs.getDouble("objetivo7"), rs.getDouble("objetivo8") });
                        i++;
                    } else {
                        continuar = false;
                    }
                }
                n.actualizaNodo(pesos);
            }
            rs = stat.executeQuery("select * from aficionesnodos;");
            while (rs.next()) {
                nodos.get(rs.getInt("idnodo")).ponAficion(rs.getString("nombre"), rs.getInt("grado"));
            }
            rs = stat.executeQuery("select * from contenidos;");
            while (rs.next()) {
                Contenido c = new Contenido(rs.getInt("id"), nodos.get(rs.getInt("creador")));
                contenidos.put(c.dameId(), c);
                c.dameAutor().ponContenidos(c);
            }
            rs = stat.executeQuery("select * from aficionescontenidos;");
            while (rs.next()) {
                nodos.get(rs.getInt("idcontenido")).ponAficion(rs.getString("nombre"), rs.getInt("grado"));
            }
            rs = stat.executeQuery("select * from relaciones;");
            while (rs.next()) {
                Nodo origen = nodos.get(rs.getInt("origen"));
                Entidad destino;
                if (rs.getBoolean("nan")) {
                    destino = nodos.get(rs.getInt("destino"));
                } else {
                    destino = contenidos.get(rs.getInt("destino"));
                }
                Relacion r = new Relacion(rs.getInt("id"), origen, destino, rs.getInt("afinidad"));
                if (r.dameNaN()) {
                    origen.ponRelacion(r);
                    destino.ponRelacion(r);
                    if (redSocial.findEdge(r.dameOrigen(), ((Nodo) r.dameDestino())) == null) {
                        redSocial.addEdge(r, r.dameOrigen(), ((Nodo) r.dameDestino()), EdgeType.UNDIRECTED);
                    }
                } else {
                    origen.ponComentario(r);
                    destino.ponRelacion(r);
                }
            }
            redSocial.ponContenidos(contenidos);
            stat.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * Sobrecarga del método clone() para evitar que haya más de una instancia
	 * del ConectorBD.
	 */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
	 * Cierra la conexión con la base de datos.
	 */
    private void connectionClose() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Abre la conexión con la base de datos.
	 */
    private void connectionOpen() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + nombreBDtemp);
            conn.setAutoCommit(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Crea una afición con el nombre y el grado especificados en el contenido.
	 * 
	 * @see Controlador#crearAficion(Entidad, String, int)
	 * @param contenido
	 *            Contenido en el que crear la afición.
	 * @param nombre
	 *            Nombre de la afición.
	 * @param grado
	 *            Grado de la afición.
	 */
    public void crearAficionContenido(Contenido contenido, String nombre, int grado) {
        try {
            PreparedStatement stat = conn.prepareStatement("insert into aficionescontenidos values (?, ?, ?);");
            stat.setInt(1, contenido.dameId());
            stat.setString(2, nombre);
            stat.setInt(3, grado);
            stat.executeUpdate();
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Crea una afición con el nombre y el grado especificados en el nodo.
	 * 
	 * @see Controlador#crearAficion(Entidad, String, int)
	 * @param nodo
	 *            Nodo en el que crear la afición.
	 * @param nombre
	 *            Nombre de la afición.
	 * @param grado
	 *            Grado de la afición.
	 */
    public void crearAficionNodo(Nodo nodo, String nombre, int grado) {
        try {
            PreparedStatement stat = conn.prepareStatement("insert into aficionesnodos values (?, ?, ?);");
            stat.setInt(1, nodo.dameId());
            stat.setString(2, nombre);
            stat.setInt(3, grado);
            stat.executeUpdate();
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Añade el {@link Contenido} especificado a la red.
	 * 
	 * @see Controlador#crearContenido(Nodo)
	 * @param c
	 *            Contenido que se va a añadir.
	 */
    public void crearContenido(Contenido c) {
        try {
            Statement stat = conn.createStatement();
            stat.executeUpdate("insert into contenidos values (" + c.dameId() + ", " + c.dameAutor().dameId() + ");");
            stat.close();
            ultimoIdContenidos++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Añade el {@link Nodo} especificado a la red.
	 * 
	 * @see Controlador#crearNodo(java.awt.geom.Point2D)
	 * @param n
	 *            Nodo que se va a añadir.
	 */
    public void crearNodo(Nodo n) {
        try {
            Statement stat = conn.createStatement();
            stat.executeUpdate("insert into nodos values (" + n.dameId() + ", " + n.getObjetivo()[0] + ", " + n.getObjetivo()[1] + ", " + n.getObjetivo()[2] + ", " + n.getObjetivo()[3] + ", " + n.getObjetivo()[4] + ", " + n.getObjetivo()[5] + ", " + n.getObjetivo()[6] + ", " + n.getObjetivo()[7] + ");");
            Vector<double[]> pesos = n.getPesos();
            for (int i = 0; i < pesos.size(); i++) {
                double[] pesosi = pesos.get(i);
                stat.executeUpdate("insert into pesosnodos values (" + n.dameId() + ", " + i + ", " + pesosi[0] + ", " + pesosi[1] + ", " + pesosi[2] + ", " + pesosi[3] + ", " + pesosi[4] + ", " + pesosi[5] + ", " + pesosi[6] + ", " + pesosi[7] + ");");
            }
            stat.close();
            ultimoIdNodos++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Añade la {@link Relacion} especificada a la red.
	 * 
	 * @see Controlador#crearRelacion(Nodo, Nodo)
	 * @param r
	 *            Relacion que se va a añadir.
	 */
    public void crearRelacion(Relacion r) {
        try {
            Statement stat = conn.createStatement();
            stat.executeUpdate("insert into relaciones values (" + r.dameId() + ", " + r.dameOrigen().dameId() + ", " + r.dameDestino().dameId() + ", " + r.dameAfinidad() + ", " + (r.dameNaN() ? 1 : 0) + ");");
            stat.close();
            ultimoIdRelaciones++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Último id disponible para la clase de los {@link Contenido}s.
	 * 
	 * @return Id para el próximo contenido que se genere.
	 */
    public int dameUltimoIdContenidos() {
        return ultimoIdContenidos;
    }

    /**
	 * Último id disponible para la clase de los {@link Nodo}s.
	 * 
	 * @return Id para el próximo nodo que se genere.
	 */
    public int dameUltimoIdNodos() {
        return ultimoIdNodos;
    }

    /**
	 * Último id disponible para la clase de las {@link Relacion}es.
	 * 
	 * @return Id para la próxima relación que se genere.
	 */
    public int dameUltimoIdRelaciones() {
        return ultimoIdRelaciones;
    }

    /**
	 * Actualiza el grado de la afición de un {@link Contenido}. La afición debe
	 * existir antes de invocar a este método.
	 * 
	 * @see Controlador#editarAficion(Entidad, String, int)
	 * @param contenido
	 *            Contenido que queremos modificar.
	 * @param aficion
	 *            Una afición de ese contenido, debe existir.
	 * @param grado
	 *            El grado que queremos asignar a la afición.
	 */
    public void editarAficionContenido(Contenido contenido, String aficion, int grado) {
        try {
            PreparedStatement stat = conn.prepareStatement("update aficionescontenidos set grado = ? where idcontenido = ? and nombre = ?");
            stat.setInt(1, grado);
            stat.setInt(2, contenido.dameId());
            stat.setString(3, aficion);
            stat.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Actualiza el grado de la afición de un {@link Nodo}. La afición debe
	 * existir antes de invocar a este método.
	 * 
	 * @see Controlador#editarAficion(Entidad, String, int)
	 * @param nodo
	 *            Nodo que queremos modificar.
	 * @param aficion
	 *            Una afición de ese nodo, debe existir.
	 * @param grado
	 *            El grado que queremos asignar a la afición.
	 */
    public void editarAficionNodo(Nodo nodo, String aficion, int grado) {
        try {
            PreparedStatement stat = conn.prepareStatement("update aficionesnodos set grado = ? where idnodo = ? and nombre = ?");
            stat.setInt(1, grado);
            stat.setInt(2, nodo.dameId());
            stat.setString(3, aficion);
            stat.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Actualiza la afinidad de una {@link Relacion}, ya sea una relación de
	 * {@link Nodo} a nodo o de nodo a {@link Contenido} (comentario). Dicha
	 * relación tiene que existir antes de la llamada a este método.
	 * 
	 * @see Controlador#editarRelacion(Relacion, int)
	 * @param id
	 *            Id de la relacion que se va a actualizar.
	 * @param afinidad
	 *            Nuevo valor de la afinidad.
	 */
    public void editarRelacion(int id, int afinidad) {
        try {
            Statement stat = conn.createStatement();
            stat.executeUpdate("update relaciones set afinidad = " + afinidad + " where id = " + id + ";");
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Guarda la red activa en una base de datos con el nombre especificado. Si
	 * ya existe una base de datos con ese nombre se sobreescribirá
	 * directamente, por lo que en caso de querer evitarlo es aconsejable
	 * comprobar que el archivo no existe.
	 * 
	 * @see Controlador#guardarComo(String)
	 * @param nombreBD
	 *            Nombre con el que guardar la red.
	 * @return <code>true</code> si la red se guardó correctamente.
	 */
    public boolean guardarComo(String nombreBD) {
        try {
            conn.commit();
            File definitivo = new File(nombreBD + ".sqlite");
            definitivo.delete();
            definitivo.createNewFile();
            File temporal = new File(nombreBDtemp);
            FileInputStream fis = new FileInputStream(temporal);
            FileOutputStream fos = new FileOutputStream(definitivo);
            int byteLeido = fis.read();
            while (byteLeido != -1) {
                fos.write(byteLeido);
                byteLeido = fis.read();
            }
            fis.close();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * Prepara el conector para crear una red nueva, reiniciando los contadores
	 * y utilizando una base de datos vacía.
	 */
    private void init() {
        ultimoIdNodos = 0;
        ultimoIdContenidos = 0;
        ultimoIdRelaciones = 0;
        File file = new File(nombreBDtemp);
        file.delete();
        try {
            connectionOpen();
            Statement stat = conn.createStatement();
            stat.executeUpdate("create table if not exists nodos (id integer not null, objetivo1 double not null, objetivo2 double not null, objetivo3 double not null, objetivo4 double not null, objetivo5 double not null, objetivo6 double not null, objetivo7 double not null, objetivo8 double not null, primary key (id));");
            stat.executeUpdate("create table if not exists pesosnodos (id integer not null, numpeso integer not null, objetivo1 double not null, objetivo2 double not null, objetivo3 double not null, objetivo4 double not null, objetivo5 double not null, objetivo6 double not null, objetivo7 double not null, objetivo8 double not null, primary key (id, numpeso));");
            stat.executeUpdate("create table if not exists relaciones (id integer not null, origen integer not null, destino integer not null, afinidad integer not null, nan boolean not null, primary key (id))");
            stat.executeUpdate("create table if not exists contenidos (id integer not null, creador integer not null, primary key (id));");
            stat.executeUpdate("create table if not exists aficionesnodos (idnodo integer not null, nombre varchar not null, grado integer not null, primary key (idnodo, nombre));");
            stat.executeUpdate("create table if not exists aficionescontenidos (idcontenido integer not null, nombre varchar not null, grado integer not null, primary key (idcontenido, nombre));");
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        file.deleteOnExit();
    }

    /**
	 * Cierra la conexión con la base de datos actual, descartando cualquier
	 * cambio que no se haya guardado, y crea una nueva conexión.
	 * 
	 * @see #connectionClose()
	 * @see #init()
	 */
    public void reiniciar() {
        connectionClose();
        init();
    }

    /**
	 * Este método se ejecuta automáticamente cuando va a terminar la ejecución
	 * de la máquina virtual de Java. Se encarga de liberar el cerrojo y borrar
	 * los archivos temporales que se estuvieran usando.
	 * 
	 * Este método no debe ser invocado excepto por el shutdown hook.
	 */
    protected void terminar() {
        if (cerrojo != null) {
            try {
                cerrojo.close();
            } catch (IOException e) {
            }
            new File(nombreBDtemp + ".lock").delete();
            connectionClose();
        }
    }
}
