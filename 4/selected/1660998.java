package com.koossery.adempiere.fe.actions.payroll.conceptCatalog.concept.createModifyConcept;

import java.util.ArrayList;
import java.util.Map;
import com.koossery.adempiere.fe.actions.base.KTAdempiereBaseForm;
import com.koossery.adempiere.fe.beans.payroll.conceptCatalog.concept.HR_ConceptBean;

public class ConceptForm extends KTAdempiereBaseForm {

    public ConceptForm() {
    }

    public void finalize() throws Throwable {
        super.finalize();
    }

    private HR_ConceptBean conceptBean;

    public HR_ConceptBean getConceptBean() {
        return this.conceptBean;
    }

    public void setConceptBean(HR_ConceptBean _conceptBean) {
        this.conceptBean = _conceptBean;
    }

    private Map conceptMap;

    public Map getConceptMap() {
        return this.conceptMap;
    }

    public void setConceptMap(Map _conceptMap) {
        this.conceptMap = _conceptMap;
    }

    private Map conceptMap1;

    public Map getConceptMap1() {
        return this.conceptMap1;
    }

    public void setConceptMap1(Map _conceptMap) {
        this.conceptMap1 = _conceptMap;
    }

    private int idAccountAssignSelected;

    public int getIdAccountAssignSelected() {
        return this.idAccountAssignSelected;
    }

    public void setIdAccountAssignSelected(int _idAccountAssignSelected) {
        this.idAccountAssignSelected = _idAccountAssignSelected;
    }

    private int idClientSelected;

    public int getIdClientSelected() {
        return this.idClientSelected;
    }

    public void setIdClientSelected(int _idClientSelected) {
        this.idClientSelected = _idClientSelected;
    }

    private int idColunmTypeSelected;

    public int getIdColunmTypeSelected() {
        return this.idColunmTypeSelected;
    }

    public void setIdColunmTypeSelected(int _idColunmTypeSelected) {
        this.idColunmTypeSelected = _idColunmTypeSelected;
    }

    private int idConceptSelected;

    public int getIdConceptSelected() {
        return this.idConceptSelected;
    }

    public void setIdConceptSelected(int _idConceptSelected) {
        this.idConceptSelected = _idConceptSelected;
    }

    private int idOrgSelected;

    public int getIdOrgSelected() {
        return this.idOrgSelected;
    }

    public void setIdOrgSelected(int _idOrgSelected) {
        this.idOrgSelected = _idOrgSelected;
    }

    private int idTableTypeSelected;

    public int getIdTableTypeSelected() {
        return this.idTableTypeSelected;
    }

    public void setIdTableTypeSelected(int _idTableTypeSelected) {
        this.idTableTypeSelected = _idTableTypeSelected;
    }

    public ArrayList listOfAccountAssignAllowed;

    public ArrayList getListOfAccountAssignAllowed() {
        return this.listOfAccountAssignAllowed;
    }

    public void setListOfAccountAssignAllowed(ArrayList _listOfAccountAssignAllowed) {
        this.listOfAccountAssignAllowed = _listOfAccountAssignAllowed;
    }

    public ArrayList listOfClientAllowed;

    public ArrayList getListOfClientAllowed() {
        return this.listOfClientAllowed;
    }

    public void setListOfClientAllowed(ArrayList _listOfClientAllowed) {
        this.listOfClientAllowed = _listOfClientAllowed;
    }

    public ArrayList listOfColunmTypeAllowed;

    public ArrayList getListOfColunmTypeAllowed() {
        return this.listOfColunmTypeAllowed;
    }

    public void setListOfColunmTypeAllowed(ArrayList _listOfColunmTypeAllowed) {
        this.listOfColunmTypeAllowed = _listOfColunmTypeAllowed;
    }

    public ArrayList listOfConceptCategoryAllowed;

    public ArrayList getListOfConceptCategoryAllowed() {
        return this.listOfConceptCategoryAllowed;
    }

    public void setListOfConceptCategoryAllowed(ArrayList _listOfConceptCategoryAllowed) {
        this.listOfConceptCategoryAllowed = _listOfConceptCategoryAllowed;
    }

    public ArrayList listOfOrgAllowed;

    public ArrayList getListOfOrgAllowed() {
        return this.listOfOrgAllowed;
    }

    public void setListOfOrgAllowed(ArrayList _listOfOrgAllowed) {
        this.listOfOrgAllowed = _listOfOrgAllowed;
    }

    public ArrayList listOfTableTypeAllowed;

    public ArrayList getListOfTableTypeAllowed() {
        return this.listOfTableTypeAllowed;
    }

    public void setListOfTableTypeAllowed(ArrayList _listOfTableTypeAllowed) {
        this.listOfTableTypeAllowed = _listOfTableTypeAllowed;
    }

    private String nomOrg;

    private String nomClient;

    public String getNomOrg() {
        return nomOrg;
    }

    public void setNomOrg(String nomOrg) {
        this.nomOrg = nomOrg;
    }

    public String getNomClient() {
        return nomClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    private int display;

    public int getDisplay() {
        return this.display;
    }

    public void setDisplay(int _display) {
        this.display = _display;
    }

    private boolean active;

    private boolean printed;

    private boolean registred;

    private boolean paid;

    private boolean readwrite;

    private boolean defaut;

    private boolean employee;

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    private ArrayList ListOfClientAllowed;

    private ArrayList ListOfOrgAllowed;

    private int flag;

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public void resetForm() {
        setConceptBean(new HR_ConceptBean());
        if (getConceptMap() != null) getConceptMap().clear();
        setIdAccountAssignSelected(0);
        setIdClientSelected(0);
        setIdColunmTypeSelected(0);
        setIdConceptSelected(0);
        setIdOrgSelected(0);
        setIdTableTypeSelected(0);
    }

    public boolean isPrinted() {
        return printed;
    }

    public void setPrinted(boolean printed) {
        this.printed = printed;
    }

    public boolean isRegistred() {
        return registred;
    }

    public void setRegistred(boolean registred) {
        this.registred = registred;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public boolean isReadwrite() {
        return readwrite;
    }

    public void setReadwrite(boolean readwrite) {
        this.readwrite = readwrite;
    }

    public boolean isDefaut() {
        return defaut;
    }

    public void setDefaut(boolean defaut) {
        this.defaut = defaut;
    }

    public boolean isEmployee() {
        return employee;
    }

    public void setEmployee(boolean employee) {
        this.employee = employee;
    }
}
