import tearsol.*;
import com.ssc.tnet.*;
import com.ssc.tnetmsg.*;
import java.io.*;

public class TNetTCPWrapperTester implements TNetTCPWrapperCallback, TNetUDPWrapperCallback {

    public TNetTCPWrapper m_MyTNetWrapper = null;

    public TNetUDPWrapper m_MyUDPWrapper = null;

    public TNetTCPWrapperTester() {
        m_MyTNetWrapper = new TNetTCPWrapper(600, this);
        m_MyUDPWrapper = new TNetUDPWrapper(600, this);
    }

    public static void main(String[] args) {
        TNetTCPWrapperTester tntcpwt = new TNetTCPWrapperTester();
        if (tntcpwt.m_MyTNetWrapper == null) {
            System.out.println("TNetTCPWrapperTester - m_MyTNetWrapper == null");
            return;
        }
        if (!tntcpwt.m_MyTNetWrapper.connect("65.23.155.44", 5050)) {
            System.out.println("TNetTCPWrapperTester - connect failed.");
            return;
        }
        tntcpwt.m_MyTNetWrapper.login("Bill", "billpwd");
        tntcpwt.m_MyTNetWrapper.login("Robert", "robert");
        tntcpwt.m_MyTNetWrapper.login("Jenny", "jennypwd");
    }

    public void login_response() {
        System.out.println("TNetTCPWrapperTester - login_response callback");
        if (m_MyTNetWrapper.m_AccountID == 0) {
            System.out.println("TNetTCPWrapperTester - username exists but wrong pwd");
            return;
        }
        if (m_MyTNetWrapper.m_AccountID == 1) {
            System.out.println("TNetTCPWrapperTester - username does not exist");
            return;
        }
        if (m_MyTNetWrapper.m_AccountID == 2) {
            System.out.println("TNetTCPWrapperTester - username already logged in");
            return;
        }
        m_MyUDPWrapper.m_CharacterID = 103;
        TNetRAint32 id_list = TNetRAint32.create();
        id_list.append(new Integer(108));
        id_list.append(new Integer(100));
        id_list.append(new Integer(4127));
        m_MyTNetWrapper.privateChat(id_list, "Howdy!");
        TNetRAint32 id_list2 = TNetRAint32.create();
        id_list2.append(new Integer(111));
        m_MyTNetWrapper.getCharacterProperties(id_list2);
    }

    public void account_list_response() {
        System.out.println("TNetTCPWrapperTester - connected_account_response callback");
    }

    public void buddy_list_response() {
        for (int i = 0; i < m_MyTNetWrapper.m_BuddyList.size(); i++) {
            account_struct as = (account_struct) (m_MyTNetWrapper.m_BuddyList.elementAt(i));
            System.out.println("TNetTCPWrapperTester::buddy_list_response=" + as.get_account_name());
        }
        m_MyTNetWrapper.disconnect();
    }

    public void character_list_response() {
        for (int i = 0; i < m_MyTNetWrapper.m_CharacterList.size(); i++) {
            character_struct as = (character_struct) (m_MyTNetWrapper.m_CharacterList.elementAt(i));
            System.out.println("TNetTCPWrapperTester::character_list_response -> resources=" + as.get_character_resources());
            System.out.println("TNetTCPWrapperTester::character_list_response -> name=" + as.get_character_name());
            System.out.println("->property_mask=" + new Integer(as.get_property_mask()).toString());
        }
        m_MyTNetWrapper.getResource("MaleBasic.axm");
    }

    public void resource_download_response(String Filename, byte[] binary, int length) {
        System.out.println("resource_download_response : filename = " + Filename + ", binary size = " + new Integer(length).toString());
        FileOutputStream fos;
        DataOutputStream ds;
        try {
            fos = new FileOutputStream("./" + Filename);
            ds = new DataOutputStream(fos);
            ds.write(binary, 0, length);
            ds.close();
            fos.close();
        } catch (IOException e) {
            System.out.println("Error creating file: ./" + Filename);
        }
        m_MyTNetWrapper.AddBuddy(102);
        m_MyTNetWrapper.getBuddyList();
    }

    public void private_chat_response(int SenderID, String text) {
        System.out.println("TNetTCPWrapperTester - private_chat_response callback");
    }

    public void send_entity_location() {
        System.out.println("TNetTCPWrapperTester - send_entity_location callback");
        m_MyUDPWrapper.m_CharacterID = 103;
        m_MyUDPWrapper.m_dPosX = 102.234;
        m_MyUDPWrapper.m_dPosY = 0.123456789012345;
        m_MyUDPWrapper.m_dPosZ = 0;
    }

    public void entity_list_response() {
        System.out.println("TNetTCPWrapperTester - entity_list_response callback");
    }
}
