package net.sf.chellow.physical;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import net.sf.chellow.billing.DceService;
import net.sf.chellow.data08.HhDatumRaw;
import net.sf.chellow.monad.DeployerException;
import net.sf.chellow.monad.DesignerException;
import net.sf.chellow.monad.Hiber;
import net.sf.chellow.monad.Invocation;
import net.sf.chellow.monad.MonadUtils;
import net.sf.chellow.monad.ProgrammerException;
import net.sf.chellow.monad.Urlable;
import net.sf.chellow.monad.UserException;
import net.sf.chellow.monad.Invocation.HttpMethod;
import net.sf.chellow.monad.types.MonadBoolean;
import net.sf.chellow.monad.types.MonadDate;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Channel extends PersistentEntity implements Urlable {

    private static final long serialVersionUID = 1L;

    private static HttpMethod[] httpMethods = { HttpMethod.GET };

    public static void addHhData(DceService dceService, List<HhDatumRaw> dataRaw) throws ProgrammerException, UserException {
        Channel channel;
        HhDatumRaw firstDatum = dataRaw.get(0);
        try {
            channel = (Channel) Hiber.session().createQuery("select distinct channel from MpanCore mpanCore join mpanCore.supply.channels as channel where channel.isImport = :isImport and channel.isKwh = :isKwh and mpanCore.dso.code.string || mpanCore.uniquePart.string || mpanCore.checkDigit.character = :core").setBoolean("isImport", firstDatum.getIsImport().getBoolean()).setBoolean("isKwh", firstDatum.getIsKwh().getBoolean()).setString("core", firstDatum.getMpanCore().toStringNoSpaces()).uniqueResult();
        } catch (HibernateException e) {
            throw new ProgrammerException(e);
        }
        if (channel == null) {
            throw UserException.newInvalidParameter("The MPAN core " + firstDatum.getMpanCore() + " is not set up in Chellow.");
        }
        channel.addHhDataBlock(dceService, dataRaw);
    }

    private Supply supply;

    private boolean isImport;

    private boolean isKwh;

    public Channel() {
        setTypeName("channel");
    }

    public Channel(Supply supply, boolean isImport, boolean isKwh) {
        this.supply = supply;
        this.isImport = isImport;
        this.isKwh = isKwh;
    }

    public Supply getSupply() {
        return supply;
    }

    void setSupply(Supply supply) {
        this.supply = supply;
    }

    public boolean getIsImport() {
        return isImport;
    }

    void setIsImport(boolean isImport) {
        this.isImport = isImport;
    }

    public boolean getIsKwh() {
        return isKwh;
    }

    void setIsKwh(boolean isKwh) {
        this.isKwh = isKwh;
    }

    @SuppressWarnings("unchecked")
    private void addHhDataBlock(DceService dceService, List<HhDatumRaw> dataRaw) throws ProgrammerException, UserException {
        HhEndDate from = dataRaw.get(0).getEndDate();
        HhEndDate to = dataRaw.get(dataRaw.size() - 1).getEndDate();
        List<SupplyGeneration> supplyGenerations = supply.getGenerations(from, to);
        for (SupplyGeneration generation : supplyGenerations) {
            DceService actualDceService = generation.getDceService(isImport, isKwh);
            if (actualDceService == null) {
                addSnagChannel(dceService, SnagChannel.SNAG_DATA_IGNORED, from, to, false);
                return;
            }
            if (!dceService.equals(actualDceService)) {
                throw UserException.newInvalidParameter("Somewhere in the block of hh data between (" + dataRaw.get(0) + ") and (" + dataRaw.get(dataRaw.size() - 1) + ") and between the dates " + generation.getStartDate() + " and " + (generation.getFinishDate() == null ? "ongoing" : generation.getFinishDate()) + " there are one or more data with a contract that is not the contract under which the data is provided.");
            }
        }
        if (supply.getGeneration(from) == null) {
            addSnagChannel(dceService, SnagChannel.SNAG_DATA_IGNORED, from, to, false);
            return;
        }
        if (supply.getGeneration(to) == null) {
            addSnagChannel(dceService, SnagChannel.SNAG_DATA_IGNORED, from, to, false);
            return;
        }
        List<HhDatum> data = (List<HhDatum>) Hiber.session().createQuery("from HhDatum datum where datum.channel = :channel and " + "datum.endDate.date >= :startDate and datum.endDate.date <= :finishDate order by datum.endDate.date").setEntity("channel", this).setTimestamp("startDate", from.getDate()).setTimestamp("finishDate", to.getDate()).list();
        if (data.isEmpty()) {
            checkForMissingFromLatest(from.getPrevious());
        }
        HhEndDate siteCheckFrom = null;
        HhEndDate siteCheckTo = null;
        HhEndDate notActualFrom = null;
        HhEndDate notActualTo = null;
        HhEndDate resolveMissingFrom = null;
        HhEndDate resolveMissingTo = null;
        HhEndDate prevEndDate = null;
        int missing = 0;
        HhDatum originalDatum = null;
        for (int i = 0; i < dataRaw.size(); i++) {
            boolean added = false;
            boolean altered = false;
            HhDatumRaw datumRaw = dataRaw.get(i);
            HhDatum datum = null;
            if (i - missing < data.size()) {
                datum = data.get(i - missing);
                if (!datumRaw.getEndDate().equals(datum.getEndDate())) {
                    datum = null;
                }
            }
            if (datum == null) {
                Hiber.session().save(new HhDatum(this, datumRaw));
                added = true;
                missing++;
                if (resolveMissingFrom == null) {
                    resolveMissingFrom = datumRaw.getEndDate();
                }
                resolveMissingTo = datumRaw.getEndDate();
            } else if (datumRaw.getValue() != datum.getValue() || (datumRaw.getStatus() == null ? datum.getStatus() != null : !datumRaw.getStatus().equals(datum.getStatus()))) {
                originalDatum = datum;
                datum.update(datumRaw.getValue(), datumRaw.getStatus());
                altered = true;
            }
            if (added || altered) {
                if (siteCheckFrom == null) {
                    siteCheckFrom = datumRaw.getEndDate();
                }
                siteCheckTo = datumRaw.getEndDate();
                if (datumRaw.getValue() < 0) {
                    addSnagChannel(dceService == null ? getDceService(datumRaw.getEndDate()) : dceService, SnagChannel.SNAG_NEGATIVE, datumRaw.getEndDate(), datumRaw.getEndDate(), false);
                } else if (altered && originalDatum.getValue() < 0) {
                    resolveSnag(SnagChannel.SNAG_NEGATIVE, datumRaw.getEndDate());
                }
                if (!HhDatumStatus.ACTUAL.equals(datumRaw.getStatus())) {
                    if (notActualFrom == null) {
                        notActualFrom = datumRaw.getEndDate();
                    }
                    notActualTo = datumRaw.getEndDate();
                } else if (altered && !originalDatum.getStatus().equals(HhDatumStatus.ACTUAL)) {
                    resolveSnag(SnagChannel.SNAG_NOT_ACTUAL, datumRaw.getEndDate());
                }
            }
            if (siteCheckTo != null && siteCheckTo.equals(prevEndDate)) {
                siteCheck(siteCheckFrom, siteCheckTo, supplyGenerations);
                siteCheckFrom = null;
                siteCheckTo = null;
            }
            if (notActualTo != null && notActualTo.equals(prevEndDate)) {
                notActualSnag(notActualFrom, notActualTo, supplyGenerations);
                notActualFrom = null;
                notActualTo = null;
            }
            if (resolveMissingTo != null && resolveMissingTo.equals(prevEndDate)) {
                resolveSnag(SnagChannel.SNAG_MISSING, resolveMissingFrom, resolveMissingTo);
                resolveMissingFrom = null;
                resolveMissingTo = null;
            }
            prevEndDate = datumRaw.getEndDate();
        }
        if (siteCheckTo != null && siteCheckTo.equals(prevEndDate)) {
            siteCheck(siteCheckFrom, siteCheckTo, supplyGenerations);
        }
        if (notActualTo != null && notActualTo.equals(prevEndDate)) {
            notActualSnag(notActualFrom, notActualTo, supplyGenerations);
        }
        if (resolveMissingTo != null && resolveMissingTo.equals(prevEndDate)) {
            resolveSnag(SnagChannel.SNAG_MISSING, resolveMissingFrom, resolveMissingTo);
        }
    }

    private void siteCheck(HhEndDate from, HhEndDate to, List<SupplyGeneration> supplyGenerations) throws ProgrammerException, UserException {
        if (isKwh) {
            for (SupplyGeneration generation : supplyGenerations) {
                Site site = generation.getSiteSupplyGenerations().iterator().next().getSite();
                HhEndDate checkFrom = from.getDate().after(generation.getStartDate().getDate()) ? from : generation.getStartDate();
                HhEndDate checkTo = generation.getFinishDate() == null || to.getDate().before(generation.getFinishDate().getDate()) ? to : generation.getFinishDate();
                site.hhCheck(checkFrom, checkTo);
            }
        }
    }

    private void notActualSnag(HhEndDate from, HhEndDate to, List<SupplyGeneration> supplyGenerations) throws ProgrammerException, UserException {
        for (SupplyGeneration generation : supplyGenerations) {
            HhEndDate checkFrom = from.getDate().after(generation.getStartDate().getDate()) ? from : generation.getStartDate();
            HhEndDate checkTo = generation.getFinishDate() == null || to.getDate().before(generation.getFinishDate().getDate()) ? to : generation.getFinishDate();
            addSnagChannel(generation.getDceService(isImport, isKwh), SnagChannel.SNAG_NOT_ACTUAL, checkFrom, checkTo, false);
        }
    }

    private void addSnagChannel(DceService dceService, String description, HhEndDate startDate, HhEndDate finishDate, boolean isResolved) throws ProgrammerException, UserException {
        SnagDateBounded.addSnagChannel(dceService, this, description, startDate, finishDate, isResolved);
    }

    private void resolveSnag(String description, HhEndDate date) throws ProgrammerException, UserException {
        resolveSnag(description, date, date);
    }

    @SuppressWarnings("unchecked")
    private void resolveSnag(String description, HhEndDate startDate, HhEndDate finishDate) throws ProgrammerException, UserException {
        for (SnagChannel snag : (List<SnagChannel>) Hiber.session().createQuery("from SnagChannel snag where snag.channel = :channel and snag.description = :description and snag.startDate.date <= :finishDate and snag.finishDate.date >= :startDate and snag.dateResolved is null").setEntity("channel", this).setString("description", description.toString()).setTimestamp("startDate", startDate.getDate()).setTimestamp("finishDate", finishDate.getDate()).list()) {
            addSnagChannel(snag.getService(), description, snag.getStartDate().getDate().before(startDate.getDate()) ? startDate : snag.getStartDate(), snag.getFinishDate().getDate().after(finishDate.getDate()) ? finishDate : snag.getFinishDate(), true);
        }
    }

    private HhEndDate getCheckToDate() throws ProgrammerException, UserException {
        HhEndDate lastSnagDate = (HhEndDate) Hiber.session().createQuery("select snag.finishDate from SnagChannel snag where snag.channel = :channel and snag.description = :snagDescription and snag.dateResolved is null order by snag.finishDate.date desc").setEntity("channel", this).setString("snagDescription", SnagChannel.SNAG_MISSING).setMaxResults(1).uniqueResult();
        HhEndDate finish = null;
        SupplyGeneration generation = supply.getGenerationLast();
        if (generation.getFinishDate() == null) {
            DceService latestDceService = generation.getDceService(isImport, isKwh);
            if (latestDceService == null) {
                finish = HhEndDate.roundDown(new Date());
            } else {
                finish = HhEndDate.roundDown(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * latestDceService.getLag()));
                Calendar cal = MonadDate.getCalendar();
                cal.clear();
                cal.setTime(finish.getDate());
                cal.set(Calendar.MILLISECOND, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                if (latestDceService.getFrequency().equals(ContractFrequency.DAILY)) {
                    finish = new HhEndDate(cal.getTime());
                } else if (latestDceService.getFrequency().equals(ContractFrequency.MONTHLY)) {
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    finish = new HhEndDate(cal.getTime());
                } else {
                    throw new ProgrammerException("Frequency not recognized.");
                }
            }
        } else {
            finish = generation.getFinishDate();
        }
        if (lastSnagDate != null) {
            finish = finish.getDate().after(lastSnagDate.getDate()) ? finish : lastSnagDate;
        }
        return finish;
    }

    public void checkForMissingFromLatest() throws ProgrammerException, UserException {
        checkForMissingFromLatest(null);
    }

    public void checkForMissingFromLatest(HhEndDate to) throws ProgrammerException, UserException {
        Date latestDatumDate = (Date) Hiber.session().createQuery("select max(datum.endDate.date) from HhDatum datum where datum.channel = :channel").setEntity("channel", this).uniqueResult();
        Date latestMissingDate = (Date) Hiber.session().createQuery("select max(snag.finishDate.date) from SnagChannel snag where snag.channel = :channel and snag.description = :description").setEntity("channel", this).setString("description", SnagChannel.SNAG_MISSING).uniqueResult();
        HhEndDate latestPresentDate;
        if (latestDatumDate == null) {
            if (latestMissingDate == null) {
                latestPresentDate = supply.getGenerationFirst().getStartDate();
            } else {
                latestPresentDate = new HhEndDate(latestMissingDate);
            }
        } else {
            if (latestMissingDate == null) {
                latestPresentDate = new HhEndDate(latestDatumDate);
            } else {
                latestPresentDate = new HhEndDate(latestMissingDate).getDate().after(new HhEndDate(latestDatumDate).getDate()) ? new HhEndDate(latestMissingDate) : new HhEndDate(latestDatumDate);
            }
        }
        checkForMissing(latestPresentDate.getNext(), to);
    }

    @SuppressWarnings("unchecked")
    public void checkForMissing(HhEndDate from, HhEndDate to) throws ProgrammerException, UserException {
        if (from == null) {
            from = supply.getGenerationFirst().getStartDate();
        }
        if (from.getDate().before(supply.getGenerationFirst().getStartDate().getDate())) {
            resolveSnag(SnagChannel.SNAG_MISSING, from, supply.getGenerationFirst().getStartDate().getPrevious());
            from = supply.getGenerationFirst().getStartDate();
        }
        if (to == null) {
            to = getCheckToDate();
        }
        Calendar cal = MonadDate.getCalendar();
        HhEndDate lastGenerationDate = supply.getGenerationLast().getFinishDate();
        if (lastGenerationDate != null && to.getDate().after(lastGenerationDate.getDate())) {
            resolveSnag(SnagChannel.SNAG_MISSING, lastGenerationDate.getNext(), to);
            to = lastGenerationDate;
        }
        if (from.getDate().after(to.getDate())) {
            return;
        }
        Query query = Hiber.session().createQuery("select count(*) from HhDatum datum where datum.channel = :channel and datum.endDate.date >= :startDate and datum.endDate.date <= :finishDate").setEntity("channel", this);
        List<SupplyGeneration> generations = supply.getGenerations(from, to);
        for (int i = 0; i < generations.size(); i++) {
            DceService contractDce = generations.get(i).getDceService(isImport, isKwh);
            HhEndDate generationStartDate = i == 0 ? from : generations.get(i).getStartDate();
            HhEndDate generationFinishDate = i == generations.size() - 1 ? to : generations.get(i).getFinishDate();
            if (contractDce == null) {
                resolveSnag(SnagChannel.SNAG_MISSING, generationStartDate, generationFinishDate);
            } else {
                HhEndDate spanStartDate = generationStartDate;
                HhEndDate spanFinishDate = generationFinishDate;
                boolean finished = false;
                while (!finished) {
                    long present = (Long) query.setTimestamp("startDate", spanStartDate.getDate()).setTimestamp("finishDate", spanFinishDate.getDate()).uniqueResult();
                    if (present == 0) {
                        addSnagChannel(contractDce, SnagChannel.SNAG_MISSING, spanStartDate, spanFinishDate, false);
                        spanStartDate = HhEndDate.getNext(spanFinishDate);
                        spanFinishDate = generationFinishDate;
                        if (spanStartDate.getDate().after(spanFinishDate.getDate())) {
                            finished = true;
                        }
                    } else {
                        long shouldBe = (long) (spanFinishDate.getDate().getTime() - spanStartDate.getDate().getTime() + 1000 * 60 * 30) / (1000 * 60 * 30);
                        if (present == shouldBe) {
                            spanStartDate = HhEndDate.getNext(spanFinishDate);
                            spanFinishDate = generationFinishDate;
                            if (spanStartDate.getDate().after(spanFinishDate.getDate())) {
                                finished = true;
                            }
                        } else {
                            spanFinishDate = new HhEndDate(new Date(HhEndDate.roundDown(cal, spanStartDate.getDate().getTime() + (spanFinishDate.getDate().getTime() - spanStartDate.getDate().getTime()) / 2)));
                        }
                    }
                }
            }
        }
    }

    private DceService getDceService(HhEndDate date) {
        return supply.getGeneration(date).getDceService(isImport, isKwh);
    }

    public void deleteData(HhEndDate from, int days) throws ProgrammerException, UserException {
        Calendar cal = MonadDate.getCalendar();
        cal.setTime(from.getDate());
        cal.add(Calendar.DAY_OF_MONTH, days);
        HhEndDate to = new HhEndDate(cal.getTime()).getPrevious();
        long numDeleted = Hiber.session().createQuery("delete from HhDatum datum where datum.channel = :channel and datum.endDate.date >= :from and datum.endDate.date <= :to").setEntity("channel", this).setTimestamp("from", from.getDate()).setTimestamp("to", to.getDate()).executeUpdate();
        if (numDeleted == 0) {
            throw UserException.newInvalidParameter("There aren't any data to delete for this period.");
        }
        checkForMissing(from, to);
        this.siteCheck(from, to, supply.getGenerations(from, to));
    }

    @SuppressWarnings("unchecked")
    public List<HhDatum> getHhData(HhEndDate from, HhEndDate to) {
        return (List<HhDatum>) Hiber.session().createQuery("from HhDatum datum where datum.channel = :channel and datum.endDate.date >= :from and datum.endDate.date <= :to order by datum.endDate.date").setEntity("channel", this).setTimestamp("from", from.getDate()).setTimestamp("to", to.getDate()).list();
    }

    public Element toXML(Document doc) throws ProgrammerException, UserException {
        Element element = (Element) super.toXML(doc);
        element.setAttributeNode(MonadBoolean.toXml(doc, "is-import", isImport));
        element.setAttributeNode(MonadBoolean.toXml(doc, "is-kwh", isKwh));
        return element;
    }

    public void httpGet(Invocation inv) throws DesignerException, ProgrammerException, UserException, DeployerException {
        Document doc = MonadUtils.newSourceDocument();
        Element source = doc.getDocumentElement();
        Element channelElement = toXML(doc);
        source.appendChild(channelElement);
        Element supplyElement = getSupply().toXML(doc);
        supplyElement.appendChild(supply.getOrganization().toXML(doc));
        channelElement.appendChild(supplyElement);
        supplyElement.appendChild(getSupply().getOrganization().toXML(doc));
        inv.sendOk(doc);
    }

    public HhData getHhDataInstance() {
        return new HhData(this);
    }

    public void httpPost(Invocation inv) throws ProgrammerException, UserException {
    }

    public Urlable getChild(UriPathElement uriId) throws UserException, ProgrammerException {
        Urlable child = null;
        if (HhData.URI_ID.equals(uriId)) {
            child = new HhData(this);
        }
        return child;
    }

    public void httpDelete(Invocation inv) throws ProgrammerException, UserException {
        inv.sendMethodNotAllowed(httpMethods);
    }

    public MonadUri getUri() throws ProgrammerException, UserException {
        return supply.getChannelsInstance().getUri().resolve(getUriId());
    }

    public String toString() {
        return "Channel id: " + getId() + "is import: " + getIsImport() + " is kWh: " + getIsKwh();
    }
}
