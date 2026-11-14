package org.digital_wallet.backend.api;

import org.digital_wallet.backend.api.model.AccountDto;
import org.digital_wallet.backend.api.model.AccountOverviewDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController implements AccountsApi {

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public ResponseEntity<AccountOverviewDto> getAccountOverview() {
        return ResponseEntity.ok(accountService.getAccountOverview());
    }

    @Override
    public ResponseEntity<AccountDto> getAccount(String accountId) {
        return accountService.getAccount(accountId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
