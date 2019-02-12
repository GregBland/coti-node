package io.coti.zerospend.services;

import io.coti.basenode.communication.interfaces.IPropagationPublisher;
import io.coti.basenode.crypto.ClusterStampStateCrypto;
import io.coti.basenode.data.*;
import io.coti.basenode.model.ClusterStamp;
import io.coti.basenode.model.Transactions;
import io.coti.basenode.services.BaseNodeClusterStampService;
import io.coti.basenode.services.TccConfirmationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ClusterStampService extends BaseNodeClusterStampService {

    final private static int DSP_NODES_MAJORITY = 1;

    @Autowired
    private IPropagationPublisher propagationPublisher;
    @Autowired
    private DspVoteService dspVoteService;
    @Autowired
    private SourceStarvationService sourceStarvationService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private TccConfirmationService tccConfirmationService;
    @Autowired
    private Transactions transactions;
    @Autowired
    private ClusterStampStateCrypto clusterStampStateCrypto;

    @Value("${clusterstamp.reply.timeout}")
    private int replyTimeOut;

    private ClusterStampData currentClusterStamp;

    private boolean clusterStampInProgress;



    @PostConstruct
    private void init() {
        clusterStampInProgress = false;
        currentClusterStamp = new ClusterStampData(new Hash("inProgress"));
    }

    @Override
    public void prepareForClusterStamp(ClusterStampPreparationData clusterStampPreparationData) {
        log.debug("Start preparation of ZS for cluster stamp");
        propagationPublisher.propagate(clusterStampPreparationData, Arrays.asList(NodeType.DspNode, NodeType.TrustScoreNode, NodeType.FinancialServer));
        CompletableFuture.runAsync(this::initTimer);
    }

    private void initTimer(){
        try {
            Thread.sleep(replyTimeOut);
            clusterStampInProgress = true;
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void handleDspNodeReadyForClusterStampMessage(DspReadyForClusterStampData dspReadyForClusterStampData) {

        log.debug("\'Ready for cluster stamp\' propagated message received from DSP to ZS");
        if(!clusterStampInProgress && clusterStampStateCrypto.verifySignature(dspReadyForClusterStampData)) {
            currentClusterStamp.getDspReadyForClusterStampDataList().add(dspReadyForClusterStampData);

            if ( currentClusterStamp.getDspReadyForClusterStampDataList().size() >= DSP_NODES_MAJORITY ) {
                log.info("Stop dsp vote service from sum and save dsp votes");
                dspVoteService.stopSumAndSaveVotes();
                sourceStarvationService.stopCheckSourcesStarvation();
                makeAndPropagateClusterStamp();
            }
        }

    }

    public void makeAndPropagateClusterStamp() {

        ClusterStampData clusterStampData = currentClusterStamp;
        clusterStampData.setBalanceMap(balanceService.getBalanceMap());
        clusterStampData.setUnconfirmedTransactions(getUnconfirmedTransactions());
        clusterStampData.setHash();
        propagationPublisher.propagate(clusterStampData, Arrays.asList(NodeType.DspNode));

        currentClusterStamp = new ClusterStampData(new Hash("inProgress"));
        log.info("Restart DSP vote service to sum and save dsp votes, and starvation service");
        //dspVoteService.startSumAndSaveVotes();
        //sourceStarvationService.startCheckSourcesStarvation();
    }

    @Override
    public boolean isClusterStampInProgress() {
        return clusterStampInProgress;
    }

    private List<TransactionData> getUnconfirmedTransactions() {

        Set unreachedDspcHashTransactions = dspVoteService.getTransactionHashToVotesListMapping().keySet();
        Set unreachedTccHashTransactions = tccConfirmationService.getHashToTccUnConfirmTransactionsMapping().keySet();

        List<Hash> unconfirmedHashTransactions = new ArrayList<>();
        unconfirmedHashTransactions.addAll(unreachedDspcHashTransactions);
        unconfirmedHashTransactions.addAll(unreachedTccHashTransactions);

        List<TransactionData> unconfirmedTransactions = new ArrayList<>();
        for(Hash unconfirmedHashTransaction : unconfirmedHashTransactions) {
            unconfirmedTransactions.add(transactions.getByHash(unconfirmedHashTransaction));
        }

        return unconfirmedTransactions;
    }
}