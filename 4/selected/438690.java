package maze.commons.examples.auction.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import maze.common.adv_io.basic.BasicReader;
import maze.common.adv_io.basic.BasicWriter;
import maze.commons.examples.auction.common.basics.LotType;
import maze.commons.examples.auction.common.basics.beans.AuctionClientBean;
import maze.commons.examples.auction.common.bidding.ActualAuctionBiddingInfo;
import maze.commons.examples.auction.common.bidding.BiddingResult;
import maze.commons.examples.auction.common.cli.AuctionCommonCli;
import maze.commons.examples.auction.common.cli.impl.AuctionCommonCliFactoryImpl;
import maze.commons.examples.auction.common.com.basic.beans.FinishComBean;
import maze.commons.examples.auction.common.com.bidding.beans.BidComBean;
import maze.commons.examples.auction.common.com.bidding.beans.CheckClientNameComBean;
import maze.commons.examples.auction.common.com.bidding.beans.RegNewClientComBean;
import maze.commons.examples.auction.common.com.monitor.RequestLotCountCom;
import maze.commons.examples.auction.common.com.monitor.beans.ReadLotsComBean;
import maze.commons.examples.auction.common.com.monitor.beans.RequestLotCountComBean;
import maze.commons.examples.auction.common.server.ServerSideError;
import maze.commons.examples.auction.common.ui.BasicOutput;
import maze.commons.examples.auction.common.ui.BeanCompletedAsyncCaller;
import maze.commons.examples.auction.common.ui.FieldCompletedAsyncCaller;
import maze.commons.examples.auction.client.con.BasicConnection;
import maze.commons.examples.auction.client.con.impl.BasicConnectionImpl;
import maze.commons.examples.auction.client.ui.interaction.BiddingInteraction;
import maze.commons.examples.auction.client.ui.interaction.ClientRegistrationInteraction;
import maze.commons.examples.auction.client.ui.manager.UiInteractionManager;
import maze.commons.examples.auction.client.ui.manager.impl.UiInteractionManagerFactoryImpl;
import maze.commons.generic.ErrorResult;
import org.apache.commons.lang.StringUtils;

/**
 * @author Normunds Mazurs (MAZE)
 * 
 */
public class AuctionClient {

    private static final Logger logger = Logger.getLogger(AuctionClient.class.getName());

    public static void main(final String[] args) throws Exception {
        final AuctionCommonCli auctionCommonCli = AuctionCommonCliFactoryImpl.INSTANCE.create(args);
        if (auctionCommonCli.hasHelpOpt()) {
            AuctionCommonCliFactoryImpl.INSTANCE.printHelp("AuctionClient");
            return;
        }
        final UiInteractionManager interactionManager = UiInteractionManagerFactoryImpl.getInstance().createTextInteractionManager();
        com(auctionCommonCli, interactionManager);
    }

    public static void handlePossibleErrors(final Object recvBean) {
        if (recvBean instanceof ErrorResult) {
            final ErrorResult errorResult = (ErrorResult) recvBean;
            if (ErrorResult.Unexpected == errorResult) {
                throw new RuntimeException("Unexpected unknown server-side error!");
            } else {
                assert false;
                throw new RuntimeException("Unknown server-side error!");
            }
        }
        if (recvBean instanceof ServerSideError) {
            final ServerSideError errorResult = (ServerSideError) recvBean;
            if (ServerSideError.ConnectedUserLimitExceeded == errorResult) {
                throw new RuntimeException("Server side: Simultaneously connected user limit exceeded!");
            } else {
                assert false;
                throw new RuntimeException("Unknown server-side error!");
            }
        }
    }

    public static boolean regNewClient(final BasicConnection basicConnection, final maze.commons.examples.auction.common.basics.AuctionClient auctionClient) {
        final BasicWriter w = basicConnection.getBasicWriter();
        final BasicReader r = basicConnection.getBasicReader();
        final RegNewClientComBean regNewClientComBean = new RegNewClientComBean();
        regNewClientComBean.setAuctionClient(auctionClient);
        w.writeBean(regNewClientComBean);
        w.flush();
        final Object recvBean = r.readFixBean();
        handlePossibleErrors(recvBean);
        return (Boolean) recvBean;
    }

    public static Object recvCount(final BasicConnection basicConnection, final AuctionClientBean acBean) {
        final BasicWriter w = basicConnection.getBasicWriter();
        final BasicReader r = basicConnection.getBasicReader();
        final Set<LotType> acLotTypes = acBean.getLotTypes();
        final RequestLotCountCom requestLotCountCom = RequestLotCountComBean.create(acLotTypes);
        w.writeBean(requestLotCountCom);
        w.flush();
        final Object recvBean = r.readFixBean();
        handlePossibleErrors(recvBean);
        return recvBean;
    }

    @SuppressWarnings("unchecked")
    public static List<ActualAuctionBiddingInfo> recvLots(final BasicConnection basicConnection, final int fromVal, final int toVal) {
        final BasicWriter w = basicConnection.getBasicWriter();
        final BasicReader r = basicConnection.getBasicReader();
        final ReadLotsComBean readLotsComBean = new ReadLotsComBean();
        readLotsComBean.setFromVal(fromVal);
        readLotsComBean.setToVal(toVal);
        w.writeBean(readLotsComBean);
        w.flush();
        final Object recvBean = r.readFixBean();
        handlePossibleErrors(recvBean);
        return (List<ActualAuctionBiddingInfo>) recvBean;
    }

    public static BiddingResult makeABid(final BasicConnection basicConnection, final int lotUniqNum, final int price) {
        final BasicWriter w = basicConnection.getBasicWriter();
        final BasicReader r = basicConnection.getBasicReader();
        final BidComBean bean = new BidComBean();
        bean.setLotUniqNum(lotUniqNum);
        bean.setPrice(BigDecimal.valueOf(price));
        w.writeBean(bean);
        w.flush();
        final Object recvBean = r.readFixBean();
        handlePossibleErrors(recvBean);
        return (BiddingResult) recvBean;
    }

    public static void com(final AuctionCommonCli auctionCommonCli, final UiInteractionManager interactionManager) throws Exception {
        final ClientRegistrationInteraction clientRegistrationInteraction = interactionManager.createClientRegistration();
        final int serverBidderListenPort = auctionCommonCli.getServerBidderListenPort();
        final String serverAddr = auctionCommonCli.getServerAddr();
        logger.info("serverBidderListenPort: " + serverBidderListenPort);
        logger.info("serverAddr: " + serverAddr);
        final BasicConnection basicConnection = BasicConnectionImpl.create(serverAddr, serverBidderListenPort);
        try {
            final BasicWriter w = basicConnection.getBasicWriter();
            final BasicReader r = basicConnection.getBasicReader();
            final FieldCompletedAsyncCaller usernameFieldCompletedCaller = new FieldCompletedAsyncCaller() {

                @Override
                public boolean fieldCompletedCall(final BasicOutput basicOutput, final String value) {
                    basicOutput.outputInfoMessage("Checking name...");
                    if (StringUtils.isBlank(value)) {
                        basicOutput.outputErrorMessage("Username should not be blank!");
                        return false;
                    }
                    if (value.length() > 255) {
                        basicOutput.outputErrorMessage("Username should not be longer than 255 chars!");
                        return false;
                    }
                    final CheckClientNameComBean checkClientNameComBean = new CheckClientNameComBean();
                    checkClientNameComBean.setUsername(value);
                    w.writeBean(checkClientNameComBean);
                    w.flush();
                    final Object recvBean = r.readFixBean();
                    handlePossibleErrors(recvBean);
                    final boolean r = (Boolean) recvBean;
                    if (r) {
                        basicOutput.outputInfoMessage("Name is ok.");
                        return true;
                    }
                    basicOutput.outputErrorMessage("Name is already taken!");
                    return false;
                }
            };
            final BeanCompletedAsyncCaller<AuctionClientBean> beanCompletedCaller = new BeanCompletedAsyncCaller<AuctionClientBean>() {

                @Override
                public boolean beanCompletedCall(final BasicOutput basicOutput, final AuctionClientBean bean) {
                    if (regNewClient(basicConnection, bean)) {
                        basicOutput.outputInfoMessage("Registration is ok.");
                        return true;
                    }
                    basicOutput.outputErrorMessage("Username is already taken!");
                    return false;
                }
            };
            final AuctionClientBean acBean = clientRegistrationInteraction.completeClientRegistration(usernameFieldCompletedCaller, beanCompletedCaller);
            assert acBean != null;
            final BiddingInteraction biddingInteraction = interactionManager.createBidding();
            final FieldCompletedAsyncCaller commandCaller = new FieldCompletedAsyncCaller() {

                boolean countCmd(final BasicOutput basicOutput) {
                    final Object recvBean = recvCount(basicConnection, acBean);
                    basicOutput.outputInfoMessage("Count of active auction lots: " + recvBean);
                    return true;
                }

                boolean readLots(final BasicOutput basicOutput, final int fromVal, final int toVal) {
                    final List<ActualAuctionBiddingInfo> infoList = recvLots(basicConnection, fromVal, toVal);
                    int c = fromVal;
                    for (final ActualAuctionBiddingInfo info : infoList) {
                        basicOutput.outputInfoMessage(c + ". info: " + info);
                        c++;
                    }
                    return true;
                }

                boolean readLots(final BasicOutput basicOutput, final String param) {
                    if (StringUtils.isBlank(param)) {
                        return false;
                    }
                    final String[] p = StringUtils.split(param, '-');
                    if (p == null || p.length != 2) {
                        return false;
                    }
                    if (!StringUtils.isNumeric(p[0]) || !StringUtils.isNumeric(p[1])) {
                        return false;
                    }
                    final int fromVal = Integer.parseInt(p[0]);
                    final int toVal = Integer.parseInt(p[1]);
                    return readLots(basicOutput, fromVal, toVal);
                }

                boolean readLot(final BasicOutput basicOutput, final String param) {
                    if (StringUtils.isBlank(param)) {
                        return false;
                    }
                    if (!StringUtils.isNumeric(param)) {
                        return false;
                    }
                    final int val = Integer.parseInt(param);
                    return readLots(basicOutput, val, val);
                }

                boolean bidLot(final BasicOutput basicOutput, final String param) {
                    if (StringUtils.isBlank(param)) {
                        return false;
                    }
                    final String[] p = StringUtils.split(param, ':');
                    if (p == null || p.length != 2) {
                        return false;
                    }
                    if (!StringUtils.isNumeric(p[0]) || !StringUtils.isNumeric(p[1])) {
                        return false;
                    }
                    final int lotUniqNum = Integer.parseInt(p[0]);
                    final int price = Integer.parseInt(p[1]);
                    final BiddingResult biddingResult = makeABid(basicConnection, lotUniqNum, price);
                    if (BiddingResult.BidTooSoon == biddingResult) {
                        basicOutput.outputErrorMessage("Repeated bid have been made too soon!");
                    } else if (BiddingResult.LotNotExistent == biddingResult) {
                        basicOutput.outputErrorMessage("Lot with this uniq.num. does not exist or is no more active or out of lot types scope!");
                    } else if (BiddingResult.NotEnoughMoney == biddingResult) {
                        basicOutput.outputErrorMessage("You have not enough money!");
                    } else if (BiddingResult.PriceTooLow == biddingResult) {
                        basicOutput.outputErrorMessage("Bid is too low!");
                    } else if (BiddingResult.SamePrice == biddingResult) {
                        basicOutput.outputErrorMessage("Bid is same as highest!");
                    } else if (BiddingResult.Ok == biddingResult) {
                        basicOutput.outputInfoMessage("Bid is OK");
                    } else {
                        return false;
                    }
                    return true;
                }

                void outputUnknownCommand(final BasicOutput basicOutput) {
                    basicOutput.outputErrorMessage("Unknown command!");
                }

                @Override
                public boolean fieldCompletedCall(final BasicOutput basicOutput, final String cmd) {
                    if (StringUtils.isBlank(cmd)) {
                        outputUnknownCommand(basicOutput);
                        return false;
                    }
                    if (cmd.startsWith("e")) {
                        basicOutput.outputInfoMessage("exit");
                        return true;
                    }
                    if (cmd.startsWith("c")) {
                        if (!countCmd(basicOutput)) {
                            outputUnknownCommand(basicOutput);
                        }
                    } else if (cmd.startsWith("l")) {
                        if (!readLots(basicOutput, cmd.substring(1))) {
                            outputUnknownCommand(basicOutput);
                        }
                    } else if (cmd.startsWith("r")) {
                        if (!readLot(basicOutput, cmd.substring(1))) {
                            outputUnknownCommand(basicOutput);
                        }
                    } else if (cmd.startsWith("b")) {
                        if (!bidLot(basicOutput, cmd.substring(1))) {
                            outputUnknownCommand(basicOutput);
                        }
                    }
                    return false;
                }
            };
            biddingInteraction.makeBid(commandCaller);
            w.writeBean(FinishComBean.INSTANCE);
        } finally {
            basicConnection.close();
        }
    }
}
