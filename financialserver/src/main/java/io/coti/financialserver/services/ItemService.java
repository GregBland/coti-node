package io.coti.financialserver.services;

import io.coti.basenode.http.Response;
import io.coti.financialserver.data.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.coti.financialserver.crypto.ItemCrypto;
import io.coti.financialserver.crypto.ItemVoteCrypto;
import io.coti.financialserver.http.ItemRequest;
import io.coti.financialserver.http.VoteRequest;
import io.coti.financialserver.model.Disputes;

import java.util.ArrayList;
import java.util.List;

import static io.coti.financialserver.http.HttpStringConstants.*;

@Slf4j
@Service
public class ItemService {

    @Autowired
    Disputes disputes;

    @Autowired
    DisputeService disputeService;

    public ResponseEntity itemNew(ItemRequest request) {

        DisputeItemData disputeItemData = request.getDisputeItemData();
        ItemCrypto itemCrypto = new ItemCrypto();
        itemCrypto.signMessage(disputeItemData);

        if ( !itemCrypto.verifySignature(disputeItemData) ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(UNAUTHORIZED, STATUS_ERROR));
        }

        DisputeData disputeData = disputes.getByHash(disputeItemData.getDisputeHash());

        if (disputeData == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(DISPUTE_NOT_FOUND, STATUS_ERROR));
        }

        if( !disputeData.getConsumerHash().equals(disputeItemData.getUserHash()) ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(UNAUTHORIZED, STATUS_ERROR));
        }

        List<DisputeItemData> disputeItems = new ArrayList<>();
        disputeItems.add(disputeItemData);
        if ( !disputeService.isDisputeItemsValid(disputeData.getConsumerHash(), disputeItems, disputeData.getTransactionHash()) ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(DISPUTE_ITEMS_EXIST_ALREADY, STATUS_ERROR));
        }

        disputeData.getDisputeItems().add(disputeItemData);
        disputeService.update(disputeData);

        return ResponseEntity.status(HttpStatus.OK).body(new Response(SUCCESS, STATUS_SUCCESS));
    }

    public ResponseEntity itemUpdate(ItemRequest request) {

        DisputeItemData disputeItemDataNew = request.getDisputeItemData();
        ItemCrypto itemCrypto = new ItemCrypto();
        itemCrypto.signMessage(disputeItemDataNew);

        if ( !itemCrypto.verifySignature(disputeItemDataNew) ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(UNAUTHORIZED, STATUS_ERROR));
        }

        DisputeData disputeData = disputes.getByHash(disputeItemDataNew.getDisputeHash());

        if (disputeData == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(DISPUTE_NOT_FOUND, STATUS_ERROR));
        }

        DisputeItemData disputeItemData = disputeData.getDisputeItem(disputeItemDataNew.getId());

        if (disputeItemData == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(ITEM_NOT_FOUND, STATUS_ERROR));
        }

        if (disputeItemData.getStatus() != DisputeItemStatus.Recall) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(DISPUTE_ITEM_PASSED_RECALL_STATUS, STATUS_ERROR));
        }

        ActionSide actionSide;
        if(disputeData.getConsumerHash().equals(disputeItemDataNew.getUserHash())) {
            actionSide = ActionSide.Consumer;
        }
        else if(disputeData.getMerchantHash().equals(disputeItemDataNew.getUserHash())) {
            actionSide = ActionSide.Merchant;
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(UNAUTHORIZED, STATUS_ERROR));
        }
        actionSide = ActionSide.Merchant; // TODO: remove this line

        if(actionSide == ActionSide.Consumer && disputeItemDataNew.getStatus() == DisputeItemStatus.CanceledByConsumer) {
            disputeData.getDisputeItem(disputeItemDataNew.getId()).setStatus(disputeItemDataNew.getStatus());
        }

        if(actionSide == ActionSide.Consumer && disputeItemDataNew.getReason() != null) {
            disputeData.getDisputeItem(disputeItemDataNew.getId()).setReason(disputeItemDataNew.getReason());
        }

        if(actionSide == ActionSide.Merchant && (disputeItemDataNew.getStatus() == DisputeItemStatus.AcceptedByMerchant ||
                                                 disputeItemDataNew.getStatus() == DisputeItemStatus.RejectedByMerchant)) {
            disputeItemData.setStatus(disputeItemDataNew.getStatus());
        }

        disputeService.update(disputeData);

        return ResponseEntity.status(HttpStatus.OK).body(new Response(SUCCESS, STATUS_SUCCESS));
    }

    public ResponseEntity vote(VoteRequest request) {

        DisputeItemVoteData disputeItemVoteData = request.getDisputeItemVoteData();
        ItemVoteCrypto itemVoteCrypto = new ItemVoteCrypto();
        itemVoteCrypto.signMessage(disputeItemVoteData);

        if ( !itemVoteCrypto.verifySignature(disputeItemVoteData) ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(UNAUTHORIZED, STATUS_ERROR));
        }

        DisputeData disputeData = disputes.getByHash(disputeItemVoteData.getDisputeHash());

        if (disputeData == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(DISPUTE_NOT_FOUND, STATUS_ERROR));
        }

        if( ! disputeData.getArbitratorHashes().contains(disputeItemVoteData.getUserHash()) ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(UNAUTHORIZED, STATUS_ERROR));
        }

        if (disputeData.getDisputeStatus() != DisputeStatus.Claim) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(DISPUTE_NOT_IN_CLAIM_STATUS, STATUS_ERROR));
        }

        DisputeItemData disputeItemData = disputeData.getDisputeItem(disputeItemVoteData.getItemId());

        if (disputeItemData == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(ITEM_NOT_FOUND, STATUS_ERROR));
        }

        if (disputeItemData.getStatus() != DisputeItemStatus.RejectedByMerchant) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(ITEM_NOT_REJECTED_BY_MERCHANT, STATUS_ERROR));
        }

        if(disputeItemVoteData.getStatus() != DisputeItemStatus.AcceptedByArbitrators && disputeItemVoteData.getStatus() != DisputeItemStatus.RejectedByArbitrators) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(STATUS_NOT_VALID, STATUS_ERROR));
        }

        if(disputeItemData.arbitratorAlreadyVoted(disputeItemVoteData.getUserHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(ALREADY_GOT_YOUR_VOTE, STATUS_ERROR));
        }

        disputeItemData.addItemVoteData(disputeItemVoteData);

        disputeService.updateAfterVote(disputeData);
        disputes.put(disputeData);

        return ResponseEntity.status(HttpStatus.OK).body(new Response(SUCCESS, STATUS_SUCCESS));
    }
}