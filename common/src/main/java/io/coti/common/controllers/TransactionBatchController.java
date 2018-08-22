package io.coti.common.controllers;

import io.coti.common.http.GetTransactionBatchRequest;
import io.coti.common.http.GetTransactionBatchResponse;
import io.coti.common.services.TransactionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Slf4j
@Controller
public class TransactionBatchController {

    @Autowired
    private TransactionHelper transactionHelper;


    @RequestMapping(value = "/getTransactionBatch", method = POST)
    public ResponseEntity<GetTransactionBatchResponse> getTransactionBatch(@Valid @RequestBody GetTransactionBatchRequest getTransactionBatchRequest) {
        log.info("Recccccc");
        return ResponseEntity.ok(transactionHelper.getTransactionBatch(getTransactionBatchRequest.getStartingIndex()));
    }
}