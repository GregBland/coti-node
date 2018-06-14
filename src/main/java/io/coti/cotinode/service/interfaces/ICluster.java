package io.coti.cotinode.service.interfaces;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.coti.cotinode.data.Hash;
import io.coti.cotinode.model.Transaction;
import io.coti.cotinode.storage.Interfaces.IPersistenceProvider;
import org.springframework.stereotype.Component;

@Component
public interface ICluster {
    void initCluster(List<Transaction> allClusterTransactions);
    void addToHashToAllClusterTransactionsMap(Transaction transaction);
    void addToUnconfirmedTransactionMap(Transaction transaction);
    void addToTrustScoreToSourceListMap(Transaction transaction);
    boolean addNewTransaction(Transaction transaction);
    void updateParentsTotalSumScore(Transaction transaction, int sonsTotalTrustScore, List<Hash> trustChainTransactionHashes);
    void attachToSource(Transaction newTransaction, Transaction source);
    List<Transaction> getAllSourceTransactions();
    void deleteTransactionFromHashToAllClusterTransactionsMapping(Hash hash);
    void deleteTransactionFromHashToToUnconfirmedTransactionsMapping(Hash hash);
    void deleteTrustScoreToSourceListMapping(Hash hash, Transaction transaction );
}
