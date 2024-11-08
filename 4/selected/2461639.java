package com.chuidiang.pom_version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import junit.framework.TestCase;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.FileUtils;

/**
 * Test para la clase CambiaPom
 * 
 * @author Chuidiang
 * 
 */
public class TestCambiaPom extends TestCase {

    /** Path en el que estan los ficheros pom.xml de pruebas */
    private static final String SRC_TEST_CONFIG = "./src/test/config/";

    /** Borra ficheros de resultados de test anteriores */
    @Override
    protected void setUp() throws Exception {
        borraFicherosDeResultados();
        super.setUp();
    }

    /**
     * Borra los ficheros pom.xml y pom1.xml del path SRT_TEST_CONFIG.<br>
     * Dichos ficheros se supone que son de resultados de otros test
     */
    private void borraFicherosDeResultados() {
        File pom = new File(SRC_TEST_CONFIG, "pom.xml");
        if (pom.exists()) if (!pom.delete()) System.out.println("No puedo borrar pom.xml");
        pom = new File(SRC_TEST_CONFIG, "pom1.xml");
        if (pom.exists()) if (!pom.delete()) System.out.println("No puedo borrar pom1.xml");
    }

    /**
     * Borra ficheros de resultados de test anteriores
     */
    @Override
    protected void tearDown() throws Exception {
        borraFicherosDeResultados();
        super.tearDown();
    }

    /**
     * testea metodo limpiaEspacios. Este test es mas para confirmar que
     * replaceAll() de String funciona como espero que para probar el metodo.
     */
    public void testLimpiaEspacios() {
        CambiaPom cambia = new CambiaPom(null, null, false, null, null);
        String cadena = "  hola  ";
        assertEquals("hola", cambia.limpiaEspacios(cadena));
        cadena = "	\r\n dos\n";
        assertEquals("dos", cambia.limpiaEspacios(cadena));
    }

    /**
     * Testea el metodo reemplazaPropieadesPorValor(), comprobando que cambia en
     * una cadena de texto variables que existen estilo ${variable} por su
     * valor.
     */
    public void testCambiaVariables() {
        CambiaPom cambia = new CambiaPom(null, null, false, null, null);
        Properties propiedades = new Properties();
        propiedades.setProperty("unaVersion", "1.2.3");
        String valorObtenido = cambia.reemplazaPropiedadesPorValor("${unaVersion}", propiedades);
        assertEquals("1.2.3", valorObtenido);
        valorObtenido = cambia.reemplazaPropiedadesPorValor("uno ${unaVersion} dos", propiedades);
        assertEquals("uno 1.2.3 dos", valorObtenido);
        valorObtenido = cambia.reemplazaPropiedadesPorValor("uno ${noexiste} dos", propiedades);
        assertEquals("uno ${noexiste} dos", valorObtenido);
    }

    /**
     * Con un pom yun cambio de version para hacer, comprueba que genera los dos
     * ficheros pom.xml y pom1.xml y que ha cambiado la version.
     */
    public void testEscrituraFichero() {
        try {
            FileUtils.copyFile(new File(SRC_TEST_CONFIG, "pom_testEscrituraFichero.xml"), new File(SRC_TEST_CONFIG, "pom.xml"));
        } catch (IOException e) {
            fail("No se puede copiar fichero " + e);
        }
        Artifact artifactOrigen = new Artifact();
        artifactOrigen.setGroupId("com.chuidiang");
        artifactOrigen.setArtifactId("pom_version");
        artifactOrigen.setVersion("1.1.0");
        Artifact artifactDestino = new Artifact();
        artifactDestino.setGroupId("com.chuidiang");
        artifactDestino.setArtifactId("pom_version");
        artifactDestino.setVersion("1.1.1");
        Hashtable<Artifact, Artifact> cambios = new Hashtable<Artifact, Artifact>();
        cambios.put(artifactOrigen, artifactDestino);
        new CambiaPom(cambios, new File(SRC_TEST_CONFIG), false, new SystemStreamLog(), null);
        File pom = new File(SRC_TEST_CONFIG, "pom.xml");
        File pom1 = new File(SRC_TEST_CONFIG, "pom1.xml");
        assertTrue(pom.exists());
        assertTrue(pom1.exists());
        try {
            BufferedReader brentrada = new BufferedReader(new FileReader(pom));
            BufferedReader bsalida = new BufferedReader(new FileReader(pom1));
            String lineaEntrada = avanzaHastaPrimerGroupId(brentrada);
            String lineaEntrada2 = avanzaHastaPrimerGroupId(bsalida);
            while (null != lineaEntrada) {
                if (lineaEntrada.indexOf("1.1.1") == -1) {
                    assertEquals(lineaEntrada, lineaEntrada2);
                } else {
                    assertEquals("<version>1.1.1</version>", lineaEntrada.trim());
                    assertEquals("<version>1.1.0</version>", lineaEntrada2.trim());
                }
                lineaEntrada = brentrada.readLine();
                lineaEntrada2 = bsalida.readLine();
            }
            assertNull(lineaEntrada);
            assertNull(lineaEntrada2);
            brentrada.close();
            bsalida.close();
        } catch (Exception e) {
            fail("no se puede leer fichero " + e);
        }
    }

    /**
     * Comprobacion de que un fichero pom.xml con variables, se reemplazan las
     * variables por su valor a la hora de leer los artifacts.
     */
    public void testPomConVariables() {
        try {
            FileUtils.copyFile(new File(SRC_TEST_CONFIG, "pom_conVariables.xml"), new File(SRC_TEST_CONFIG, "pom.xml"));
        } catch (IOException e) {
            fail("No se puede copiar fichero " + e);
        }
        Properties propiedades = new Properties();
        propiedades.setProperty("variableExiste", "1.2.3");
        CambiaPom cambia = new CambiaPom(null, new File(SRC_TEST_CONFIG), true, new SystemStreamLog(), propiedades);
        LinkedList<Artifact> listado = cambia.getListado();
        assertNotNull(listado);
        assertEquals(3, listado.size());
        int contador = 0;
        for (Artifact artefacto : listado) {
            if ("pom_version".equals(artefacto.getArtifactId())) {
                assertEquals("1.1.0", artefacto.getVersion());
                contador++;
            }
            if ("maven-plugin-api".equals(artefacto.getArtifactId())) {
                assertEquals("1.2.3", artefacto.getVersion());
                contador++;
            }
            if ("junit".equals(artefacto.getArtifactId())) {
                assertEquals("${variableNoExiste}", artefacto.getVersion());
                contador++;
            }
        }
        assertEquals(3, contador);
    }

    /**
     * Cambio de version de un pom.xml en el que hay variables.
     */
    public void testCambiaPomConVariables() {
        boolean encontradaVariable = false;
        try {
            FileUtils.copyFile(new File(SRC_TEST_CONFIG, "pom_conVariables.xml"), new File(SRC_TEST_CONFIG, "pom.xml"));
        } catch (IOException e) {
            fail("No se puede copiar fichero " + e);
        }
        Properties propiedades = new Properties();
        propiedades.setProperty("variableExiste", "1.2.3");
        Hashtable<Artifact, Artifact> cambios = new Hashtable<Artifact, Artifact>();
        Artifact artifactOriginal = new Artifact();
        artifactOriginal.setGroupId("org.apache.maven");
        artifactOriginal.setArtifactId("maven-plugin-api");
        artifactOriginal.setVersion("1.2.3");
        Artifact artifactDestino = new Artifact();
        artifactDestino.setGroupId("org.apache.maven");
        artifactDestino.setArtifactId("maven-plugin-api");
        artifactDestino.setVersion("3.2.1");
        cambios.put(artifactOriginal, artifactDestino);
        new CambiaPom(cambios, new File(SRC_TEST_CONFIG), true, new SystemStreamLog(), propiedades);
        File pom = new File(SRC_TEST_CONFIG, "pom.xml");
        File pom1 = new File(SRC_TEST_CONFIG, "pom1.xml");
        assertTrue(pom.exists());
        assertTrue(pom1.exists());
        try {
            BufferedReader brentrada = new BufferedReader(new FileReader(pom));
            String lineaEntrada = avanzaHastaPrimerGroupId(brentrada);
            BufferedReader bsalida = new BufferedReader(new FileReader(pom1));
            String lineaEntrada2 = avanzaHastaPrimerGroupId(bsalida);
            while (null != lineaEntrada) {
                if (lineaEntrada2.indexOf("${variableExiste}") > -1) {
                    assertTrue(lineaEntrada.indexOf("3.2.1") > -1);
                    encontradaVariable = true;
                } else {
                    assertEquals(lineaEntrada, lineaEntrada2);
                }
                lineaEntrada = brentrada.readLine();
                lineaEntrada2 = bsalida.readLine();
            }
            assertNull(lineaEntrada);
            assertNull(lineaEntrada2);
            assertTrue(encontradaVariable);
            bsalida.close();
            brentrada.close();
        } catch (Exception e) {
            fail("no se puede leer fichero " + e);
        }
    }

    /**
     * Avanza en el BufferedReader, leyendo linea a linea, hasta que encuentra
     * la primera linea que contiene <groupId>. Devuelve dicha linea.
     * 
     * @param brentrada
     *            BufferedReader de lectura
     * @return
     * @throws IOException
     */
    private String avanzaHastaPrimerGroupId(BufferedReader brentrada) throws IOException {
        String lineaEntrada = brentrada.readLine();
        while ((lineaEntrada.indexOf("groupId") == -1)) lineaEntrada = brentrada.readLine();
        return lineaEntrada;
    }
}
