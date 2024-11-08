package rra.model;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

public class HPSStats {

    private String abilityName = null;

    private long actions = 0;

    private long outcomeHealing = 0;

    private long outcomeCritHealing = 0;

    private long outcomeOverhealing = 0;

    private List<HPSStats> subcategory = null;

    public HPSStats(String name) {
        this.abilityName = name;
    }

    public void addHealingEvent(long healing, long overhealing, boolean isCrit) {
        this.actions++;
        if (isCrit) this.outcomeCritHealing += healing; else this.outcomeHealing += healing;
        this.outcomeOverhealing += overhealing;
    }

    public void addHealingEventAndSubcategory(String[] subco, long healing, long overhealing, boolean isCrit) {
        this.addHealingEvent(healing, overhealing, isCrit);
        if (subco.length < 1) return;
        if (this.subcategory == null) this.subcategory = new ArrayList<HPSStats>();
        HPSStats t2 = new HPSStats(subco[0]);
        HPSStats t1 = (HPSStats) CollectionUtils.find(this.subcategory, new RRAPredicate(t2));
        if (t1 == null) {
            t1 = t2;
            this.subcategory.add(t1);
        }
        if (subco.length == 1) t1.addHealingEvent(healing, overhealing, isCrit); else {
            String[] newsubco = new String[subco.length - 1];
            for (int i = 0; i < newsubco.length; i++) newsubco[i] = subco[i + 1];
            t1.addHealingEventAndSubcategory(newsubco, healing, overhealing, isCrit);
        }
    }

    public String getAbilityName() {
        return this.abilityName;
    }

    public String getOverhealingPercentage() {
        if (this.actions == 0) return "0 %";
        if ((this.outcomeHealing + this.outcomeCritHealing + this.outcomeOverhealing) == 0) return "0 %";
        double t1 = (double) this.outcomeOverhealing / (double) (this.outcomeHealing + this.outcomeCritHealing + this.outcomeOverhealing);
        return NumberFormat.getPercentInstance().format(t1);
    }

    public String getHealingCritPercentage() {
        if (this.actions == 0) return "0 %";
        if ((this.outcomeHealing + this.outcomeCritHealing) == 0) return "0 %";
        double t1 = (double) this.outcomeCritHealing / (double) (this.outcomeHealing + this.outcomeCritHealing);
        return NumberFormat.getPercentInstance().format(t1);
    }

    public long getAbilityHealing() {
        return this.outcomeHealing + this.outcomeCritHealing;
    }

    public long getAbilityOverhealing() {
        return this.outcomeOverhealing;
    }

    public List<HPSStats> getSubcategoryList() {
        Collections.sort(this.subcategory, new HPSStatsComparator());
        return this.subcategory;
    }
}
