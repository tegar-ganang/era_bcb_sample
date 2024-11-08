package com.ttporg.pe.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang.StringUtils;

/**
 * @author: Ricardo Guerra.
 * @clase:  UtilFicheros.java
 * @descripci?n: Clase POJO utilitario para el manejo de ficheros, tanto para la manipulacion de sus
 *               registros internos, como para el mantenimiento, compresion, etc de los ficheros y
 *               Directorios en si.
 *
 *               java.util.zip.Checksum calculan la suma de comprobaci?n requerida para la compresi?n de datos.
 *               Estas sumas de comprobaci?n se usan para detectar archivos o mensajes corruptos o se corrompieron
 *               durante la transmisi?n.
 *               El algoritmo Adler32 se conoce por ser mas r?pido que el algoritmo CRC32, y ?ste ?ltimo es conocido
 *               por ser m?s confiable.
 *
 * @author_web:  http://frameworksjava2008.blogspot.com - http://viviendoconjavaynomoririntentandolo.blogspot.com
 * @author_email: cesarricardo_guerra19@hotmail.com.
 * @fecha_de_creaci?n: 05-08-2009.
 * @fecha_de_ultima_actualizaci?n: 26-09-2009.
 * @versi?n: 3.0
 */
public class UtilFicheros {

    private static String rutaDirectorio = "C:\\Ficheros\\";

    private static String ficheroOrigen = (rutaDirectorio + "PersonasLog.txt");

    private static String ficheroDestino = (rutaDirectorio + "PersonasLogCOPY.txt");

    private static String ficheroRenombrado = (rutaDirectorio + "PersonasLog_2009.txt");

    private static String saltoLinea = "\n";

    private static String registrosInicio = "1, Cesar Ricardo,    Guerra Arnaiz,   av.Huaylas 357,     Chorrillos, 93453770" + saltoLinea + "2, Carlos Aurelio,   Cotrina Vera,    av.Los Jazmins 453, Callao,     93254561" + saltoLinea + "3, Paolo Pedro,      Guerrero Palma,  av.Arenales 1234,   San Isidro, 93423440" + saltoLinea + "4, Catherine Magaly, Cotrina Vasquez, av.Naranjal 1029,   Los Olivos, 98475548";

    private static List listaDatos = null;

    private static String separador = ",";

    private static String[] cadenasBuscar = { "Guer", "av" };

    private static String rutaDirectorioBackup = "C:\\BackupDestino\\";

    private static String ficheroBackup = "RicardoGuerra.jpg";

    private static String[] archivoZipear = new String[] { "C:\\ORIGEN\\VIDEO1.mpg", "C:\\ORIGEN\\VIDEO2.mpg", "C:\\ORIGEN\\VIDEO3.mpg" };

    private static String rutaArchivoComprimido = "C:\\DESTINO\\Comprimido.zip";

    private static String rutaSalida = "C:\\DESTINO";

    private static int TAMANO_BUFFER = 8192;

    public UtilFicheros() {
    }

    public static void main(String[] args) throws IOException {
        UtilFicheros manejoFicheros = new UtilFicheros();
        boolean creaReg = false;
        boolean leeReg = false;
        boolean buscaReg = false;
        boolean cargaReg = false;
        boolean modificaReg = false;
        boolean eliminaReg = false;
        boolean estado = false;
        manejoFicheros.comprimirFicheros(archivoZipear, rutaArchivoComprimido);
    }

    /**
	 * creaDirectorioFichero  crea el fichero validando la existencia de las carpetas y el fichero en si.
	 * @param   rutaDirectorio  rura del directorio para validar su existencia.
	 * @param   ficheroOrigen   ruta del fichero origen, para validar su existencia.
	 * @return  estado del proceso realizado.
	 */
    public boolean creaDirectorioFichero(String rutaDirectorio, String ficheroOrigen) {
        boolean estado = false;
        boolean existe = this.existeDirectorio(rutaDirectorio);
        if (existe == true) {
            existe = this.existeFichero(ficheroOrigen);
            if (existe == true) {
                estado = true;
            }
        }
        return estado;
    }

    /**
	 * buscaCadenasEnRegistrosFichero  aqui se buscan texto entre los registros del fichero
	 * @param   rutaFichero  String
	 * @param   cadenas      String[]
	 * @param   rutaDestino  String
	 * @return  estado del proceso realizado.
	 */
    public boolean buscaCadenasEnRegistrosFichero(String rutaFichero, String[] cadenas, String rutaDestino) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'buscaCadenasEnRegistrosFichero' ***********");
        boolean estado = false;
        String texto = "";
        String linea = null;
        String saltoLinea = "\n";
        String cadenaRetorno = "";
        try {
            FileReader lector = new FileReader(rutaFichero);
            BufferedReader entrada = new BufferedReader(lector);
            while ((linea = entrada.readLine()) != null) {
                texto = (linea + saltoLinea);
                String cadena_01 = cadenas[0];
                String cadena_02 = cadenas[1];
                int existe_01 = texto.indexOf(cadena_01);
                int existe_02 = texto.indexOf(cadena_02);
                if ((existe_01 != -1) && (existe_02 != -1)) {
                    cadenaRetorno += texto;
                    estado = true;
                }
            }
            System.out.println("Registros Encontrados: " + saltoLinea + cadenaRetorno);
            FileWriter escritor = new FileWriter(rutaDestino);
            BufferedWriter salida = new BufferedWriter(escritor);
            PrintWriter imprime = new PrintWriter(salida);
            imprime.println(cadenaRetorno.trim());
            imprime.close();
        } catch (FileNotFoundException e) {
            System.out.println("No se encontro el Archivo");
            e.printStackTrace();
            estado = false;
        } catch (IOException e) {
            System.out.println("Error de I/O");
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * esDirectorio valida si la ruta indicada es la de un Directorio.
	 *
	 * @param rutaFichero String
	 * @return estado del proceso realizado.
	 */
    public boolean esDirectorio(String rutaCarpeta) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'esDirectorio' ***********");
        boolean estado = false;
        File archivo = new File(rutaCarpeta);
        boolean existe = archivo.exists();
        try {
            if (existe == true) {
                if (archivo.isDirectory() == true) {
                    System.out.println("El archivo es un Directorio ...!!!");
                    estado = true;
                } else {
                    System.out.println("El archivo no es un Directorio ...!!!");
                    estado = false;
                }
            } else {
                System.out.println("La ruta no existe ...!!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * esFichero valida si la ruta indicada es la de un Fichero.
	 *
	 * @param rutaFichero String
	 * @return estado del proceso realizado.
	 */
    public boolean esFichero(String rutaFichero) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'esFichero' ***********");
        boolean estado = false;
        File archivo = new File(rutaFichero);
        boolean existe = archivo.exists();
        try {
            if (existe == true) {
                if (archivo.isFile() == true) {
                    System.out.println("El archivo es un Fichero ...!!!");
                    estado = true;
                } else {
                    System.out.println("El archivo no es un Fichero ...!!!");
                    estado = false;
                }
            } else {
                System.out.println("La ruta no existe ...!!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * existeDirectorio  busca la existencia de la carpeta en una ruta indicada, si no existe se crea la carpeta.
	 * @param   rutaCarpeta  String
	 * @return  estado del proceso realizado.
	 */
    public boolean existeDirectorio(String rutaCarpeta) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'existeDirectorio' ***********");
        boolean estado = false;
        File archivo = new File(rutaCarpeta);
        boolean existe = archivo.exists();
        try {
            if (existe == false) {
                archivo.mkdir();
                System.out.println("El Directorio se creo correctamente ...!!!");
            } else {
                System.out.println("El Directorio ya Existe ...!!!");
            }
            estado = true;
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * existeFichero busca la existencia de un fichero en una ruta indicada, si no existe el fichero se crea.
	 * @param   rutaFichero  String
	 * @return  estado del proceso realizado.
	 */
    public boolean existeFichero(String rutaFichero) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'existeFichero' ***********");
        boolean estado = false;
        File archivo = new File(rutaFichero);
        boolean existe = archivo.exists();
        try {
            if (existe == false) {
                archivo.createNewFile();
                System.out.println("El Fichero se creo correctamente ...!!!");
            } else {
                System.out.println("El Fichero ya Existe ...!!!");
            }
            estado = true;
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * clonarFichero busca un fichero en una ruta y lo clona.
	 * @param   rutaFicheroOrigen   String
	 * @param   rutaFicheroDestino  String
	 * @return  estado del proceso realizado.
	 */
    public boolean clonarFichero(String rutaFicheroOrigen, String rutaFicheroDestino) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'clonarFichero' ***********");
        boolean estado = false;
        try {
            FileInputStream entrada = new FileInputStream(rutaFicheroOrigen);
            FileOutputStream salida = new FileOutputStream(rutaFicheroDestino);
            FileChannel canalOrigen = entrada.getChannel();
            FileChannel canalDestino = salida.getChannel();
            canalOrigen.transferTo(0, canalOrigen.size(), canalDestino);
            entrada.close();
            salida.close();
            estado = true;
        } catch (IOException e) {
            System.out.println("No se encontro el archivo");
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * clonarFichero busca un fichero en una ruta y lo clona.
	 * @param   rutaFicheroOrigen   String
	 * @param   rutaFicheroDestino  String
	 * @return  estado del proceso realizado.
	 */
    public boolean clonarFichero(FileInputStream rutaFicheroOrigen, String rutaFicheroDestino) {
        System.out.println("");
        boolean estado = false;
        try {
            FileOutputStream salida = new FileOutputStream(rutaFicheroDestino);
            FileChannel canalOrigen = rutaFicheroOrigen.getChannel();
            FileChannel canalDestino = salida.getChannel();
            canalOrigen.transferTo(0, canalOrigen.size(), canalDestino);
            rutaFicheroOrigen.close();
            salida.close();
            estado = true;
        } catch (IOException e) {
            System.out.println("No se encontro el archivo");
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * renombrarFichero  busca un fichero en una ruta y lo renombra con 'rutaFicheroRenombre'.
	 * @param   rutaFicheroOrigen   String
	 * @param   rutaFicheroDestino  String
	 * @return  estado del proceso realizado.
	 */
    public boolean renombrarFichero(String rutaFicheroOrigen, String rutaFicheroRenombre) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'renombrarFichero' ***********");
        boolean estado = false;
        File archivo = new File(rutaFicheroOrigen);
        File archivoNEW = new File(rutaFicheroRenombre);
        boolean existe = archivo.exists();
        try {
            if (existe == true) {
                archivo.renameTo(archivoNEW);
                System.out.println("El archivo se Renombro ...!!!");
                estado = true;
            } else {
                System.out.println("El archivo no se pudo Renombrar ...!!!");
                estado = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * eliminaFichero  elimina un fichero en una ruta indicada.
	 * @param   rutaFichero  String
	 * @return  estado del proceso realizado.
	 */
    public boolean eliminaFichero(String rutaFichero) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'eliminaFichero' ***********");
        boolean estado = false;
        File archivo = new File(rutaFichero);
        boolean existe = archivo.exists();
        try {
            if (existe == true) {
                archivo.delete();
                System.out.println("El archivo a sido Eliminado ...!!!");
                estado = true;
            } else {
                System.out.println("El archivo no a sido Eliminado ...!!!");
                estado = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * creaRegistrosFichero  crea un fichero en una ruta indicada y crea registros de inicio en el.
	 * @param   rutaFichero     ruta del fichero a crear.
	 * @param   registrosInicio cada registro debe estar separado con separado con ','
	 * @return  estado del proceso realizado.
	 */
    public boolean creaRegistrosFichero(String rutaFichero, String registrosInicio) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'creaRegistrosFichero' ***********");
        boolean estado = false;
        try {
            FileWriter escritor = new FileWriter(rutaFichero);
            BufferedWriter salida = new BufferedWriter(escritor);
            PrintWriter imprime = new PrintWriter(salida);
            imprime.println(registrosInicio);
            imprime.close();
            estado = true;
        } catch (FileNotFoundException e) {
            System.out.println("No se encontro el Archivo: " + registrosInicio);
            e.printStackTrace();
            estado = false;
        } catch (IOException e) {
            System.out.println("Error de I/O");
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    public boolean creaFichero(String rutaFichero, String registrosInicio) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'creaRegistrosFichero' ***********");
        boolean estado = false;
        try {
            FileWriter escritor = new FileWriter(rutaFichero);
            BufferedWriter salida = new BufferedWriter(escritor);
            PrintWriter imprime = new PrintWriter(salida);
            estado = true;
        } catch (FileNotFoundException e) {
            System.out.println("No se encontro el Archivo: " + registrosInicio);
            e.printStackTrace();
            estado = false;
        } catch (IOException e) {
            System.out.println("Error de I/O");
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * leeRegistrosFichero   leer el contenido de los registro en base a la ruta indicada.
	 * @param   rutaFichero  ruta del fichero a crear.
	 * @return  estado del proceso realizado.
	 */
    public boolean leeRegistrosFichero(String rutaFichero) {
        System.out.println("");
        System.out.println("*********** DENTRO DE 'leeRegistrosFichero' ***********");
        boolean estado = false;
        String texto = "";
        String linea = null;
        String saltoLinea = "\n";
        try {
            FileReader lector = new FileReader(rutaFichero);
            BufferedReader entrada = new BufferedReader(lector);
            while ((linea = entrada.readLine()) != null) {
                texto += (linea + saltoLinea);
            }
            entrada.close();
            System.out.println("" + texto.trim());
            estado = true;
        } catch (FileNotFoundException e) {
            System.out.println("No se encontro el Archivo");
            e.printStackTrace();
            estado = false;
        } catch (IOException e) {
            System.out.println("Error de I/O");
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * leeRegistrosFicheroII  leer el contenido de los registro en base a la ruta indicada.
	 * @param   rutaFichero  ruta del fichero a crear.
	 * @return  estado del proceso realizado.
	 */
    public boolean leeRegistrosFicheroII(String rutaFichero) {
        boolean estado = false;
        String texto = "";
        String linea = null;
        String saltoLinea = "\n";
        try {
            RandomAccessFile ficheroAleatorio = new RandomAccessFile(rutaFichero, "r");
            while ((linea = ficheroAleatorio.readLine()) != null) {
                texto += (linea + saltoLinea);
            }
            System.out.println(texto);
            ficheroAleatorio.close();
        } catch (FileNotFoundException e) {
            System.out.println("No se encontro el Archivo");
            e.printStackTrace();
            estado = false;
        } catch (IOException e) {
            System.out.println("Error de I/O");
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * guardarCadenaEnRegistro guarda una cadena en una posicion indicada.
	 * @param   rutaFichero  ruta del fichero a crear.
	 * @param   cadena       cadena que sera insertada.
	 * @param   posicion     posicion a insertar la cadena.INICIO inserta al inicio, ULTIMO
	 *                       inserta al ultimo o el numero de la fila donde se insertara.
	 * @return  estado del proceso realizado.
	 */
    public boolean guardarCadenaEnRegistro(String rutaFichero, String cadena, String posicion) {
        boolean estado = false;
        String texto = "";
        String linea = null;
        String saltoLinea = "\n";
        int contador = 0;
        try {
            RandomAccessFile ficheroAleatorio = new RandomAccessFile(rutaFichero, "rw");
            long ultimaFila = ficheroAleatorio.length();
            if (posicion != null) {
                if (posicion.equalsIgnoreCase("INICIO")) {
                    ficheroAleatorio.seek(0);
                    ficheroAleatorio.writeBytes(cadena + saltoLinea);
                } else if (posicion.equalsIgnoreCase("ULTIMO")) {
                    ficheroAleatorio.seek(ultimaFila);
                    ficheroAleatorio.writeBytes(cadena + saltoLinea);
                } else {
                    while ((linea = ficheroAleatorio.readLine()) != null) {
                        texto += (linea + saltoLinea);
                        contador++;
                        if (contador == Integer.parseInt(posicion)) {
                            ficheroAleatorio.writeBytes(cadena);
                        }
                    }
                }
            }
            ficheroAleatorio.close();
            estado = true;
        } catch (FileNotFoundException e) {
            System.out.println("No se encontro el Archivo ");
            e.printStackTrace();
            estado = false;
        } catch (IOException e) {
            System.out.println("Error de I/O");
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * getListadoDirectorios_x_Ruta obtiene una lista de nombres de rutas de directorios hijos en
	 *                              base a una ruta padre.
	 * @param    rutaDirectorioPadrea.
	 * @return   lista de directorios hijos.
	 */
    public List<File> getListadoDirectorios_x_Ruta(String rutaDirectorioPadrea) {
        List<File> listaDirectorios = new Vector<File>();
        try {
            if ((rutaDirectorioPadrea != null) && !(rutaDirectorioPadrea.equals(""))) {
                File directorioRaiz = new File(rutaDirectorioPadrea);
                if (directorioRaiz.exists() == true) {
                    File[] listadoArchivos = directorioRaiz.listFiles();
                    for (int i = 0; i < listadoArchivos.length; i++) {
                        if (listadoArchivos[i].isDirectory() == true) {
                            listaDirectorios.add(listadoArchivos[i]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listaDirectorios;
    }

    /**
	 * comprimirFicheros  m?todo utilizado para la compresion estandar de directorios y ficheros.
	 *
	 * @param  archivoZipear
	 * @param  rutaArchivoComprimido
	 * @return boolean
	 */
    public boolean comprimirFicheros(String[] archivoZipear, String rutaArchivoComprimido) {
        System.out.println("**** DENTRO DE 'comprimirFicheros' ****");
        boolean estado = false;
        try {
            FileInputStream entradaFile = null;
            BufferedInputStream entradaBuffer = null;
            File archivo = new File(rutaArchivoComprimido);
            FileOutputStream salidaFile = new FileOutputStream(archivo);
            ZipOutputStream ZIP = new ZipOutputStream(salidaFile);
            byte[] datos = new byte[TAMANO_BUFFER];
            for (int i = 0; i < archivoZipear.length; i++) {
                String nombreArchivo = archivoZipear[i];
                System.out.println("Archivos: " + nombreArchivo);
                entradaFile = new FileInputStream(nombreArchivo);
                entradaBuffer = new BufferedInputStream(entradaFile, TAMANO_BUFFER);
                ZipEntry entradaZip = new ZipEntry(archivoZipear[i]);
                ZIP.putNextEntry(entradaZip);
                int bytes;
                while ((bytes = entradaBuffer.read(datos)) > 0) {
                    ZIP.write(datos, 0, bytes);
                    estado = true;
                }
                ZIP.closeEntry();
                entradaFile.close();
            }
            ZIP.close();
        } catch (IOException e) {
            e.printStackTrace();
            estado = false;
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * comprimirFicheros_x_algoritmo  m?todo utilizado para la compresion de directorios y ficheros
	 *                                en base a dos algoritmos de compresion .
	 *
	 *                                - Adler32: Para Mayor Rapidez.
	 *                                - CRC32:   Para Mayor Seguridad (Confiable).
	 *
	 * @param archivoZipear
	 * @param rutaArchivoComprimido
	 * @param nombreAlgoritmo
	 * @return boolean
	 */
    public boolean comprimirFicheros_x_algoritmo(String[] archivoZipear, String rutaArchivoComprimido, String nombreAlgoritmo) {
        System.out.println("**** DENTRO DE 'comprimirFicheros_x_algoritmo' ****");
        boolean estado = false;
        try {
            Checksum algoritmoCompresor = null;
            if (nombreAlgoritmo.equalsIgnoreCase("Adler32")) {
                algoritmoCompresor = new Adler32();
            } else if (nombreAlgoritmo.equalsIgnoreCase("CRC32")) {
                algoritmoCompresor = new CRC32();
            }
            FileInputStream entradaFile = null;
            BufferedInputStream entradaBuffer = null;
            FileOutputStream salidaFile = new FileOutputStream(rutaArchivoComprimido);
            CheckedOutputStream comprobacion = new CheckedOutputStream(salidaFile, algoritmoCompresor);
            ZipOutputStream ZIP = new ZipOutputStream(new BufferedOutputStream(comprobacion));
            System.out.println("Algoritmo Compresor: " + nombreAlgoritmo);
            byte[] datos = new byte[TAMANO_BUFFER];
            for (int i = 0; i < archivoZipear.length; i++) {
                String nombreArchivo = archivoZipear[i];
                System.out.println("Archivos: " + nombreArchivo);
                entradaFile = new FileInputStream(nombreArchivo);
                entradaBuffer = new BufferedInputStream(entradaFile, TAMANO_BUFFER);
                ZipEntry entradaZip = new ZipEntry(nombreArchivo);
                ZIP.putNextEntry(entradaZip);
                int bytes;
                while ((bytes = entradaBuffer.read(datos, 0, TAMANO_BUFFER)) != -1) {
                    ZIP.write(datos, 0, bytes);
                    estado = true;
                }
                entradaBuffer.close();
            }
            ZIP.close();
        } catch (IOException e) {
            e.printStackTrace();
            estado = false;
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * descomprimirFicheros  m?todo utilizado para descomprimir directorios y ficheros
	 *                       de forma estandar.
	 *
	 * @param rutaArchivoZipeado
	 * @param rutaSalida
	 * @return boolean
	 */
    public boolean descomprimirFicheros(String rutaArchivoZipeado, String rutaSalida) {
        System.out.println("**** DENTRO DE 'descomprimirFicheros' ****");
        boolean estado = false;
        try {
            BufferedOutputStream salidaBuffer = null;
            FileOutputStream salidaFile = null;
            FileInputStream entradaFile = new FileInputStream(rutaArchivoZipeado);
            BufferedInputStream entradaBuffer = new BufferedInputStream(entradaFile);
            ZipInputStream ZIP = new ZipInputStream(entradaBuffer);
            ZipEntry entradaZip = null;
            int bytes;
            byte[] datos = new byte[TAMANO_BUFFER];
            while ((entradaZip = ZIP.getNextEntry()) != null) {
                if (!(entradaZip.isDirectory())) {
                    System.out.println("Archivos Extraidos: " + entradaZip);
                    String rutaInternaArchivo = entradaZip.getName();
                    String nuevaRutaSalida = this.redireccionaDirectorioSalida(rutaSalida, rutaInternaArchivo);
                    salidaFile = new FileOutputStream(nuevaRutaSalida);
                    salidaBuffer = new BufferedOutputStream(salidaFile, TAMANO_BUFFER);
                    while ((bytes = ZIP.read(datos, 0, TAMANO_BUFFER)) != -1) {
                        salidaBuffer.write(datos, 0, bytes);
                        estado = true;
                    }
                    salidaBuffer.flush();
                    salidaBuffer.close();
                }
            }
            ZIP.close();
        } catch (IOException e) {
            e.printStackTrace();
            estado = false;
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * descomprimirFicheros_x_algoritmo  m?todo utilizado para descomprimir directorios y ficheros
	 *                                   en base a dos algoritmos de compresion .
	 *
	 *                                   - Adler32: Para Mayor Rapidez.
	 *                                   - CRC32:   Para Mayor Seguridad (Confiable).
	 * @param rutaArchivoZipeado
	 * @param rutaSalida
	 * @param nombreAlgoritmo
	 * @return boolean
	 */
    public boolean descomprimirFicheros_x_algoritmo(String rutaArchivoZipeado, String rutaSalida, String nombreAlgoritmo) {
        System.out.println("**** DENTRO DE 'descomprimirFicheros_x_algoritmo' ****");
        boolean estado = false;
        try {
            Checksum algoritmoCompresor = null;
            if (nombreAlgoritmo.equalsIgnoreCase("Adler32")) {
                algoritmoCompresor = new Adler32();
            } else if (nombreAlgoritmo.equalsIgnoreCase("CRC32")) {
                algoritmoCompresor = new CRC32();
            }
            BufferedOutputStream salidaBuffer = null;
            FileOutputStream salidaFile = null;
            FileInputStream entradaFile = new FileInputStream(rutaArchivoZipeado);
            CheckedInputStream comprobacion = new CheckedInputStream(entradaFile, algoritmoCompresor);
            BufferedInputStream entradaBuffer = new BufferedInputStream(comprobacion);
            ZipInputStream ZIP = new ZipInputStream(entradaBuffer);
            ZipEntry entradaZip = null;
            int bytes;
            byte[] datos = new byte[TAMANO_BUFFER];
            while ((entradaZip = ZIP.getNextEntry()) != null) {
                if (!(entradaZip.isDirectory())) {
                    System.out.println("Archivos Extraidos: " + entradaZip);
                    String rutaInternaArchivo = entradaZip.getName();
                    String nuevaRutaSalida = this.redireccionaDirectorioSalida(rutaSalida, rutaInternaArchivo);
                    salidaFile = new FileOutputStream(nuevaRutaSalida);
                    salidaBuffer = new BufferedOutputStream(salidaFile, TAMANO_BUFFER);
                    while ((bytes = ZIP.read(datos, 0, TAMANO_BUFFER)) != -1) {
                        salidaBuffer.write(datos, 0, bytes);
                        estado = true;
                    }
                    salidaBuffer.flush();
                    salidaBuffer.close();
                }
            }
            ZIP.close();
        } catch (IOException e) {
            e.printStackTrace();
            estado = false;
        } catch (Exception e) {
            e.printStackTrace();
            estado = false;
        }
        return estado;
    }

    /**
	 * verContenidorComprimido m?todo utilizado para ver el contenidor de un archivo comprimido.
	 *
	 * @param  rutaArchivoZipeado
	 * @param  rutaSalida
	 * @return boolean
	 */
    public void verContenidorComprimido(String rutaArchivoZipeado, String rutaSalida) {
        System.out.println("**** DENTRO DE 'verContenidorComprimido' ****");
        try {
            FileInputStream archivoEntrada = new FileInputStream(rutaArchivoZipeado);
            BufferedInputStream entradaBuffer = new BufferedInputStream(archivoEntrada);
            ZipInputStream ZIP = new ZipInputStream(entradaBuffer);
            ZipEntry entradaZip = null;
            DateFormat formato = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            while ((entradaZip = ZIP.getNextEntry()) != null) {
                if (!(entradaZip.isDirectory())) {
                    System.out.println("");
                    System.out.println("Nombre:     " + entradaZip.getName());
                    System.out.println("F.Creacion: " + formato.format(new Date(entradaZip.getTime())));
                    System.out.println("Tama?o (-1 si no se conoce):     " + entradaZip.getSize());
                    System.out.println("Compresion (-1 si no se conoce): " + entradaZip.getCompressedSize());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * redireccionaDirectorioSalida  m?todo utilizado para cambiar la ruta de salida de
	 *                               un fichero comprimido al momento de su descompresion.
	 *
	 * @param  rutaSalida
	 * @param  entradaFile
	 * @return String
	 **/
    public String redireccionaDirectorioSalida(String rutaSalida, String entradaFile) {
        String datoRetorno = "";
        String cadenaInvertida = "";
        String separador = "";
        int contador = 0;
        boolean estado = this.esFichero(entradaFile);
        System.out.println("esFichero: " + estado);
        if (estado == true) {
            for (int i = entradaFile.length() - 1; i >= 0; i--) {
                Character caracter = (Character) entradaFile.charAt(i);
                cadenaInvertida += caracter;
                if (caracter == '\\') {
                    contador++;
                    separador = (('\\') + "");
                } else if (caracter == '/') {
                    contador++;
                    separador = (('/') + "");
                } else if (caracter == File.separatorChar) {
                    contador++;
                    separador = ((File.separatorChar) + "");
                }
            }
            System.out.println("Cadena 'ANTES':   " + entradaFile);
            System.out.println("Cadena 'DESPUES': " + cadenaInvertida);
            System.out.println("Total Separadores#: " + contador);
            if (contador != 0) {
                System.out.println("Separador Aplicado: " + separador);
                String[] arrayCadenaInvertida = StringUtils.split(cadenaInvertida, separador);
                String cadenaReversa = StringUtils.reverse(arrayCadenaInvertida[0]);
                datoRetorno = cadenaReversa;
                System.out.println("Nombre Fichero Obtenido: " + arrayCadenaInvertida[0]);
                System.out.println("Nombre Fichero Invertido: " + datoRetorno);
            } else {
                datoRetorno = cadenaInvertida;
                System.out.println("Nombre Fichero Obtenido: " + datoRetorno);
            }
        }
        datoRetorno = (rutaSalida + File.separator + datoRetorno);
        System.out.println("Nueva Ruta Descomprimir Fichero: " + datoRetorno);
        return datoRetorno;
    }

    /**
	 * copiaArchivoRutaNueva    copia el fichero desde en una ruta origen a una de destino.
	 * @param   rutaArchivoOrigen
	 * @param   rutaArchivoDestino.
	 */
    public void copiaArchivoRutaNueva(String rutaArchivoOrigen, String rutaArchivoDestino) {
        try {
            File archivoOrigen = new File(rutaArchivoOrigen);
            File archivoDestino = new File(rutaArchivoDestino);
            this.validaFichero_01(rutaArchivoOrigen, rutaArchivoDestino);
            System.out.println("");
            if (archivoDestino.exists() == true) {
                System.out.println("********* Archivo de DESTINO, 'existente' *********");
            } else {
                System.out.println("********* Archivo de DESTINO, 'NO existente' *********");
                this.validaFichero_02(rutaArchivoDestino, rutaArchivoDestino);
            }
            this.guardarCopiaArchivo(archivoOrigen, archivoDestino);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * guardarCopiaArchivo  guarda el archivo en una nueva ruta.
	 * @param   archivoOrigen
	 * @param   archivoDestino.
	 */
    public void guardarCopiaArchivo(File archivoOrigen, File archivoDestino) {
        try {
            FileInputStream entrada = null;
            FileOutputStream salida = null;
            entrada = new FileInputStream(archivoOrigen);
            salida = new FileOutputStream(archivoDestino);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = entrada.read(buffer)) != -1) {
                salida.write(buffer, 0, bytesRead);
            }
            if (entrada != null) {
                salida.close();
            }
            if (entrada != null) {
                salida.close();
            }
            System.out.println("Proceso de copia de archivos en nueva ubicacion 'Exitoso' ...!!!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error durante el proceso de copia...!!!");
        }
    }

    /**
	 * validaFichero_01
	 * @param   rutaArchivoOrigen
	 * @param   rutaArchivoDestino
	 */
    private void validaFichero_01(String rutaArchivoOrigen, String rutaArchivoDestino) {
        try {
            File archivoOrigen = new File(rutaArchivoOrigen);
            File archivoDestino = new File(rutaArchivoDestino);
            if (archivoOrigen.exists() == false) {
                throw new IOException("El fichero: [" + rutaArchivoOrigen + "], no Encontrado ");
            }
            if (archivoOrigen.isFile() == false) {
                throw new IOException("No se puede copiar el directorio: [" + rutaArchivoOrigen + "]");
            }
            if (archivoOrigen.canRead() == false) {
                throw new IOException("El fichero: [" + rutaArchivoOrigen + "], es ilegible.");
            }
            if (archivoDestino.isDirectory() == true) {
                archivoDestino = new File(archivoDestino, archivoOrigen.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * validaFichero_02
	 * @param   rutaArchivoOrigen
	 * @param   rutaArchivoDestino
	 */
    private void validaFichero_02(String rutaArchivoOrigen, String rutaArchivoDestino) {
        try {
            File archivoDestino = new File(rutaArchivoDestino);
            String directorioPadre = archivoDestino.getParent();
            if (directorioPadre == null) {
                directorioPadre = System.getProperty("user.dir");
            }
            File directorio = new File(directorioPadre);
            if (directorio.exists() == false) {
                throw new IOException("El Fichero de destino [" + directorioPadre + "], no existe.");
            }
            if (directorio.canWrite() == false) {
                throw new IOException("El Fichero de destino: [" + directorioPadre + "], no tiene permisos de escritura. ");
            }
            if (directorio.isFile() == true) {
                throw new IOException("La ruta para el directorio: [" + directorioPadre + "], no es un directorio.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * leeCodigoFichero  leer el contenido de los registro en base a la ruta indicada.
	 * @param   rutaFichero  ruta del fichero a crear.
	 * @return  estado del proceso realizado.
	 */
    public String leeCodigoFichero(String rutaFichero) {
        String texto = "";
        String linea = null;
        String saltoLinea = "\n";
        try {
            RandomAccessFile ficheroAleatorio = new RandomAccessFile(rutaFichero, "r");
            while ((linea = ficheroAleatorio.readLine()) != null) {
                texto += (linea + saltoLinea);
            }
            System.out.println(texto);
            ficheroAleatorio.close();
        } catch (FileNotFoundException e) {
            System.out.println("No se encontro el Archivo");
            e.printStackTrace();
            texto = "";
        } catch (IOException e) {
            System.out.println("Error de I/O");
            e.printStackTrace();
            texto = "";
        }
        return texto;
    }
}
