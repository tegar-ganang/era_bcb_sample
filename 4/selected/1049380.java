package asterisk;

import globali.jcFunzioni;
import globali.jcPostgreSQL;
import globali.jcVariabili;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.asteriskjava.manager.*;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.event.*;
import org.asteriskjava.manager.response.ManagerResponse;

public class jcAsterisk implements ManagerEventListener {

    private ManagerConnection managerConnection;

    public static boolean IS_CONNESSO = false;

    public jcAsterisk() throws IOException {
        ManagerConnectionFactory factory = new ManagerConnectionFactory(jcVariabili.ASTERISK_SERVER, jcPostgreSQL.USER, jcPostgreSQL.PASS);
        this.managerConnection = factory.createManagerConnection();
        if (jcVariabili.DEBUG) System.out.println("Protocollo: " + this.managerConnection.getProtocolIdentifier());
    }

    public void chiama(String num) throws IOException, TimeoutException {
        OriginateAction originateAction = new OriginateAction();
        ManagerResponse originateResponse;
        originateAction.setChannel(jcVariabili.ASTERISK_SOURCE);
        originateAction.setContext(jcVariabili.ASTERISK_CONTEXT);
        originateAction.setExten(jcVariabili.ASTERISK_OUT + num);
        originateAction.setPriority(new Integer(1));
        originateAction.setCallerId(jcVariabili.ASTERISK_INTERNO);
        originateResponse = this.managerConnection.sendAction(originateAction, 30000);
        if (jcVariabili.DEBUG) System.out.println("Sorgente: " + jcVariabili.ASTERISK_SOURCE + "\nContext: " + jcVariabili.ASTERISK_CONTEXT + "\nChiamato numero:" + jcVariabili.ASTERISK_OUT + num + "\nRisposta: " + originateResponse.getResponse() + "\nMessaggio: " + originateResponse.getMessage());
    }

    public void connetti() {
        managerConnection.addEventListener(this);
        try {
            managerConnection.login();
            IS_CONNESSO = true;
        } catch (IllegalStateException ex) {
            Logger.getLogger(jcAsterisk.class.getName()).log(Level.SEVERE, null, ex);
            jcFunzioni.erroreSQL(ex.toString());
        } catch (IOException ex) {
            Logger.getLogger(jcAsterisk.class.getName()).log(Level.SEVERE, null, ex);
            jcFunzioni.erroreSQL(ex.toString());
        } catch (AuthenticationFailedException ex) {
            Logger.getLogger(jcAsterisk.class.getName()).log(Level.SEVERE, null, ex);
            jcFunzioni.erroreSQL(ex.toString());
        } catch (TimeoutException ex) {
            Logger.getLogger(jcAsterisk.class.getName()).log(Level.SEVERE, null, ex);
            jcFunzioni.erroreSQL(ex.toString());
        }
    }

    public void disconnetti() {
        managerConnection.logoff();
        IS_CONNESSO = false;
    }

    public void onManagerEvent(ManagerEvent event) {
        String evento = event.getClass().getSimpleName();
        if (evento.equals("DialEvent")) {
            org.asteriskjava.manager.event.DialEvent call = (org.asteriskjava.manager.event.DialEvent) event;
            if (call.getDestination() != null) {
                if (call.getDestination().startsWith(jcVariabili.ASTERISK_SOURCE)) {
                    try {
                        Statement s = jcPostgreSQL.myDb.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                        ResultSet wr = s.executeQuery("SELECT clienteid, nome, cognome FROM clienti WHERE tel='" + call.getCallerIdNum() + "' OR cell='" + call.getCallerIdNum() + "' LIMIT 1");
                        int agg = 0;
                        while (wr.next()) {
                            jcVariabili.FINESTRA_PRINCIPALE.jlClienteInRicezione.setText(wr.getString("nome").trim() + (wr.getObject("cognome") == null ? "" : " " + wr.getString("cognome").trim()));
                            jcVariabili.FINESTRA_PRINCIPALE.ASTERISK_CLIENTEID = wr.getInt("clienteid");
                            agg = 0;
                        }
                        if (agg == 0) {
                            jcVariabili.FINESTRA_PRINCIPALE.jlClienteInRicezione.setText("-");
                            jcVariabili.FINESTRA_PRINCIPALE.ASTERISK_CLIENTEID = -1;
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(jcAsterisk.class.getName()).log(Level.SEVERE, null, ex);
                        if (jcVariabili.DEBUG) System.out.println("Errore riconoscimento cliente: " + ex.toString());
                    }
                    jcVariabili.FINESTRA_PRINCIPALE.jlClienteNumeroInRicezione.setText(call.getCallerIdNum());
                    if (jcVariabili.DEBUG) System.out.println("ASTERISK\nEvento: " + evento + "\nStato: " + call.getDialStatus() + "\nNumero: " + call.getCallerIdNum() + "\nNome: " + call.getCallerIdName() + "\nCanale: " + call.getChannel() + "\nlinea: " + call.getLine() + "\nID: " + call.getUniqueId() + "\nCanale: " + call.getChannel() + "\nSorce: " + call.getSource() + "\nDest" + call.getDestination() + "\n\n");
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
    }
}
