package pos.resources;

import java.math.BigDecimal;

/**
 *
 * @author MaGicBank
 */
public class UserLoginBeans {

    private int id;

    private String code;

    private String name;

    private String permission;

    private String lang;

    private String read;

    private String write;

    private String exec;

    private String sid;

    private int list;

    private int item;

    private BigDecimal amount;

    private BigDecimal cash;

    private BigDecimal credit;

    private BigDecimal amex;

    private BigDecimal diner;

    private BigDecimal jcb;

    private BigDecimal master;

    private BigDecimal visa;

    /** Creates a new instance of UserLoginBeans */
    public UserLoginBeans() {
    }

    public UserLoginBeans(int id, String code, String name, String permission, String lang, String read, String write, String exec, String sid, int list, int item, BigDecimal amount, BigDecimal cash, BigDecimal credit, BigDecimal amex, BigDecimal diner, BigDecimal jcb, BigDecimal master, BigDecimal visa) {
        this.setId(id);
        this.setCode(code);
        this.setName(name);
        this.setPermission(permission);
        this.setLang(lang);
        this.setRead(read);
        this.setWrite(write);
        this.setExec(exec);
        this.setSid(sid);
        this.setList(list);
        this.setItem(item);
        this.setAmount(amount);
        this.setCash(cash);
        this.setCredit(credit);
        this.setAmex(amex);
        this.setDiner(diner);
        this.setJcb(jcb);
        this.setMaster(master);
        this.setVisa(visa);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getRead() {
        return read;
    }

    public void setRead(String read) {
        this.read = read;
    }

    public String getWrite() {
        return write;
    }

    public void setWrite(String write) {
        this.write = write;
    }

    public String getExec() {
        return exec;
    }

    public void setExec(String exec) {
        this.exec = exec;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public int getList() {
        return list;
    }

    public void setList(int list) {
        this.list = list;
    }

    public int getItem() {
        return item;
    }

    public void setItem(int item) {
        this.item = item;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getAmex() {
        return amex;
    }

    public void setAmex(BigDecimal amex) {
        this.amex = amex;
    }

    public BigDecimal getDiner() {
        return diner;
    }

    public void setDiner(BigDecimal diner) {
        this.diner = diner;
    }

    public BigDecimal getJcb() {
        return jcb;
    }

    public void setJcb(BigDecimal jcb) {
        this.jcb = jcb;
    }

    public BigDecimal getMaster() {
        return master;
    }

    public void setMaster(BigDecimal master) {
        this.master = master;
    }

    public BigDecimal getVisa() {
        return visa;
    }

    public void setVisa(BigDecimal visa) {
        this.visa = visa;
    }

    public void addList(int list) {
        this.list = this.list + list;
    }

    public void addItem(int item) {
        this.item = this.item + item;
    }

    public void addAmount(BigDecimal amount) {
        this.amount = this.amount.add(amount);
    }

    public void addCash(BigDecimal cash) {
        this.cash = this.cash.add(cash);
    }

    public void addCredit(BigDecimal credit) {
        this.credit = this.credit.add(credit);
    }

    public void addAmex(BigDecimal amex) {
        this.amex = this.amex.add(amex);
    }

    public void addDiner(BigDecimal diner) {
        this.diner = this.diner.add(diner);
    }

    public void addJcb(BigDecimal jcb) {
        this.jcb = this.jcb.add(jcb);
    }

    public void addMaster(BigDecimal master) {
        this.master = this.master.add(master);
    }

    public void addVisa(BigDecimal visa) {
        this.visa = this.visa.add(visa);
    }
}
