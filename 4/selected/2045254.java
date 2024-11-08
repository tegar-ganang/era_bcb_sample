package com.everis.paiche.deploy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.MessageConsole;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.everis.paiche.util.ConsolePrintStream;
import com.everis.paiche.util.Constant;
import com.everis.paiche.util.ControllerProperties;

public class DeployConf implements Runnable {

    private IWorkbenchWindow window;

    private MessageConsole console;

    private int DIRECTORIO_RAIZ = 0;

    private String workspace;

    public DeployConf(String workspace, IWorkbenchWindow window) {
        this.window = window;
        console = Constant.findConsole("Paiche - Deploy");
        this.workspace = workspace;
    }

    public void _init() {
        console.newMessageStream().getConsole().activate();
        console.newMessageStream().getConsole().clearConsole();
        console.newMessageStream().println("[INFO] ---------------------------------------------------------");
        console.newMessageStream().println("[INFO] PAICHE DEPLOY START");
        console.newMessageStream().println("[INFO] ---------------------------------------------------------");
        console.newMessageStream().println("[INFO] init");
        console.newMessageStream().println("[INFO] ---------------------------------------------------------");
        console.newMessageStream().println("[INFO] MAVEN -CLEAN -PACKAGE");
        console.newMessageStream().println("[INFO] ---------------------------------------------------------");
        try {
            Constant.MAVEN_CLIENT.doMain(Constant.MAVEN_CLIENT_ARGS, workspace, new ConsolePrintStream(console), new ConsolePrintStream(console));
        } catch (FileNotFoundException e1) {
            console.newMessageStream().println(e1.toString());
            console.newMessageStream().println("[INFO] ---------------------------------------------------------");
            console.newMessageStream().println("[INFO] PAICHE DEPLOY END");
            console.newMessageStream().println("[INFO] ---------------------------------------------------------");
            return;
        }
        console.newMessageStream().getConsole().activate();
        console.newMessageStream().println("[INFO] ---------------------------------------------------------");
        console.newMessageStream().println("[INFO] SYNC FILES");
        console.newMessageStream().println("[INFO] ---------------------------------------------------------");
        File dirAutoDeploy = new File(ControllerProperties.getDirectoryWebServer70() + Constant.SEPARATOR + "auto-deploy");
        if (dirAutoDeploy.exists() && dirAutoDeploy.isDirectory()) {
            try {
                syncFiles(workspace, getNameVersionWorkspace(workspace));
            } catch (Exception e) {
                MessageDialog.openError(window.getShell(), Constant.PLUGIN_NAME, e.toString());
            }
        } else {
            MessageDialog.openError(window.getShell(), Constant.PLUGIN_NAME, "Directory \"auto-deploy\" no exist.");
        }
        console.newMessageStream().println("[INFO] ---------------------------------------------------------");
        console.newMessageStream().println("[INFO] PAICHE DEPLOY END");
        console.newMessageStream().println("[INFO] ---------------------------------------------------------");
        console.newMessageStream().getConsole().activate();
    }

    private boolean copyFile(File _file1, File _file2) {
        FileInputStream fis;
        FileOutputStream fos;
        try {
            fis = new FileInputStream(_file1);
            fos = new FileOutputStream(_file2);
            FileChannel canalFuente = fis.getChannel();
            canalFuente.transferTo(0, canalFuente.size(), fos.getChannel());
            fis.close();
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return false;
    }

    private void moveFile(String dirWorkspace, String dirWebServer, String subcarpeta, File fileWorkspace) {
        if (fileWorkspace.isDirectory()) {
            File _fileTemp2;
            for (int indice = 0; indice < fileWorkspace.listFiles().length; indice++) {
                _fileTemp2 = new File(dirWebServer + subcarpeta + Constant.SEPARATOR + fileWorkspace.listFiles()[indice].getName());
                if (fileWorkspace.listFiles()[indice].isDirectory()) {
                    if (!_fileTemp2.exists()) {
                        _fileTemp2.mkdir();
                    }
                    moveFile(dirWorkspace, dirWebServer, subcarpeta + Constant.SEPARATOR + fileWorkspace.listFiles()[indice].getName(), fileWorkspace.listFiles()[indice]);
                } else {
                    if (fileWorkspace.listFiles()[indice].lastModified() > _fileTemp2.lastModified()) {
                        if (copyFile(fileWorkspace.listFiles()[indice], _fileTemp2)) {
                            console.newMessageStream().println("[INFO]  adding " + _fileTemp2.getAbsolutePath().substring(DIRECTORIO_RAIZ));
                        } else {
                            console.newMessageStream().println("[ERROR] adding " + _fileTemp2.getAbsolutePath().substring(DIRECTORIO_RAIZ));
                        }
                    }
                }
            }
            _fileTemp2 = new File(dirWebServer + subcarpeta);
            verificacionFiles(fileWorkspace, _fileTemp2, subcarpeta);
        }
    }

    private void verificacionFiles(File _fileWorkspace, File _fileWebServer, String carpeta) {
        ArrayList<String> _archivosWorkspace = new ArrayList<String>();
        for (int indice = 0; indice < _fileWorkspace.listFiles().length; indice++) {
            _archivosWorkspace.add(_fileWorkspace.listFiles()[indice].getName());
        }
        ArrayList<String> _archivosWebServer = new ArrayList<String>();
        for (int indice = 0; indice < _fileWebServer.listFiles().length; indice++) {
            _archivosWebServer.add(_fileWebServer.listFiles()[indice].getName());
        }
        for (String tempWorkspace : _archivosWorkspace) {
            for (String tempWebServer : _archivosWebServer) {
                if (tempWorkspace.equals(tempWebServer)) {
                    _archivosWebServer.remove(tempWebServer);
                    break;
                }
            }
        }
        for (String tempWebServer : _archivosWebServer) {
            File file = new File(_fileWebServer.getAbsolutePath() + Constant.SEPARATOR + tempWebServer);
            deleteEnd(file);
        }
    }

    private void deleteEnd(File file) {
        if (file.isDirectory()) {
            File[] lista = file.listFiles();
            for (int y = 0; y < lista.length; y++) {
                deleteEnd(lista[y]);
            }
        } else {
            console.newMessageStream().println("[INFO]  remove " + file.getAbsolutePath().substring(DIRECTORIO_RAIZ));
            file.delete();
        }
    }

    private void syncFiles(String workspace, String nameVersion) {
        File _fileDestino = new File(ControllerProperties.getDirectoryWebServer70() + Constant.SEPARATOR + "auto-deploy" + Constant.SEPARATOR + nameVersion);
        if (!_fileDestino.exists()) {
            _fileDestino.mkdir();
            console.newMessageStream().println("[INFO]  adding Building the project folder in the WebServer 7.0 .");
        }
        DIRECTORIO_RAIZ = _fileDestino.getAbsolutePath().length() + 1;
        moveFile(workspace + Constant.SEPARATOR + "target" + Constant.SEPARATOR + nameVersion, ControllerProperties.getDirectoryWebServer70() + Constant.SEPARATOR + "auto-deploy" + Constant.SEPARATOR + nameVersion, "", new File(workspace + Constant.SEPARATOR + "target" + Constant.SEPARATOR + nameVersion));
    }

    private String getNameVersionWorkspace(String workspace) throws Exception {
        String nombre = "";
        Document document;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(workspace + Constant.SEPARATOR + "pom.xml"));
            NodeList listArtifactId = document.getElementsByTagName("artifactId");
            for (int _indi = 0; _indi < 1; _indi++) {
                nombre += listArtifactId.item(_indi).getTextContent();
            }
            nombre += "-";
            NodeList listVersion = document.getElementsByTagName("version");
            for (int _indi = 0; _indi < 1; _indi++) {
                nombre += listVersion.item(_indi).getTextContent();
            }
        } catch (SAXException e) {
            console.newMessageStream().println("[INFO] Read POM:" + e.toString());
            throw new Exception("[INFO] Error read pom.xml.");
        } catch (IOException e) {
            console.newMessageStream().println("[INFO] Read POM:" + e.toString());
            throw new Exception("[INFO] Error read pom.xml.");
        } catch (ParserConfigurationException e) {
            console.newMessageStream().println("[INFO] Read POM:" + e.toString());
            throw new Exception("[INFO] Error read pom.xml.");
        }
        return nombre;
    }

    @Override
    public void run() {
        _init();
    }
}
