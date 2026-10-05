package com.adc.adcampaign.finance.payments;

import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import com.adc.adcampaign.finance.payments.entity.Account;
import com.adc.adcampaign.finance.payments.entity.PaymentMethod;
import com.adc.adcampaign.finance.payments.entity.Transaction;
import com.adc.adcampaign.finance.payments.entity.TransactionType;
import com.adc.adcampaign.finance.payments.repository.AccountRepository;
import com.adc.adcampaign.finance.payments.repository.PaymentMethodRepository;
import com.adc.adcampaign.finance.payments.repository.TransactionRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Advertiser account balance and fund movement — Finance/Payments, distinct
 * from finance.billing's per-campaign charge ledger. {@link #hasFunds} gates
 * bid eligibility in bidding.BiddingService and {@link #debitForSpend} is
 * called from finance.billing.BillingService on every won, charged
 * impression — a simple balance check/debit, not a hard reservation (see
 * their own javadocs for the accepted non-atomic limitation this implies).
 */
@Service
public class AccountService {

    private static final String DEFAULT_CURRENCY = "USD";

    private static final String INSERT_TRANSACTION_SQL = """
            INSERT INTO payment_transactions
                (tenant_id, advertiser_id, type, amount, currency, source_event_id, provider_charge_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (tenant_id, source_event_id) DO NOTHING
            RETURNING id
            """;

    private static final String ADJUST_BALANCE_SQL = """
            UPDATE accounts SET balance = COALESCE(balance, 0) + ? WHERE id = ?
            """;

    private final AccountRepository accountRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentProviderAdapter paymentProviderAdapter;
    private final AdvertiserRepository advertiserRepository;
    private final JdbcTemplate jdbcTemplate;

    public AccountService(AccountRepository accountRepository,
                           PaymentMethodRepository paymentMethodRepository,
                           TransactionRepository transactionRepository,
                           PaymentProviderAdapter paymentProviderAdapter,
                           AdvertiserRepository advertiserRepository,
                           JdbcTemplate jdbcTemplate) {
        this.accountRepository = accountRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
        this.paymentProviderAdapter = paymentProviderAdapter;
        this.advertiserRepository = advertiserRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Account getOrCreateAccount(Long tenantId, Long advertiserId) {
        if (advertiserRepository.findByIdAndTenantId(advertiserId, tenantId).isEmpty()) {
            throw new NotFoundException("Advertiser not found: " + advertiserId);
        }
        return accountRepository.findByTenantIdAndAdvertiserId(tenantId, advertiserId)
                .orElseGet(() -> accountRepository.save(Account.builder()
                        .tenantId(tenantId)
                        .advertiserId(advertiserId)
                        .balance(BigDecimal.ZERO)
                        .currency(DEFAULT_CURRENCY)
                        .build()));
    }

    /**
     * Charges the payment method via the provider adapter, then — only if
     * {@code idempotencyKey} hasn't been seen before for this tenant —
     * records the transaction and credits the balance. A duplicate request
     * (e.g. a client retry) is a safe no-op: no second charge attempt is
     * even needed since {@code idempotencyKey} uniqueness is checked first
     * via the insert, matching finance.billing.BillingService's pattern.
     */
    @Transactional
    public Transaction topUp(Long tenantId, Long advertiserId, Long paymentMethodId,
                              BigDecimal amount, String idempotencyKey) {
        PaymentMethod method = paymentMethodRepository.findByIdAndTenantIdAndAdvertiserId(
                        paymentMethodId, tenantId, advertiserId)
                .orElseThrow(() -> new NotFoundException("Payment method not found: " + paymentMethodId));
        Account account = getOrCreateAccount(tenantId, advertiserId);

        PaymentProviderAdapter.ChargeResult charge = paymentProviderAdapter.charge(amount, method);
        if (!charge.success()) {
            throw new IllegalStateException("Payment provider declined the charge");
        }

        Long insertedId = jdbcTemplate.query(INSERT_TRANSACTION_SQL,
                rs -> rs.next() ? rs.getLong(1) : null,
                tenantId, advertiserId, TransactionType.TOP_UP.name(), amount, DEFAULT_CURRENCY,
                idempotencyKey, charge.providerChargeId(), Timestamp.valueOf(LocalDateTime.now()));

        if (insertedId != null) {
            jdbcTemplate.update(ADJUST_BALANCE_SQL, amount, account.getId());
            return transactionRepository.findById(insertedId).orElseThrow();
        }
        return findExistingTransaction(tenantId, idempotencyKey);
    }

    /** Debits the balance directly — a refund reverses money already collected, no provider call. */
    @Transactional
    public Transaction refund(Long tenantId, Long advertiserId, BigDecimal amount, String idempotencyKey) {
        Account account = getOrCreateAccount(tenantId, advertiserId);

        Long insertedId = jdbcTemplate.query(INSERT_TRANSACTION_SQL,
                rs -> rs.next() ? rs.getLong(1) : null,
                tenantId, advertiserId, TransactionType.REFUND.name(), amount, DEFAULT_CURRENCY,
                idempotencyKey, null, Timestamp.valueOf(LocalDateTime.now()));

        if (insertedId != null) {
            jdbcTemplate.update(ADJUST_BALANCE_SQL, amount.negate(), account.getId());
            return transactionRepository.findById(insertedId).orElseThrow();
        }
        return findExistingTransaction(tenantId, idempotencyKey);
    }

    private Transaction findExistingTransaction(Long tenantId, String idempotencyKey) {
        return transactionRepository.findByTenantIdAndSourceEventId(tenantId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Conflicting transaction not found for idempotency key: " + idempotencyKey));
    }

    public Account getBalance(Long tenantId, Long advertiserId) {
        return getOrCreateAccount(tenantId, advertiserId);
    }

    /**
     * Read-only funds check for bidding eligibility — unlike every other
     * method here, never creates an account as a side effect (an advertiser
     * who's never funded anything simply has no funds, not a fresh zero
     * account), and is cheap enough for the tmax-bounded bid-decision path.
     */
    public boolean hasFunds(Long tenantId, Long advertiserId) {
        return accountRepository.findByTenantIdAndAdvertiserId(tenantId, advertiserId)
                .map(a -> a.getBalance() != null && a.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .orElse(false);
    }

    /**
     * Debits the balance for a won, charged impression. Idempotent on
     * {@code sourceEventId} — the same id finance.billing.BillingService
     * already uses for the ledger charge; reusing it here (a different
     * table's idempotency scope) correlates the two append-only records of
     * the same real-world event rather than inventing a second id. Not
     * atomically joined with {@link #hasFunds}'s earlier check — the same
     * accepted non-atomic simplicity already relied on by
     * bidding.PacingService's budget check.
     */
    @Transactional
    public void debitForSpend(Long tenantId, Long advertiserId, BigDecimal amount, String sourceEventId) {
        Account account = getOrCreateAccount(tenantId, advertiserId);

        Long insertedId = jdbcTemplate.query(INSERT_TRANSACTION_SQL,
                rs -> rs.next() ? rs.getLong(1) : null,
                tenantId, advertiserId, TransactionType.AD_SPEND.name(), amount, DEFAULT_CURRENCY,
                sourceEventId, null, Timestamp.valueOf(LocalDateTime.now()));

        if (insertedId != null) {
            jdbcTemplate.update(ADJUST_BALANCE_SQL, amount.negate(), account.getId());
        }
    }

    public List<Transaction> listTransactions(Long tenantId, Long advertiserId) {
        return transactionRepository.findByTenantIdAndAdvertiserId(tenantId, advertiserId);
    }
}
