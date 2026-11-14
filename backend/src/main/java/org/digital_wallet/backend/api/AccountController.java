package org.digital_wallet.backend.api;

import org.digital_wallet.backend.api.model.Account;
import org.digital_wallet.backend.api.model.AccountOverview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController implements AccountsApi {

    //    todo: write integration test

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public ResponseEntity<AccountOverview> getAccountOverview() {
        return accountService.getAccountOverview()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Account> getAccount(String accountId) {
        return accountService.getAccount(accountId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
