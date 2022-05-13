package com.laylamonteiro.bankaccount.dto.response;

import com.laylamonteiro.bankaccount.entity.Balance;
import com.laylamonteiro.bankaccount.enums.TransactionDirection;
import com.laylamonteiro.bankaccount.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {

    private Long accountId;
    private Long transactionId;
    private BigDecimal amount;
    private Currency currency;
    private TransactionDirection direction;
    private String description;
    private Balance balance;

    public TransactionDTO(Transaction transaction) {

    }
}
