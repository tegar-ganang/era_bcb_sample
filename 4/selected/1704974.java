package bienvenido;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import javax.microedition.io.Connector;
import javax.obex.*;
import java.lang.*;
import java.util.Calendar;
import javax.swing.JOptionPane;

public class ClienteOBEX {

    public static boolean activar_envio;

    public static String fra;

    public static boolean activar_encabezado;

    public static String encabezado;

    public static String asunto;

    public static String mensaje;

    public static int conecciones_exitosas;

    public static File file;

    public static String segmentaFrase(String frase) {
        int l = frase.length();
        String f = frase;
        if (l < 27) {
            fra = "";
            return f;
        } else {
            f = "";
            for (int i = 0; i < 27; i++) {
                f = f + frase.charAt(i);
            }
            int j = 26;
            char c = frase.charAt(j);
            while (c != ' ') {
                j = j - 1;
                c = frase.charAt(j);
            }
            f = f.substring(0, j);
            fra = frase.substring(j + 1, l);
            return f;
        }
    }

    public static BufferedImage ConvertirTxtPng(String txt) {
        fra = txt;
        int c = 60;
        String segmento;
        BufferedImage bufferedImage = new BufferedImage(250, 250, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font mifuente = new Font("Calibri", Font.BOLD, 20);
        g2.setFont(mifuente);
        while (fra != "") {
            segmento = segmentaFrase(fra);
            g2.drawString(segmento, 0, c);
            c = c + 20;
        }
        g2.dispose();
        return bufferedImage;
    }

    public static void cliente_OBEX(String[] args) throws IOException, InterruptedException {
        try {
            System.out.println("cliente obex");
            TestConnection aux = new TestConnection();
            file = new File("C:\\Bienvenido\\" + asunto + ".png");
            ImageIO.write(ConvertirTxtPng(encabezado + mensaje), "png", file);
            System.out.println(encabezado + mensaje);
        } catch (NullPointerException error) {
            System.out.println("Error null" + error);
        }
        String serverURL = null;
        if ((args != null) && (args.length > 0)) {
            serverURL = args[0];
        }
        if (serverURL == null) {
            String[] searchArgs = null;
            ServicesSearch.servicios(searchArgs);
            if (ServicesSearch.serviceFound.size() == 0) {
                System.out.println("Servicios OBEX no encontrados.");
                return;
            }
            int j = ServicesSearch.largo_servicios;
            if (ClienteOBEX.activar_envio) {
                for (int i = 0; i < j; i++) {
                    System.out.println(ServicesSearch.serviceFound.elementAt(i));
                    System.out.println("largo servicio o dispositivos: " + j);
                    String nick = (String) ServicesSearch.nickDispositivos.elementAt(i);
                    String direc = (String) ServicesSearch.direcDispositivos.elementAt(i);
                    System.out.println(direc + " " + nick + " Pasada nº: " + i);
                    Usuario usuario = new Usuario(nick, direc);
                    TestConnection conecta = new TestConnection();
                    if (conecta.existeUsuario(usuario)) {
                        if (!conecta.usuarioBloqueado(usuario)) {
                            if (conecta.usuarioHabilitado(usuario)) {
                                try {
                                    BienvenidoView.Infoenvio.setText("Ejecutando envío de " + asunto);
                                    if (nick.equals("")) nick = "Desconocido";
                                    BienvenidoView.Infoenvio2.setText("Enviando a: " + nick);
                                    serverURL = (String) ServicesSearch.serviceFound.elementAt(i);
                                    System.out.println("Conectándose a " + serverURL);
                                    ClientSession clientSession = (ClientSession) Connector.open(serverURL);
                                    HeaderSet hsConnectReply = clientSession.connect(null);
                                    if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
                                        System.out.println("Falla en la conexión");
                                        return;
                                    }
                                    HeaderSet hsOperation = clientSession.createHeaderSet();
                                    hsOperation.setHeader(HeaderSet.NAME, asunto + ".png");
                                    hsOperation.setHeader(HeaderSet.TYPE, "image");
                                    FileInputStream fis = new FileInputStream(file);
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                    byte[] buf = new byte[1024];
                                    try {
                                        for (int readNum; (readNum = fis.read(buf)) != -1; ) {
                                            bos.write(buf, 0, readNum);
                                        }
                                    } catch (IOException ex) {
                                    }
                                    byte[] bytes = bos.toByteArray();
                                    Operation putOperation = clientSession.put(hsOperation);
                                    byte data[] = bytes;
                                    OutputStream os = putOperation.openOutputStream();
                                    os.write(data);
                                    os.close();
                                    putOperation.close();
                                    clientSession.disconnect(null);
                                    clientSession.close();
                                    System.out.println(ServicesSearch.direcDispositivos.elementAt(i));
                                    System.out.println(ServicesSearch.nickDispositivos.elementAt(i));
                                    conecta.actualizaUsuarioNickNull(usuario);
                                    conecta.conexExitosas();
                                    conecta.modificaHabilitado(usuario);
                                    Calendar calendario = Calendar.getInstance();
                                    int dia = calendario.get(Calendar.DAY_OF_WEEK);
                                    conecta.setcontadorDia(dia);
                                    BienvenidoView.Infoenvio2.setText("Envío de mensaje realizado con éxito");
                                } catch (IOException e) {
                                    System.out.println("Conexión rechazada por el usuario " + nick);
                                    System.out.println("Error: " + e);
                                    BienvenidoView.Infoenvio2.setText("Envío de mensaje no fue realizado");
                                    BienvenidoView.Infoenvio2.setVisible(true);
                                    conecta.conexRechazadas();
                                    conecta.actualizaUsuarioNickNull(usuario);
                                    conecta.modificaHabilitado(usuario);
                                }
                            } else {
                                System.out.println("Usuario no habilitado para recibir el msje: " + nick);
                            }
                        } else {
                            System.out.println("Usuario Bloqueado:" + nick);
                        }
                    } else {
                        try {
                            BienvenidoView.Infoenvio.setText("Ejecutando envío ");
                            if (nick.equals("")) nick = "Desconocido";
                            BienvenidoView.Infoenvio2.setText("Enviando a: " + nick);
                            serverURL = (String) ServicesSearch.serviceFound.elementAt(i);
                            System.out.println("Conectándose a " + serverURL);
                            ClientSession clientSession = (ClientSession) Connector.open(serverURL);
                            HeaderSet hsConnectReply = clientSession.connect(null);
                            if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
                                System.out.println("Falla en la conexión");
                                return;
                            }
                            HeaderSet hsOperation = clientSession.createHeaderSet();
                            hsOperation.setHeader(HeaderSet.NAME, asunto + ".png");
                            hsOperation.setHeader(HeaderSet.TYPE, "image");
                            FileInputStream fis = new FileInputStream(file);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte[] buf = new byte[1024];
                            try {
                                for (int readNum; (readNum = fis.read(buf)) != -1; ) {
                                    bos.write(buf, 0, readNum);
                                }
                            } catch (IOException ex) {
                            }
                            byte[] bytes = bos.toByteArray();
                            Operation putOperation = clientSession.put(hsOperation);
                            byte data[] = bytes;
                            OutputStream os = putOperation.openOutputStream();
                            os.write(data);
                            os.close();
                            putOperation.close();
                            clientSession.disconnect(null);
                            clientSession.close();
                            System.out.print(ServicesSearch.direcDispositivos.elementAt(i));
                            System.out.println(ServicesSearch.nickDispositivos.elementAt(i));
                            usuario.setHabilitado(1);
                            conecta.AgregarUsuario(usuario);
                            conecta.conexExitosas();
                            Calendar calendario = Calendar.getInstance();
                            int dia = calendario.get(Calendar.DAY_OF_WEEK);
                            conecta.setcontadorDia(dia);
                            BienvenidoView.Infoenvio2.setText("Envío de mensaje realizado con éxito");
                        } catch (IOException e) {
                            System.out.println("Conexión rechazada por el usuario");
                            System.out.println("Error: " + e);
                            BienvenidoView.Infoenvio2.setText("Envío de mensaje no fue realizado");
                            BienvenidoView.Infoenvio2.setVisible(true);
                            usuario.setHabilitado(1);
                            conecta.AgregarUsuario(usuario);
                            conecta.conexRechazadas();
                        }
                    }
                }
            }
        }
    }
}
