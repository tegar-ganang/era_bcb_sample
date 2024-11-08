package br.gov.sjc.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;
import br.gov.sjc.socialalimenta.R;
import br.gov.sjc.socialalimenta.SocialAlimenta2Activity;

public class EnviarArquivoFTP extends Activity implements OnClickListener, Runnable {

    private static String file = "/data/data/br.gov.sjc.socialalimenta/files/socialAlimenta.xml";

    private ProgressDialog dialog;

    private ProgressDialog progressDialog;

    FTPClient ftp = new FTPClient();

    TextView notificacao;

    boolean retorno = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enviararquivoftp);
        clickSemClick();
        notificacao = (TextView) findViewById(R.enviarArquivo.lblnotificacao);
        String NomeArquivo = "socialAlimenta-";
        NomeArquivo += getMacAddress();
        String tempString = NomeArquivo.replace(':', '-');
        Log.i("WriteFile", "Nome do arquivo:" + tempString);
        if (uploadFTP("10.1.30.1", "ntadm/social.alimenta", "so1234", "tablet_ftp", "/data/data/br.gov.sjc.socialalimenta/files/", tempString + ".xml")) {
            notificacao.setText("Arquivo enviado com sucesso");
            notificacao.setVisibility(View.VISIBLE);
        } else {
            notificacao.setText("Erro na Classe");
            notificacao.setVisibility(View.VISIBLE);
        }
        Button b = (Button) findViewById(R.enviarArquivo.btnenviarFTP);
        b.setOnClickListener(this);
    }

    public void clickSemClick() {
        Log.v("clickSemClick()", "Iniciada a Fun��o");
    }

    public void onClick(View v) {
        Log.v("onClick()", "Botao voltar acionado");
        Intent ittEVoltar = new Intent(getApplicationContext(), SocialAlimenta2Activity.class);
        startActivity(ittEVoltar);
    }

    public void run() {
        String login = "celebrandoavida";
        String senha = "c.c.b.r.";
        String diretorio = "/data/data/br.gov.sjc.socialalimenta/files/";
        Log.v("run()", "Atribuidas as Vari�veis");
        try {
            String NomeArquivo = "socialAlimenta-";
            NomeArquivo += getMacAddress();
            String tempString = NomeArquivo.replace(':', '-');
            Log.i("WriteFile", "Nome do arquivo:" + tempString);
            handler.post(new Runnable() {

                public void run() {
                    notificacao.setText("Arquivo enviado com sucesso");
                    notificacao.setVisibility(View.VISIBLE);
                    Log.v("run()", "Try Executado ");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("run", "Catch Executado " + e.getMessage());
            notificacao.setText("==" + e.getMessage());
            notificacao.setVisibility(View.VISIBLE);
        } finally {
            dialog.dismiss();
            Log.v("run", "finally Executado ");
        }
    }

    public static String loadFileAsString(String filePath) throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    final Handler dialogHandler = new Handler() {

        public void handleMessage(Message msg) {
            progressDialog = new ProgressDialog(EnviarArquivoFTP.this);
            progressDialog.setTitle("Consultando Pend�ncias");
            progressDialog.setMessage("Aguarde...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }
    };

    final Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            progressDialog.dismiss();
        }
    };

    public String getMacAddress() {
        try {
            return loadFileAsString("/sys/class/net/eth0/address").toUpperCase().substring(0, 17);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * 
	 * @param Arquivo
	 * @return
	 * @throws IOException 
	 * @throws SocketException 
	 */
    public boolean downloadFTP(String ipFTP, String loginFTP, String senhaFTP, String diretorioFTP, String diretorioAndroid, String arquivoFTP) throws SocketException, IOException {
        boolean retorno = false;
        FileOutputStream arqReceber = null;
        try {
            ftp.connect(ipFTP);
            Log.i("DownloadFTP", "Connected: " + ipFTP);
            ftp.login(loginFTP, senhaFTP);
            Log.i("DownloadFTP", "Logged on");
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
            arqReceber = new FileOutputStream(file.toString());
            ftp.retrieveFile("/tablet_ftp/Novo/socialAlimenta.xml", arqReceber);
            retorno = true;
            ftp.disconnect();
            Log.i("DownloadFTP", "retorno:" + retorno);
        } catch (Exception e) {
            ftp.disconnect();
            Log.e("DownloadFTP", "Erro:" + e.getMessage());
        } finally {
            Log.e("DownloadFTP", "Finally");
        }
        return retorno;
    }

    /**
	 * Unico m�todo ftp em uso atualmente
	 * @param ipFTP
	 * @param loginFTP
	 * @param senhaFTP
	 * @param diretorioFTP
	 * @param diretorioAndroid
	 * @param arquivoFTP
	 * @return
	 */
    public boolean uploadFTP(String ipFTP, String loginFTP, String senhaFTP, String diretorioFTP, String diretorioAndroid, String arquivoFTP) {
        try {
            dialogHandler.sendEmptyMessage(0);
            File file = new File(diretorioAndroid);
            File file2 = new File(diretorioAndroid + arquivoFTP);
            Log.v("uploadFTP", "Atribuidas as vari�veis");
            String status = "";
            if (file.isDirectory()) {
                Log.v("uploadFTP", "� diret�rio");
                if (file.list().length > 0) {
                    Log.v("uploadFTP", "file.list().length > 0");
                    ftp.connect(ipFTP);
                    ftp.login(loginFTP, senhaFTP);
                    ftp.enterLocalPassiveMode();
                    ftp.setFileTransferMode(FTPClient.ASCII_FILE_TYPE);
                    ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
                    ftp.changeWorkingDirectory(diretorioFTP);
                    FileInputStream arqEnviar = new FileInputStream(diretorioAndroid + arquivoFTP);
                    Log.v("uploadFTP", "FileInputStream declarado");
                    if (ftp.storeFile(arquivoFTP, arqEnviar)) {
                        Log.v("uploadFTP", "ftp.storeFile(arquivoFTP, arqEnviar)");
                        status = ftp.getStatus().toString();
                        Log.v("uploadFTP", "getStatus(): " + status);
                        if (file2.delete()) {
                            Log.i("uploadFTP", "Arquivo " + arquivoFTP + " exclu�do com sucesso");
                            retorno = true;
                        } else {
                            Log.e("uploadFTP", "Erro ao excluir o arquivo " + arquivoFTP);
                            retorno = false;
                        }
                    } else {
                        Log.e("uploadFTP", "ERRO: arquivo " + arquivoFTP + "n�o foi enviado!");
                        retorno = false;
                    }
                } else {
                    Log.e("uploadFTP", "N�o existe o arquivo " + arquivoFTP + "neste diret�rio!");
                    retorno = false;
                }
            } else {
                Log.e("uploadFTP", "N�o � diret�rio");
                retorno = false;
            }
            if (ftp.isConnected()) {
                Log.v("uploadFTP", "isConnected ");
                ftp.abort();
                status = ftp.getStatus().toString();
                Log.v("uploadFTP", "quit " + retorno);
            }
            return retorno;
        } catch (IOException e) {
            Log.e("uploadFTP", "ERRO FTP: " + e);
            retorno = false;
            return retorno;
        } finally {
            handler.sendEmptyMessage(0);
            Log.v("uploadFTP", "finally executado");
        }
    }
}
