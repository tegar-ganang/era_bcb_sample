package ddss.data;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import ddss.db.ChaakOpenHelper;
import ddss.db.ContainersService;
import ddss.db.PersonsService;
import ddss.db.TaskAddressListService;
import ddss.db.TaskPersonnelService;
import ddss.db.TaskSurveyService;
import ddss.db.TasksService;
import ddss.loader.Container;
import ddss.loader.ContainerType;
import ddss.loader.Personal;
import ddss.loader.SubTask;
import ddss.loader.Surveillance;
import ddss.loader.Task;
import ddss.loader.TaskPersonnal;
import ddss.util.STATUS;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DumpForAccount extends Activity implements OnClickListener {

    private TextView m_txtTaskName;

    private TextView m_txtUsername;

    private TextView m_txtDumpFileLocation;

    private LinearLayout m_layoutDumpFileLocation;

    private Button m_btnGenerate;

    private int m_AccountId;

    private TaskPersonnal m_Account;

    private Task m_AssociatedTask;

    private Personal m_AssociatedPerson;

    public DumpForAccount() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            prepareView();
            retrieveAccountInformation();
            showAccountInformation();
        } catch (Exception ex) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setMessage(ex.getMessage());
            alertDialog.setButton(getString(R.string.OK), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            alertDialog.show();
        }
    }

    private void prepareView() {
        setContentView(R.layout.dump_for_account);
        m_txtTaskName = (TextView) findViewById(R.id.txtTaskName);
        m_txtUsername = (TextView) findViewById(R.id.txtUsername);
        m_txtDumpFileLocation = (TextView) findViewById(R.id.txtDumpFileLocation);
        m_layoutDumpFileLocation = (LinearLayout) findViewById(R.id.layoutDumpFileLocation);
        m_btnGenerate = (Button) findViewById(R.id.btnGenerate);
        m_layoutDumpFileLocation.setVisibility(View.GONE);
        m_btnGenerate.setOnClickListener(this);
    }

    private void retrieveAccountInformation() throws Exception {
        Bundle bundle = getIntent().getExtras();
        m_AccountId = bundle.getInt("chaak.parameter.accountId");
        ChaakOpenHelper dbHelper = new ChaakOpenHelper(getApplicationContext());
        SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
        TaskPersonnelService taskPersonnelSrv = new TaskPersonnelService(dbReader, this);
        m_Account = taskPersonnelSrv.findById(m_AccountId);
        if (m_Account == null) throw new Exception(getString(R.string.accountIdNotValid));
        TasksService taskSrv = new TasksService(dbReader, this);
        m_AssociatedTask = taskSrv.findById(m_Account.IdTask);
        PersonsService personsSrv = new PersonsService(dbReader, this);
        m_AssociatedPerson = personsSrv.findById(m_Account.IdPerson);
        dbReader.close();
    }

    private void showAccountInformation() {
        m_txtTaskName.setText(m_AssociatedTask.Name);
        m_txtUsername.setText(String.format("%s %s %s", m_AssociatedPerson.firstname, m_AssociatedPerson.lastname1, m_AssociatedPerson.lastname2));
    }

    public void onClick(View arg0) {
        String externalStatus = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(externalStatus)) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setMessage(getString(R.string.dumpFailExternalStorage));
            alertDialog.setButton(getString(R.string.OK), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                }
            });
            alertDialog.show();
            return;
        }
        ChaakOpenHelper dbHelper = new ChaakOpenHelper(getApplicationContext());
        SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
        ContainersService containersSrv = new ContainersService(dbReader, this);
        ArrayList<Container> breedingSitesList = containersSrv.findAll(true);
        TaskAddressListService addrListSrv = new TaskAddressListService(dbReader, this);
        ArrayList<SubTask> addrList = addrListSrv.getAddressListByAccountId(m_AccountId);
        Document xDoc = new Document();
        Element root = xDoc.createElement("", "AccountDump");
        xDoc.addChild(Node.ELEMENT, root);
        Element accountInfo = xDoc.createElement("", "AccountInformation");
        accountInfo.setAttribute(null, "key", Integer.toString(m_AccountId));
        Element taskName = xDoc.createElement("", "TaskName");
        taskName.addChild(Node.TEXT, m_AssociatedTask.Name);
        accountInfo.addChild(Node.ELEMENT, taskName);
        Element userName = xDoc.createElement("", "User");
        Element firstName = xDoc.createElement("", "FirstName");
        firstName.addChild(Node.TEXT, m_AssociatedPerson.firstname);
        userName.addChild(Node.ELEMENT, firstName);
        Element lastName1 = xDoc.createElement("", "LastName1");
        lastName1.addChild(Node.TEXT, m_AssociatedPerson.lastname1);
        userName.addChild(Node.ELEMENT, lastName1);
        Element lastName2 = xDoc.createElement("", "LastName2");
        lastName2.addChild(Node.TEXT, m_AssociatedPerson.lastname2);
        userName.addChild(Node.ELEMENT, lastName2);
        accountInfo.addChild(Node.ELEMENT, userName);
        root.addChild(Node.ELEMENT, accountInfo);
        Element containers = xDoc.createElement("", "BreedingSites");
        for (Container bSite : breedingSitesList) {
            Element cont = xDoc.createElement("", "BreedingSite");
            cont.setAttribute(null, "key", Integer.toString(bSite.ID));
            cont.setAttribute(null, "isExterior", bSite.bexterior == ContainerType.BOTH || bSite.bexterior == ContainerType.EXTERIOR ? "1" : "0");
            cont.setAttribute(null, "isInterior", bSite.bexterior == ContainerType.BOTH || bSite.bexterior == ContainerType.INTERIOR ? "1" : "0");
            Element nameCont = xDoc.createElement("", "Name");
            nameCont.addChild(Node.TEXT, bSite.Name);
            cont.addChild(Node.ELEMENT, nameCont);
            containers.addChild(Node.ELEMENT, cont);
        }
        root.addChild(Node.ELEMENT, containers);
        Element surveys = xDoc.createElement("", "Surveys");
        TaskSurveyService surveySrv = new TaskSurveyService(dbReader, this);
        for (SubTask subtask : addrList) {
            Element survey = xDoc.createElement("", "Address");
            survey.setAttribute(null, "key", Integer.toString(subtask.ID));
            Element name = xDoc.createElement("", "Name");
            name.addChild(Node.TEXT, subtask.Name);
            survey.addChild(Node.ELEMENT, name);
            Element status = xDoc.createElement("", "Status");
            status.setAttribute(null, "key", Integer.toString(STATUS.toInt(subtask.Status)));
            status.addChild(Node.TEXT, STATUS.toString(subtask.Status, this));
            survey.addChild(Node.ELEMENT, status);
            Element surveyData = xDoc.createElement("", "SurveyData");
            ArrayList<Surveillance> data = surveySrv.getSurveyDataByTaskAddressId(subtask.ID);
            for (Surveillance survData : data) {
                Element containerValues = xDoc.createElement("", "BreedingSiteValues");
                containerValues.setAttribute(null, "key", Integer.toString(survData.iidcontainer));
                containerValues.setAttribute(null, "isExterior", survData.bexterior ? "1" : "0");
                containerValues.setAttribute(null, "existent", Integer.toString(survData.iexistent));
                containerValues.setAttribute(null, "water", Integer.toString(survData.iagua));
                containerValues.setAttribute(null, "larvae", Integer.toString(survData.ilarvae));
                containerValues.setAttribute(null, "pupae", Integer.toString(survData.ipupae));
                surveyData.addChild(Node.ELEMENT, containerValues);
            }
            survey.addChild(Node.ELEMENT, surveyData);
            surveys.addChild(Node.ELEMENT, survey);
        }
        root.addChild(Node.ELEMENT, surveys);
        dbReader.close();
        try {
            Date now = new Date();
            SimpleDateFormat formatDate = new SimpleDateFormat("yyyyMMdd_HHmmss");
            File storageDir = Environment.getExternalStorageDirectory();
            String filePath = String.format("%s%s%s_%s_%s.xml", storageDir.getAbsolutePath(), File.separator, formatDate.format(now), m_AssociatedTask.Name.replaceAll("[^[a-z][A-Z][0-9]]", "_"), m_AssociatedPerson.UserName.replaceAll("[^[a-z][A-Z][0-9]]", "_"));
            String fileStyle = String.format("%s_%s_%s.xsl", formatDate.format(now), m_AssociatedTask.Name.replaceAll("[^[a-z][A-Z][0-9]]", "_"), m_AssociatedPerson.UserName.replaceAll("[^[a-z][A-Z][0-9]]", "_"));
            String fileStylePath = storageDir.getAbsolutePath() + File.separator + fileStyle;
            ByteArrayOutputStream xmlBytes = new ByteArrayOutputStream();
            KXmlSerializer serializer = new KXmlSerializer();
            serializer.setOutput(xmlBytes, "UTF-8");
            xDoc.write(serializer);
            String xmlStr = xmlBytes.toString();
            xmlStr = xmlStr.replaceFirst("\\?>", "?><?xml-stylesheet type=\"text/xsl\" href=\"" + fileStyle + "\"?>\n");
            File dumpFile = new File(filePath);
            dumpFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(dumpFile));
            writer.write(xmlStr);
            writer.flush();
            writer.close();
            try {
                AssetManager assets = getAssets();
                InputStream in = assets.open("cellphoneDump2html.xsl");
                OutputStream out = new FileOutputStream(fileStylePath);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.flush();
                out.close();
            } catch (Exception ex) {
                Log.e("Chaak", ex.toString());
            }
            m_txtDumpFileLocation.setText(dumpFile.getAbsolutePath());
            m_layoutDumpFileLocation.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
