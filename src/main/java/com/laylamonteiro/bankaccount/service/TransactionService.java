package com.laylamonteiro.bankaccount.service;

import com.laylamonteiro.bankaccount.dao.TransactionDAO;
import com.laylamonteiro.bankaccount.dto.request.TransactionForm;
import com.laylamonteiro.bankaccount.dto.response.CreateTransactionDTO;
import com.laylamonteiro.bankaccount.dto.response.TransactionDTO;
import com.laylamonteiro.bankaccount.entity.Balance;
import com.laylamonteiro.bankaccount.entity.Transaction;
import com.laylamonteiro.bankaccount.enums.TransactionDirection;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.laylamonteiro.bankaccount.enums.AvailableCurrency.findByValue;

@Slf4j
@Service
public class TransactionService {

    @Autowired
    private TransactionDAO transactionDAO;

    @Autowired
    private BalanceService balanceService;


    public List<TransactionDTO> findAll() {
        List<Transaction> transactions = transactionDAO.findAll();
        return toDTO(transactions);
    }

    public List<TransactionDTO> findAllByAccountId(Long accountId) {
        List<Transaction> existingTransactions = transactionDAO.findAllByAccountId(accountId);

        if (existingTransactions.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("Found '{}' transactions for accountId '{}'.", existingTransactions.size(), accountId);

        return toDTO(existingTransactions);
    }

    public CreateTransactionDTO create(TransactionForm form) throws NotFoundException {
        Transaction transaction = new Transaction();
        transaction.setAccountId(form.getAccountId());
        transaction.setAmount(form.getAmount());
        transaction.setCurrency(form.getCurrency());
        transaction.setDirection(form.getDirection());
        transaction.setDescription(form.getDescription());

        validateTransaction(transaction);
        Balance balance = getBalanceByAccountIdAndCurrency(transaction);

        if (balance == null) {
            balanceService.createBalances(transaction.getAccountId(), Collections.singletonList(transaction.getCurrency()));
            balance = getBalanceByAccountIdAndCurrency(transaction);
        }

        if (form.getDirection().equals(TransactionDirection.IN)) {
            balance.setAvailableAmount(balance.getAvailableAmount().add(transaction.getAmount()));
            balanceService.updateBalance(balance);
        }
        if (form.getDirection().equals(TransactionDirection.OUT)) {
            balance.setAvailableAmount(balance.getAvailableAmount().subtract(transaction.getAmount()));
            balanceService.updateBalance(balance);
        }

        transactionDAO.createTransaction(transaction);
        return toDTO(transaction);
    }

    private void validateTransaction(Transaction transaction) {
        Boolean currencyAllowed = findByValue(transaction.getCurrency().toString());
        if (!currencyAllowed) {
            throw new IllegalArgumentException("Invalid currency.");
        } else if (insufficientFunds(transaction)) {
            throw new IllegalArgumentException("Insufficient funds.");
        } else if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid amount.");
        }
    }

    private Boolean insufficientFunds(Transaction transaction) {
        if (transaction.getDirection().equals(TransactionDirection.OUT)) {
            Balance balanceByCurrency = getBalanceByAccountIdAndCurrency(transaction);
            BigDecimal availableAmount = balanceByCurrency.getAvailableAmount();
            BigDecimal transactionAmount = transaction.getAmount();
            return (availableAmount.compareTo(transactionAmount) < 0);
        } else {
            return false;
        }
    }

    private Balance getBalanceByAccountIdAndCurrency(Transaction transaction) {
        List<Balance> balancesByAccountId = balanceService.findBalancesByAccountId(transaction.getAccountId());
        return balancesByAccountId.stream().filter(balance ->
                balance.getCurrency().equals(transaction.getCurrency())).findFirst().orElse(null);
    }

    private CreateTransactionDTO toDTO(Transaction transaction) {
        return new CreateTransactionDTO(
                transaction.getAccountId(),
                transaction.getTransactionId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDirection(),
                transaction.getDescription(),
                balanceService.findBalancesByAccountId(transaction.getAccountId())
        );
    }

    private List<TransactionDTO> toDTO(List<Transaction> transactions) {
        List<TransactionDTO> dtos = new ArrayList<>();

        for (Transaction transaction : transactions) {
            TransactionDTO dto = new TransactionDTO(
                    transaction.getAccountId(),
                    transaction.getTransactionId(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getDirection(),
                    transaction.getDescription()
            );
            dtos.add(dto);
        }
        return dtos;
    }

}
