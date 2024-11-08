package Faucet;

import javax.microedition.midlet.*;
import org.netbeans.microedition.lcdui.SplashScreen;
import java.io.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import java.lang.String.*;
import java.util.Date.*;
import java.util.Calendar;
import org.netbeans.microedition.lcdui.LoginScreen;

/**
 * @author luca
 */
public class Faucet extends MIDlet implements CommandListener {

    public String VERSION = "1.2.1";

    public int KVIDEO = 1;

    public int KRADIO = 2;

    public int DefaultRecType = KVIDEO;

    public int MAXCHANNELS = 200;

    public String REC_VIDEO = "video";

    public String REC_RADIO = "radio";

    public String RecType = REC_VIDEO;

    private boolean midletPaused = false;

    public String user, pass;

    String canale;

    String titolo;

    Integer formato;

    int ripetizione;

    int conservazione;

    String StringaInizio, StringaFine;

    int NumTV, NumRadio;

    String TVChannelName[] = new String[MAXCHANNELS];

    String RadioChannelName[] = new String[MAXCHANNELS];

    Integer ConvForm[] = new Integer[10];

    private Command exitCommand;

    private Command cmdConfirm;

    private Command cmdAbout1;

    private Command cmdAbout;

    private Command cmdSend;

    private Command exitCommand1;

    private Command cmdRipeti;

    private Command cmdCorreggi;

    private Command cmdTV;

    private Command cmdRetrieve;

    private Command cmdRADIO;

    private Command backCommand;

    private Command cmdLogin;

    private Command screenCommand;

    private Alert alAbout;

    private SplashScreen splStart;

    private Form frmImmissioneDati;

    private ChoiceGroup lstCanale;

    private ChoiceGroup lstFormato;

    private DateField dtEnd;

    private TextField txtTitolo;

    private DateField dtStart;

    private ChoiceGroup lstRipetizioni;

    private ChoiceGroup lstConservazione;

    private TextBox txtRiepilogo;

    private TextBox txtRisultato;

    private LoginScreen loginScreen;

    private Alert alTipoCanale;

    private SplashScreen splTest;

    private SplashScreen spltest2;

    private TextBox frmInfo;

    /**
     * The Faucet constructor.
     */
    public Faucet() {
    }

    /**
     * Initilizes the application.
     * It is called only once when the MIDlet is started. The method is called before the <code>startMIDlet</code> method.
     */
    private void initialize() {
    }

    /**
     * Performs an action assigned to the Mobile Device - MIDlet Started point.
     */
    public void startMIDlet() {
        switchDisplayable(null, getFrmImmissioneDati());
        switchDisplayable(null, getFrmInfo());
        frmInfo.setString("Caricamento dati login...");
        getLogin();
        frmInfo.setString("Caricamento canali...");
        getChannels();
        frmInfo.setString("Caricamento liste...");
        getOtherLists();
        frmInfo.setString("Impostazione canali...");
        impostaCanali(DefaultRecType);
        switchDisplayable(null, getFrmImmissioneDati());
    }

    /**
     * Performs an action assigned to the Mobile Device - MIDlet Resumed point.
     */
    public void resumeMIDlet() {
    }

    /**
     * Switches a current displayable in a display. The <code>display</code> instance is taken from <code>getDisplay</code> method. This method is used by all actions in the design for switching displayable.
     * @param alert the Alert which is temporarily set to the display; if <code>null</code>, then <code>nextDisplayable</code> is set immediately
     * @param nextDisplayable the Displayable to be set
     */
    public void switchDisplayable(Alert alert, Displayable nextDisplayable) {
        Display display = getDisplay();
        if (alert == null) {
            display.setCurrent(nextDisplayable);
        } else {
            display.setCurrent(alert, nextDisplayable);
        }
    }

    /**
     * Called by a system to indicated that a command has been invoked on a particular displayable.
     * @param command the Command that was invoked
     * @param displayable the Displayable where the command was invoked
     */
    public void commandAction(Command command, Displayable displayable) {
        if (displayable == frmImmissioneDati) {
            if (command == cmdAbout1) {
                switchDisplayable(getAlAbout(), getFrmImmissioneDati());
            } else if (command == cmdConfirm) {
                frmInfo.setString("Controllo dati inseriti...");
                switchDisplayable(null, getFrmInfo());
                estrazioneDati();
                switchDisplayable(null, getTxtRiepilogo());
                frmInfo.setString("Visualizzo dati inseriti:");
                switchDisplayable(null, getFrmInfo());
                aggiornaFormConferma();
                switchDisplayable(null, getTxtRiepilogo());
            } else if (command == cmdRADIO) {
                getAlTipoCanale().setString("Impostato: Radio");
                lstCanale.deleteAll();
                impostaCanali(KRADIO);
                RecType = REC_RADIO;
                switchDisplayable(getAlTipoCanale(), getFrmImmissioneDati());
            } else if (command == cmdRetrieve) {
                frmInfo.setString("Invio richiesta");
                switchDisplayable(null, getTxtRisultato());
                switchDisplayable(null, getFrmInfo());
                estrazioneDati();
                InvioRichiesta("http://www.vcast.it/faucetpvr/csp/server_rest.php?method=serve_list&uname=aUsername&pswd=aPassword");
                switchDisplayable(null, getTxtRisultato());
            } else if (command == cmdTV) {
                getAlTipoCanale().setString("Impostato: TV");
                lstCanale.deleteAll();
                impostaCanali(KVIDEO);
                RecType = REC_VIDEO;
                switchDisplayable(getAlTipoCanale(), getFrmImmissioneDati());
            }
        } else if (displayable == frmInfo) {
            if (command == screenCommand) {
            }
        } else if (displayable == loginScreen) {
            if (command == LoginScreen.LOGIN_COMMAND) {
            } else if (command == backCommand) {
            }
        } else if (displayable == splStart) {
            if (command == SplashScreen.DISMISS_COMMAND) {
            }
        } else if (displayable == splTest) {
            if (command == SplashScreen.DISMISS_COMMAND) {
                switchDisplayable(null, getTxtRiepilogo());
            }
        } else if (displayable == spltest2) {
            if (command == SplashScreen.DISMISS_COMMAND) {
                switchDisplayable(null, getTxtRisultato());
            }
        } else if (displayable == txtRiepilogo) {
            if (command == cmdAbout) {
                switchDisplayable(getAlAbout(), getTxtRiepilogo());
            } else if (command == cmdCorreggi) {
                switchDisplayable(null, getFrmImmissioneDati());
            } else if (command == cmdSend) {
                frmInfo.setString("Attendere...");
                switchDisplayable(null, getTxtRisultato());
                switchDisplayable(null, getFrmInfo());
                InvioRichiesta("http://www.vcast.it/faucetpvr/csp/server_rest.php?method=serve_insert&uname=aUsername&pswd=aPassword&param1=channelType&param2=TVChannelName&param3=title&param4=format&param5=fromTime&param6=toTime&param7=repeat&param8=retention");
                switchDisplayable(null, getFrmInfo());
                frmInfo.setString("Richiesta inviata...");
                switchDisplayable(null, getTxtRisultato());
            } else if (command == exitCommand) {
                exitMIDlet();
            }
        } else if (displayable == txtRisultato) {
            if (command == cmdRetrieve) {
                switchDisplayable(null, getTxtRisultato());
                frmInfo.setString("Invio richiesta");
                switchDisplayable(null, getFrmInfo());
                estrazioneDati();
                InvioRichiesta("http://www.vcast.it/faucetpvr/csp/server_rest.php?method=serve_list&uname=aUsername&pswd=aPassword");
                switchDisplayable(null, getTxtRisultato());
            } else if (command == cmdRipeti) {
                switchDisplayable(null, getFrmImmissioneDati());
            } else if (command == exitCommand1) {
                exitMIDlet();
            }
        }
    }

    /**
     * Returns an initiliazed instance of exitCommand component.
     * @return the initialized component instance
     */
    public Command getExitCommand() {
        if (exitCommand == null) {
            exitCommand = new Command("Exit", Command.EXIT, 0);
        }
        return exitCommand;
    }

    /**
     * Returns an initiliazed instance of cmdConfirm component.
     * @return the initialized component instance
     */
    public Command getCmdConfirm() {
        if (cmdConfirm == null) {
            cmdConfirm = new Command("Conferma", Command.OK, 0);
        }
        return cmdConfirm;
    }

    /**
     * Returns an initiliazed instance of cmdAbout1 component.
     * @return the initialized component instance
     */
    public Command getCmdAbout1() {
        if (cmdAbout1 == null) {
            cmdAbout1 = new Command("ABOUT", Command.SCREEN, 0);
        }
        return cmdAbout1;
    }

    /**
     * Returns an initiliazed instance of alAbout component.
     * @return the initialized component instance
     */
    public Alert getAlAbout() {
        if (alAbout == null) {
            alAbout.setString(VERSION);
            alAbout = new Alert("alert", "Faucet PVR v 1.1.1", null, null);
            alAbout.setTimeout(Alert.FOREVER);
        }
        return alAbout;
    }

    /**
     * Returns an initiliazed instance of splStart component.
     * @return the initialized component instance
     */
    public SplashScreen getSplStart() {
        if (splStart == null) {
            splStart = new SplashScreen(getDisplay());
            splStart.setTitle("Faucet PVR frontend");
            splStart.setCommandListener(this);
            splStart.setText("by Luca Cassioli 2008");
            splStart.setTimeout(10);
        }
        return splStart;
    }

    /**
     * Returns an initiliazed instance of frmImmissioneDati component.
     * @return the initialized component instance
     */
    public Form getFrmImmissioneDati() {
        if (frmImmissioneDati == null) {
            frmImmissioneDati = new Form("Immettere dati registrazione", new Item[] { getTxtTitolo(), getLstCanale(), getDtStart(), getDtEnd(), getLstFormato(), getLstRipetizioni(), getLstConservazione() });
            frmImmissioneDati.addCommand(getCmdConfirm());
            frmImmissioneDati.addCommand(getCmdTV());
            frmImmissioneDati.addCommand(getCmdRADIO());
            frmImmissioneDati.addCommand(getCmdRetrieve());
            frmImmissioneDati.addCommand(getCmdAbout1());
            frmImmissioneDati.setCommandListener(this);
        }
        return frmImmissioneDati;
    }

    /**
     * Returns an initiliazed instance of lstCanale component.
     * @return the initialized component instance
     */
    public ChoiceGroup getLstCanale() {
        if (lstCanale == null) {
            lstCanale = new ChoiceGroup("Canale", Choice.POPUP);
            lstCanale.setSelectedFlags(new boolean[] {});
        }
        return lstCanale;
    }

    /**
     * Returns an initiliazed instance of cmdAbout component.
     * @return the initialized component instance
     */
    public Command getCmdAbout() {
        if (cmdAbout == null) {
            cmdAbout = new Command("About", Command.SCREEN, 0);
        }
        return cmdAbout;
    }

    /**
     * Returns an initiliazed instance of txtRiepilogo component.
     * @return the initialized component instance
     */
    public TextBox getTxtRiepilogo() {
        if (txtRiepilogo == null) {
            txtRiepilogo = new TextBox("Riepilogo dati inseriti", "---\n", 5000, TextField.ANY);
            txtRiepilogo.addCommand(getCmdSend());
            txtRiepilogo.addCommand(getCmdCorreggi());
            txtRiepilogo.addCommand(getCmdAbout());
            txtRiepilogo.addCommand(getExitCommand());
            txtRiepilogo.setCommandListener(this);
        }
        return txtRiepilogo;
    }

    /**
     * Returns an initiliazed instance of lstFormato component.
     * @return the initialized component instance
     */
    public ChoiceGroup getLstFormato() {
        if (lstFormato == null) {
            lstFormato = new ChoiceGroup("Formato registrazione", Choice.POPUP);
            lstFormato.setFitPolicy(Choice.TEXT_WRAP_DEFAULT);
            lstFormato.setSelectedFlags(new boolean[] {});
        }
        return lstFormato;
    }

    /**
     * Returns an initiliazed instance of txtTitolo component.
     * @return the initialized component instance
     */
    public TextField getTxtTitolo() {
        if (txtTitolo == null) {
            txtTitolo = new TextField("Titolo", "[attendere prego]", 32, TextField.ANY);
        }
        return txtTitolo;
    }

    /**
     * Returns an initiliazed instance of dtStart component.
     * @return the initialized component instance
     */
    public DateField getDtStart() {
        if (dtStart == null) {
            dtStart = new DateField("Inizio", DateField.DATE_TIME);
            dtStart.setDate(new java.util.Date(System.currentTimeMillis() + 60000));
        }
        return dtStart;
    }

    /**
     * Returns an initiliazed instance of dtEnd component.
     * @return the initialized component instance
     */
    public DateField getDtEnd() {
        if (dtEnd == null) {
            dtEnd = new DateField("Fine", DateField.DATE_TIME);
            dtEnd.setDate(new java.util.Date(System.currentTimeMillis() + 60000 + 600000));
        }
        return dtEnd;
    }

    /**
     * Returns an initiliazed instance of cmdSend component.
     * @return the initialized component instance
     */
    public Command getCmdSend() {
        if (cmdSend == null) {
            cmdSend = new Command("Invia", Command.OK, 0);
        }
        return cmdSend;
    }

    /**
     * Returns an initiliazed instance of cmdRipeti component.
     * @return the initialized component instance
     */
    public Command getCmdRipeti() {
        if (cmdRipeti == null) {
            cmdRipeti = new Command("Inserimento dati", Command.SCREEN, 0);
        }
        return cmdRipeti;
    }

    /**
     * Returns an initiliazed instance of exitCommand1 component.
     * @return the initialized component instance
     */
    public Command getExitCommand1() {
        if (exitCommand1 == null) {
            exitCommand1 = new Command("Exit", Command.EXIT, 0);
        }
        return exitCommand1;
    }

    /**
     * Returns an initiliazed instance of txtRisultato component.
     * @return the initialized component instance
     */
    public TextBox getTxtRisultato() {
        if (txtRisultato == null) {
            txtRisultato = new TextBox("Risultato operazione", "-\n", 10000, TextField.ANY);
            txtRisultato.addCommand(getCmdRipeti());
            txtRisultato.addCommand(getExitCommand1());
            txtRisultato.addCommand(getCmdRetrieve());
            txtRisultato.setCommandListener(this);
        }
        return txtRisultato;
    }

    /**
     * Returns an initiliazed instance of lstRipetizioni component.
     * @return the initialized component instance
     */
    public ChoiceGroup getLstRipetizioni() {
        if (lstRipetizioni == null) {
            lstRipetizioni = new ChoiceGroup("Ripetizioni", Choice.POPUP);
            lstRipetizioni.setSelectedFlags(new boolean[] {});
        }
        return lstRipetizioni;
    }

    /**
     * Returns an initiliazed instance of lstConservazione component.
     * @return the initialized component instance
     */
    public ChoiceGroup getLstConservazione() {
        if (lstConservazione == null) {
            lstConservazione = new ChoiceGroup("Conservazione", Choice.POPUP);
            lstConservazione.setSelectedFlags(new boolean[] {});
        }
        return lstConservazione;
    }

    /**
     * Returns an initiliazed instance of cmdCorreggi component.
     * @return the initialized component instance
     */
    public Command getCmdCorreggi() {
        if (cmdCorreggi == null) {
            cmdCorreggi = new Command("Modifica dati", Command.SCREEN, 0);
        }
        return cmdCorreggi;
    }

    /**
     * Returns an initiliazed instance of cmdTV component.
     * @return the initialized component instance
     */
    public Command getCmdTV() {
        if (cmdTV == null) {
            cmdTV = new Command("TV", Command.OK, 0);
        }
        return cmdTV;
    }

    /**
     * Returns an initiliazed instance of cmdRADIO component.
     * @return the initialized component instance
     */
    public Command getCmdRADIO() {
        if (cmdRADIO == null) {
            cmdRADIO = new Command("Radio", Command.OK, 0);
        }
        return cmdRADIO;
    }

    /**
     * Returns an initiliazed instance of alTipoCanale component.
     * @return the initialized component instance
     */
    public Alert getAlTipoCanale() {
        if (alTipoCanale == null) {
            alTipoCanale = new Alert("Impostazione tipo canale", "Tipo canale impostato:", null, null);
            alTipoCanale.setTimeout(Alert.FOREVER);
        }
        return alTipoCanale;
    }

    /**
     * Returns an initiliazed instance of cmdRetrieve component.
     * @return the initialized component instance
     */
    public Command getCmdRetrieve() {
        if (cmdRetrieve == null) {
            cmdRetrieve = new Command("Mostra programmazioni", Command.SCREEN, 0);
        }
        return cmdRetrieve;
    }

    /**
     * Returns an initiliazed instance of cmdLogin component.
     * @return the initialized component instance
     */
    public Command getCmdLogin() {
        if (cmdLogin == null) {
            cmdLogin = new Command("Login", Command.SCREEN, 0);
        }
        return cmdLogin;
    }

    /**
     * Returns an initiliazed instance of backCommand component.
     * @return the initialized component instance
     */
    public Command getBackCommand() {
        if (backCommand == null) {
            backCommand = new Command("Back", Command.BACK, 0);
        }
        return backCommand;
    }

    /**
     * Returns an initiliazed instance of loginScreen component.
     * @return the initialized component instance
     */
    public LoginScreen getLoginScreen() {
        if (loginScreen == null) {
            loginScreen = new LoginScreen(getDisplay());
            loginScreen.setLabelTexts("Username", "Password");
            loginScreen.setTitle("loginScreen");
            loginScreen.addCommand(LoginScreen.LOGIN_COMMAND);
            loginScreen.addCommand(getBackCommand());
            loginScreen.setCommandListener(this);
            loginScreen.setBGColor(-3355444);
            loginScreen.setFGColor(0);
            loginScreen.setUseLoginButton(false);
            loginScreen.setLoginButtonText("Login");
        }
        return loginScreen;
    }

    /**
     * Returns an initiliazed instance of splTest component.
     * @return the initialized component instance
     */
    public SplashScreen getSplTest() {
        if (splTest == null) {
            splTest = new SplashScreen(getDisplay());
            splTest.setTitle("Attendere prego");
            splTest.setCommandListener(this);
            splTest.setText("Elaboro...");
            splTest.setTimeout(1000);
        }
        return splTest;
    }

    /**
     * Returns an initiliazed instance of spltest2 component.
     * @return the initialized component instance
     */
    public SplashScreen getSpltest2() {
        if (spltest2 == null) {
            spltest2 = new SplashScreen(getDisplay());
            spltest2.setTitle("Attendere risposta");
            spltest2.setCommandListener(this);
            spltest2.setText("Invio dati server.");
            spltest2.setTimeout(1000);
        }
        return spltest2;
    }

    /**
     * Returns an initiliazed instance of screenCommand component.
     * @return the initialized component instance
     */
    public Command getScreenCommand() {
        if (screenCommand == null) {
            screenCommand = new Command("Screen", Command.SCREEN, 0);
        }
        return screenCommand;
    }

    /**
     * Returns an initiliazed instance of frmInfo component.
     * @return the initialized component instance
     */
    public TextBox getFrmInfo() {
        if (frmInfo == null) {
            frmInfo = new TextBox("Informazione", "[niente]", 100, TextField.ANY);
            frmInfo.addCommand(getScreenCommand());
            frmInfo.setCommandListener(this);
        }
        return frmInfo;
    }

    /**
     * Returns a display instance.
     * @return the display instance.
     */
    public Display getDisplay() {
        return Display.getDisplay(this);
    }

    /**
     * Exits MIDlet.
     */
    public void exitMIDlet() {
        switchDisplayable(null, null);
        destroyApp(true);
        notifyDestroyed();
    }

    /**
     * Called when MIDlet is started.
     * Checks whether the MIDlet have been already started and initialize/starts or resumes the MIDlet.
     */
    public void startApp() {
        if (midletPaused) {
            resumeMIDlet();
        } else {
            initialize();
            startMIDlet();
        }
        midletPaused = false;
    }

    /**
     * Called when MIDlet is paused.
     */
    public void pauseApp() {
        midletPaused = true;
    }

    /**
     * Called to signal the MIDlet to terminate.
     * @param unconditional if true, then the MIDlet has to be unconditionally terminated and all resources has to be released.
     */
    public void destroyApp(boolean unconditional) {
    }

    private void InvioRichiesta(String url) {
        {
            HttpConnection http = null;
            InputStream iStrm = null;
            url = replace(url, "aUsername", user);
            url = replace(url, "aPassword", pass);
            url = replace(url, "title", titolo);
            url = replace(url, "channelType", RecType);
            url = replace(url, "TVChannelName", canale);
            url = replace(url, "format", "" + convert(formato));
            url = replace(url, "fromTime", StringaInizio);
            url = replace(url, "toTime", StringaFine);
            url = replace(url, "repeat", "" + ripetizione);
            url = replace(url, "retention", "" + conservazione);
            url = replace(url, " ", "_");
            System.out.println("URL INVIATO: " + url);
            try {
                txtRisultato.setString("Creazione connessione...");
                http = (HttpConnection) Connector.open(url);
                http.setRequestMethod(HttpConnection.POST);
                http.setRequestProperty("User-Agent", "Profile/MIDP-1.0 Configuration/CLDC-1.0");
                txtRisultato.setString(txtRisultato.getString() + "Url inviato:\n" + url);
                txtRisultato.setString(txtRisultato.getString() + "\nLettura risposta...");
                System.out.println("Msg: " + http.getResponseMessage());
                System.out.println("Code: " + http.getResponseCode());
                txtRisultato.setString(txtRisultato.getString() + "\nRisultato:" + http.getResponseCode());
                if (http.getResponseCode() == HttpConnection.HTTP_OK) {
                    txtRisultato.setString(txtRisultato.getString() + "\nConnessione riuscita. Leggo risposta...");
                    String str;
                    iStrm = http.openInputStream();
                    int length = (int) http.getLength();
                    if (length != -1) {
                        txtRisultato.setString(txtRisultato.getString() + "\nLeggo " + length + " bytes...");
                        byte serverData[] = new byte[length];
                        iStrm.read(serverData);
                        str = new String(serverData);
                        if (str.length() <= txtRisultato.getMaxSize()) {
                            txtRisultato.setString(txtRisultato.getString() + "\n\n****** Risposta server: " + str + "****** \n");
                        } else {
                            txtRisultato.setString(txtRisultato.getString() + "\n\n****** Risposta server TROPPO LUNGA per essere mostrata! ****** \n");
                        }
                        txtRisultato.setString(txtRisultato.getString() + "\n");
                    } else {
                        System.out.print("Leggo quantita'  dati sconosciuta...");
                        ByteArrayOutputStream bStrm = new ByteArrayOutputStream();
                        int ch;
                        while ((ch = iStrm.read()) != -1) {
                            System.out.print(".");
                            bStrm.write(ch);
                        }
                        str = new String(bStrm.toByteArray());
                        bStrm.close();
                        txtRisultato.setString(txtRisultato.getString() + "\n\n*****Risposta del server: " + str + "***\n");
                        txtRisultato.setString(txtRisultato.getString() + "\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                txtRisultato.setString(txtRisultato.getString() + "\nERRORE: non e' stato possibile collegarsi. Telefono fuori campo? (" + e.toString() + ")");
                System.out.println("ERRORE: non e' stato possibile collegarsi. Telefono fuori campo?");
            } finally {
                if (iStrm != null) {
                    try {
                        txtRisultato.setString(txtRisultato.getString() + "\nChiudo stream...");
                        iStrm.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (http != null) {
                    try {
                        txtRisultato.setString(txtRisultato.getString() + "\nChiudo connessione...");
                        http.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            txtRisultato.setString(txtRisultato.getString() + "\nConnessione terminata.\n");
        }
    }

    public String getMese(String data) {
        int trovato, i;
        String[] mese = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dec" };
        trovato = 99;
        for (i = 0; i <= 11; i++) {
            System.out.println(data.substring(4, 7) + " " + (mese[i]));
            if (data.substring(4, 7).equals(mese[i])) {
                trovato = i + 1;
                i = 12;
            }
        }
        if (trovato < 10) {
            return ("0" + trovato);
        } else {
            return ("" + trovato);
        }
    }

    public void getLogin() {
        System.out.println("Leggo dati di login...");
        try {
            InputStream is = null;
            int MAXLEN = 50;
            byte[] data = new byte[MAXLEN];
            String contenuto;
            is = getClass().getResourceAsStream("login.txt");
            if (is != null) {
                is.read(data, 0, MAXLEN);
                contenuto = new String(data, 0, MAXLEN);
                int fineuser = contenuto.indexOf('\n') - 1;
                int iniziopass = fineuser + 2;
                int finepass = contenuto.indexOf("\n", iniziopass) - 1;
                System.out.println("File='" + contenuto + "'");
                System.out.println("fineuser=" + fineuser);
                System.out.println("iniziopass=" + iniziopass);
                System.out.println("finepass=" + finepass);
                user = contenuto.substring(0, fineuser);
                pass = contenuto.substring(iniziopass, finepass);
                is.close();
                System.out.println(user + "," + pass);
            } else {
                System.out.println("Errore nell'accesso al file.");
                txtRisultato.setString(txtRisultato.getString() + "\nErrore nell'accesso al file.");
            }
        } catch (java.io.IOException ex) {
            System.out.println("Eccezione: errore generico nella lettura.");
            txtRisultato.setString(txtRisultato.getString() + "\nEccezione: errore generico nella lettura.");
            ex.printStackTrace();
        }
        System.out.println("Fatto.");
    }

    public void getChannels() {
        System.out.println("Leggo liste canali/stazioni..");
        try {
            InputStream is = null;
            int MAXLEN = 10000;
            byte[] data = new byte[MAXLEN];
            String contenuto;
            String chan;
            System.out.println("########TV#########");
            is = getClass().getResourceAsStream("tv.txt");
            if (is != null) {
                contenuto = new String(data, 0, is.read(data, 0, MAXLEN));
                NumTV = 0;
                while (contenuto.length() > 0) {
                    int finechan = contenuto.indexOf('\n') - 1;
                    chan = contenuto.substring(0, finechan);
                    TVChannelName[NumTV] = chan;
                    NumTV++;
                    contenuto = contenuto.substring(finechan + 2, contenuto.length());
                }
                is.close();
                System.out.println("Canali TV: " + Integer.toString(NumTV));
            } else {
                System.out.println("Errore nell'accesso alla lista dei canali TV.");
                txtRisultato.setString(txtRisultato.getString() + "\nErrore nell'accesso alla lista dei canali TV.");
            }
            System.out.println("########Radio#########");
            is = getClass().getResourceAsStream("radio.txt");
            if (is != null) {
                NumRadio = 0;
                contenuto = new String(data, 0, is.read(data, 0, MAXLEN));
                while (contenuto.length() > 0) {
                    int finechan = contenuto.indexOf('\n') - 1;
                    chan = contenuto.substring(0, finechan);
                    RadioChannelName[NumRadio] = chan;
                    NumRadio++;
                    contenuto = contenuto.substring(finechan + 2, contenuto.length());
                }
                is.close();
                System.out.println("Canali radio: " + Integer.toString(NumRadio));
            } else {
                System.out.println("Errore nell'accesso alla lista dei canali Radio.");
                txtRisultato.setString(txtRisultato.getString() + "\nErrore nell'accesso alla lista dei canali Radio.");
            }
        } catch (java.io.IOException ex) {
            System.out.println("Eccezione: errore generico nella lettura dell'elenco canali Radio/TV.");
            txtRisultato.setString(txtRisultato.getString() + "\nEccezione: errore generico nella lettura dell'elenco canali Radio/TV.");
            ex.printStackTrace();
        }
        System.out.println("Fatto.");
    }

    public void getOtherLists() {
        System.out.println("Leggo liste canali/stazioni..");
        try {
            InputStream is = null;
            int MAXLEN = 10000;
            byte[] data = new byte[MAXLEN];
            String contenuto;
            String riga;
            System.out.println("######## FORMATI #########");
            is = getClass().getResourceAsStream("formati.txt");
            if (is != null) {
                contenuto = new String(data, 0, is.read(data, 0, MAXLEN));
                int Num = 0;
                while (contenuto.length() > 0) {
                    int fineriga = contenuto.indexOf('\n') - 1;
                    riga = contenuto.substring(0, fineriga);
                    System.out.println(riga);
                    lstFormato.append(riga, null);
                    Num++;
                    contenuto = contenuto.substring(fineriga + 2, contenuto.length());
                }
                is.close();
                System.out.println("Formati: " + Integer.toString(Num));
            }
            System.out.println("######## TABELLA #########");
            is = getClass().getResourceAsStream("convert.txt");
            if (is != null) {
                contenuto = new String(data, 0, is.read(data, 0, MAXLEN));
                int Num = 0;
                while (contenuto.length() > 0) {
                    int fineriga = contenuto.indexOf('\n') - 1;
                    riga = contenuto.substring(0, fineriga);
                    System.out.println(Integer.toString(Num) + "->" + riga);
                    Num++;
                    ConvForm[Num] = Integer.valueOf(riga);
                    contenuto = contenuto.substring(fineriga + 2, contenuto.length());
                }
                is.close();
                System.out.println("Formati: " + Integer.toString(Num));
            }
            System.out.println("######## CONSERVAZIONE #########");
            is = getClass().getResourceAsStream("conserv.txt");
            if (is != null) {
                contenuto = new String(data, 0, is.read(data, 0, MAXLEN));
                int Num = 0;
                while (contenuto.length() > 0) {
                    int fineriga = contenuto.indexOf('\n') - 1;
                    riga = contenuto.substring(0, fineriga);
                    System.out.println(riga);
                    lstConservazione.append(riga, null);
                    Num++;
                    contenuto = contenuto.substring(fineriga + 2, contenuto.length());
                }
                is.close();
                System.out.println("Conservazione: " + Integer.toString(Num));
            }
            System.out.println("######## RIPETIZIONE #########");
            is = getClass().getResourceAsStream("ripetiz.txt");
            if (is != null) {
                contenuto = new String(data, 0, is.read(data, 0, MAXLEN));
                int Num = 0;
                while (contenuto.length() > 0) {
                    int fineriga = contenuto.indexOf('\n') - 1;
                    riga = contenuto.substring(0, fineriga);
                    System.out.println(riga);
                    lstRipetizioni.append(riga, null);
                    Num++;
                    contenuto = contenuto.substring(fineriga + 2, contenuto.length());
                }
                is.close();
                System.out.println("Ripetizione: " + Integer.toString(Num));
            } else {
                System.out.println("Errore nell'accesso alle liste.");
                txtRisultato.setString(txtRisultato.getString() + "\nErrore nell'accesso alle liste.");
            }
        } catch (java.io.IOException ex) {
            System.out.println("Eccezione: errore generico nella lettura delle liste.");
            txtRisultato.setString(txtRisultato.getString() + "\nEccezione: errore generico nella lettura delle liste.");
            ex.printStackTrace();
        }
        System.out.println("Fatto.");
        switchDisplayable(null, getFrmImmissioneDati());
    }

    public void estrazioneDati() {
        System.out.println(lstCanale.getSelectedIndex());
        canale = lstCanale.getString(lstCanale.getSelectedIndex());
        titolo = txtTitolo.getString();
        formato = Integer.valueOf(Integer.toString(lstFormato.getSelectedIndex() + 1));
        ripetizione = lstRipetizioni.getSelectedIndex() + 1;
        conservazione = lstConservazione.getSelectedIndex() + 1;
        Calendar cal = Calendar.getInstance();
        cal.setTime(dtStart.getDate());
        String InizioMinuto = addZero(cal.get(cal.MINUTE));
        String InizioOra = addZero(cal.get(cal.HOUR_OF_DAY));
        String InizioGiorno = addZero(cal.get(cal.DAY_OF_MONTH));
        String InizioMese = addZero((cal.get(cal.MONTH) + 1));
        String InizioAnno = (cal.get(cal.YEAR) + "").substring(2, 4);
        cal.setTime(dtEnd.getDate());
        String FineMinuto = addZero(cal.get(cal.MINUTE));
        String FineOra = addZero(cal.get(cal.HOUR_OF_DAY));
        String FineGiorno = addZero(cal.get(cal.DAY_OF_MONTH));
        String FineMese = addZero(cal.get(cal.MONTH) + 1);
        String FineAnno = (cal.get(cal.YEAR) + "").substring(2, 4);
        StringaInizio = "%22" + InizioAnno + "-" + InizioMese + "-" + InizioGiorno + "%20" + InizioOra + ":" + InizioMinuto + ":00%22";
        StringaFine = "%22" + FineAnno + "-" + FineMese + "-" + FineGiorno + "%20" + FineOra + ":" + FineMinuto + ":00%22";
        System.out.println("Dati immessi:");
        System.out.println("canale: " + canale);
        System.out.println("Titolo:" + titolo);
        System.out.println("Formato:" + lstFormato.getString(formato.byteValue() - 1));
        System.out.println("Ripetizione:" + lstRipetizioni.getString(ripetizione - 1));
        System.out.println("Conservazione:" + lstConservazione.getString(conservazione - 1));
        System.out.println("Inizio: " + InizioOra + ":" + InizioMinuto + " " + InizioGiorno + "/" + InizioMese + "/" + InizioAnno);
        System.out.println("Fine: " + FineOra + ":" + FineMinuto + " " + FineGiorno + "/" + FineMese + "/" + FineAnno);
        System.out.println("Estrazione dati terminata.");
    }

    public String addZero(int num) {
        if (num < 10) {
            return "0" + num;
        } else {
            return "" + num;
        }
    }

    public Integer convert(Integer formato) {
        formato = ConvForm[formato.byteValue()];
        return formato;
    }

    public static String replace(String _text, String _searchStr, String _replacementStr) {
        StringBuffer sb = new StringBuffer();
        int searchStringPos = _text.indexOf(_searchStr);
        int startPos = 0;
        int searchStringLength = _searchStr.length();
        while (searchStringPos != -1) {
            sb.append(_text.substring(startPos, searchStringPos)).append(_replacementStr);
            startPos = searchStringPos + searchStringLength;
            searchStringPos = _text.indexOf(_searchStr, startPos);
        }
        sb.append(_text.substring(startPos, _text.length()));
        return sb.toString();
    }

    public void aggiornaFormConferma() {
        String stringa;
        stringa = "Titolo: " + titolo + "\nCanale: " + canale + "\nFormato: " + formato;
        stringa = stringa + "\nRipetizioni: " + lstRipetizioni.getString(ripetizione - 1);
        stringa = stringa + "\nConservazione: " + lstConservazione.getString(conservazione - 1);
        stringa = stringa + "\nRipetizioni: " + lstRipetizioni.getString(ripetizione - 1);
        stringa = stringa + "\nInizio: " + StringaInizio;
        stringa = stringa + "\nFine: " + StringaFine;
        System.out.println(stringa);
        txtRiepilogo.setString(stringa);
    }

    public void impostaCanali(int t) {
        lstCanale.append("Attendere...", null);
        lstCanale.setSelectedIndex(0, true);
        if (t == KVIDEO) {
            for (int i = 1; i < NumTV; i++) {
                lstCanale.append(TVChannelName[i], null);
            }
            lstCanale.set(0, TVChannelName[0], null);
        } else {
            for (int i = 0; i < NumRadio; i++) {
                lstCanale.append(RadioChannelName[i], null);
            }
            lstCanale.set(0, RadioChannelName[0], null);
        }
        switchDisplayable(null, getFrmImmissioneDati());
        txtTitolo.setString("Inserire titolo");
    }

    public void salvaLogin() {
    }
}
