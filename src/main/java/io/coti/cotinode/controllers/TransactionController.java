package io.coti.cotinode.controllers;

import io.coti.cotinode.http.AddTransactionRequest;
import io.coti.cotinode.http.AddTransactionResponse;
import io.coti.cotinode.http.GetTransactionRequest;
import io.coti.cotinode.http.GetTransactionResponse;
import io.coti.cotinode.service.interfaces.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static io.coti.cotinode.http.HttpStringConstants.STATUS_ERROR;
import static io.coti.cotinode.http.HttpStringConstants.STATUS_SUCCESS;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private ITransactionService transactionService;

    @RequestMapping(method = PUT)
    public ResponseEntity<AddTransactionResponse> addTransaction(@Valid @RequestBody AddTransactionRequest addTransactionRequest) {
        return transactionService.addNewTransaction(addTransactionRequest);
    }

    @RequestMapping(method = POST)
    public GetTransactionResponse getTransactionDetails(@Valid @RequestBody GetTransactionRequest getTransactionRequest) {
        transactionService.getTransactionData(getTransactionRequest.transactionHash);
        return new GetTransactionResponse();
    }
}