package com.jgeppert.struts2.jquery.grid.showcase.action;

import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.hibernate.Transaction;
import com.googlecode.s2hibernate.struts2.plugin.annotations.TransactionTarget;
import com.jgeppert.struts2.jquery.grid.showcase.dao.CustomersDao;
import com.jgeppert.struts2.jquery.grid.showcase.dao.EmployeeDao;
import com.jgeppert.struts2.jquery.grid.showcase.model.Customers;
import com.jgeppert.struts2.jquery.grid.showcase.model.Employees;
import com.opensymphony.xwork2.ActionSupport;

@Results({ @Result(name = "error", location = "messages.jsp") })
public class EditCustomerAction extends ActionSupport {

    private static final long serialVersionUID = -3454448309088641394L;

    private static final Log log = LogFactory.getLog(EditCustomerAction.class);

    private CustomersDao customersDao = new CustomersDao();

    private EmployeeDao employeeDao = new EmployeeDao();

    private String oper = "edit";

    private String id;

    private String customername;

    private String contactfirstname;

    private String contactlastname;

    private String country;

    private String city;

    private double creditlimit;

    private Employees salesemployee;

    @TransactionTarget
    protected Transaction hTransaction;

    public String execute() throws Exception {
        log.debug("Edit Customer :" + id);
        Customers customer;
        try {
            if (oper.equalsIgnoreCase("add")) {
                log.debug("Add Customer");
                customer = new Customers();
                int nextid = customersDao.nextCustomerNumber();
                log.debug("Id for ne Customer is " + nextid);
                customer.setCustomernumber(nextid);
                customer.setCustomername(customername);
                customer.setCountry(country);
                customer.setCity(city);
                customer.setCreditlimit(creditlimit);
                customer.setContactfirstname(contactfirstname);
                customer.setContactlastname(contactlastname);
                if (salesemployee != null) {
                    customer.setSalesemployee(employeeDao.get(salesemployee.getEmployeenumber()));
                }
                customersDao.save(customer);
            } else if (oper.equalsIgnoreCase("edit")) {
                log.debug("Edit Customer");
                customer = customersDao.get(Integer.parseInt(id));
                customer.setCustomername(customername);
                customer.setCountry(country);
                customer.setCity(city);
                customer.setCreditlimit(creditlimit);
                customer.setContactfirstname(contactfirstname);
                customer.setContactlastname(contactlastname);
                if (salesemployee != null) {
                    customer.setSalesemployee(employeeDao.get(salesemployee.getEmployeenumber()));
                }
                customersDao.update(customer);
            } else if (oper.equalsIgnoreCase("del")) {
                StringTokenizer ids = new StringTokenizer(id, ",");
                while (ids.hasMoreTokens()) {
                    int removeId = Integer.parseInt(ids.nextToken());
                    log.debug("Delete Customer " + removeId);
                    customersDao.delete(removeId);
                }
            }
            hTransaction.commit();
        } catch (Exception e) {
            hTransaction.rollback();
            addActionError("ERROR : " + e.getLocalizedMessage());
            addActionError("Is Database in read/write modus?");
            return "error";
        }
        return NONE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setOper(String oper) {
        this.oper = oper;
    }

    public String getCustomername() {
        return customername;
    }

    public void setCustomername(String customername) {
        this.customername = customername;
    }

    public double getCreditlimit() {
        return creditlimit;
    }

    public void setCreditlimit(double creditlimit) {
        this.creditlimit = creditlimit;
    }

    public Employees getSalesemployee() {
        return salesemployee;
    }

    public void setSalesemployee(Employees salesemployee) {
        this.salesemployee = salesemployee;
    }

    public String getContactfirstname() {
        return contactfirstname;
    }

    public void setContactfirstname(String contactfirstname) {
        this.contactfirstname = contactfirstname;
    }

    public String getContactlastname() {
        return contactlastname;
    }

    public void setContactlastname(String contactlastname) {
        this.contactlastname = contactlastname;
    }
}
