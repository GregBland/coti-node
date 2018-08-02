package io.coti.common.crypto;

import io.coti.common.data.BaseTransactionData;
import io.coti.common.data.Hash;
import io.coti.common.data.TransactionData;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Vector;

public class TransactionCryptoWrapper {
    final static int baseTransactionHashSize = 32;
    ArrayList<BaseTransactionCryptoWrapper> baseTransactions = new ArrayList<>();
    TransactionData txData;

    public TransactionCryptoWrapper(TransactionData txData) {
        this.txData = txData;
        for (BaseTransactionData bxData : txData.getBaseTransactions()) {
            baseTransactions.add(new BaseTransactionCryptoWrapper(bxData));
        }
    }


    public TransactionCryptoWrapper(TransactionData txData, Vector<BaseTransactionCryptoWrapper> baseTransactionsWrapper) {
        this.txData = txData;
        this.baseTransactions = new ArrayList<>(baseTransactionsWrapper);

    }


    private byte[] getBaseTransactionsHashesBytes() {
        ByteBuffer baseTransactionHashBuffer = ByteBuffer.allocate(baseTransactions.size() * baseTransactionHashSize);
        for (BaseTransactionCryptoWrapper bxCrypto : this.baseTransactions) {
            byte[] baseTransactionHashBytes = bxCrypto.getBaseTransactionHash().getBytes();
            baseTransactionHashBuffer = baseTransactionHashBuffer.put(baseTransactionHashBytes);
        }
        return baseTransactionHashBuffer.array();
    }

    public String getHashFromBaseTransactionHashesData() {
        Keccak.Digest256 digest = new Keccak.Digest256();
        byte[] bytesToHash = getBaseTransactionsHashesBytes();
        digest.update(bytesToHash);
        byte[] digestedHash = digest.digest();
        String hash = Hex.toHexString(digestedHash);
        return hash;
    }


    public void setTransactionHash() {


        for (BaseTransactionCryptoWrapper bxDataCrypto : this.baseTransactions) {
            if (bxDataCrypto.getBaseTransactionHash() == null)
                bxDataCrypto.setBaseTransactionHash();

        }
        String transactionHash = getHashFromBaseTransactionHashesData();
        txData.setHash(new Hash(transactionHash));


    }

    private boolean IsTransactionHashCorrect() {

        String generatedTxHashFromBaseTransactions = getHashFromBaseTransactionHashesData();
        String txHashFromData = this.txData.getHash().toHexString();

        return generatedTxHashFromBaseTransactions.equalsIgnoreCase(txHashFromData);
    }

    public boolean isTransactionValid() {

        if (!this.IsTransactionHashCorrect())
            return false;
        for (BaseTransactionCryptoWrapper bxCrypto : this.baseTransactions) {
            if (bxCrypto.IsBaseTransactionValid(txData.getHash()) == false)
                return false;
        }
        return true;
    }
}
