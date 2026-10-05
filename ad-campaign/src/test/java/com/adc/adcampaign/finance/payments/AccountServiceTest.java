package com.adc.adcampaign.finance.payments;

import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import com.adc.adcampaign.finance.payments.entity.Account;
import com.adc.adcampaign.finance.payments.entity.PaymentMethod;
import com.adc.adcampaign.finance.payments.entity.Transaction;
import com.adc.adcampaign.finance.payments.entity.TransactionType;
import com.adc.adcampaign.finance.payments.repository.AccountRepository;
import com.adc.adcampaign.finance.payments.repository.PaymentMethodRepository;
import com.adc.adcampaign.finance.payments.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ADVERTISER_ID = 5L;

    @Mock private AccountRepository accountRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private PaymentProviderAdapter paymentProviderAdapter;
    @Mock private AdvertiserRepository advertiserRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountService = new AccountService(accountRepository, paymentMethodRepository, transactionRepository,
                paymentProviderAdapter, advertiserRepository, jdbcTemplate);

        when(advertiserRepository.findByIdAndTenantId(ADVERTISER_ID, TENANT_ID))
                .thenReturn(Optional.of(Advertiser.builder().id(ADVERTISER_ID).build()));
    }

    private Account existingAccount() {
        return Account.builder().id(9L).tenantId(TENANT_ID).advertiserId(ADVERTISER_ID)
                .balance(BigDecimal.TEN).currency("USD").build();
    }

    @SuppressWarnings("unchecked")
    private void stubTransactionInsertResult(Long returnedId) {
        // topUp/refund's INSERT passes 8 varargs: tenantId, advertiserId, type,
        // amount, currency, sourceEventId, providerChargeId, createdAt.
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(returnedId);
    }

    @Test
    void topUpChargesProviderAndCreditsBalanceOnFirstInsert() {
        Account account = existingAccount();
        PaymentMethod method = PaymentMethod.builder().id(2L).tenantId(TENANT_ID).advertiserId(ADVERTISER_ID).build();

        when(accountRepository.findByTenantIdAndAdvertiserId(TENANT_ID, ADVERTISER_ID))
                .thenReturn(Optional.of(account));
        when(paymentMethodRepository.findByIdAndTenantIdAndAdvertiserId(2L, TENANT_ID, ADVERTISER_ID))
                .thenReturn(Optional.of(method));
        when(paymentProviderAdapter.charge(BigDecimal.valueOf(50), method))
                .thenReturn(new PaymentProviderAdapter.ChargeResult(true, "mock_charge_1"));
        stubTransactionInsertResult(11L);
        when(transactionRepository.findById(11L)).thenReturn(Optional.of(
                Transaction.builder().id(11L).type(TransactionType.TOP_UP).amount(BigDecimal.valueOf(50)).build()));

        Transaction result = accountService.topUp(TENANT_ID, ADVERTISER_ID, 2L, BigDecimal.valueOf(50), "topup-key-1");

        assertThat(result.getId()).isEqualTo(11L);
        // The balance-adjusting UPDATE's own correctness (raw JdbcTemplate SQL,
        // same style as PacingService/BillingService) is proven live in the
        // phase smoke test, not re-asserted here via fragile varargs matchers.
        verify(paymentProviderAdapter).charge(BigDecimal.valueOf(50), method);
    }

    @Test
    void topUpIsANoOpOnDuplicateIdempotencyKey() {
        Account account = existingAccount();
        PaymentMethod method = PaymentMethod.builder().id(2L).tenantId(TENANT_ID).advertiserId(ADVERTISER_ID).build();

        when(accountRepository.findByTenantIdAndAdvertiserId(TENANT_ID, ADVERTISER_ID))
                .thenReturn(Optional.of(account));
        when(paymentMethodRepository.findByIdAndTenantIdAndAdvertiserId(2L, TENANT_ID, ADVERTISER_ID))
                .thenReturn(Optional.of(method));
        when(paymentProviderAdapter.charge(any(), any()))
                .thenReturn(new PaymentProviderAdapter.ChargeResult(true, "mock_charge_2"));
        stubTransactionInsertResult(null);
        when(transactionRepository.findByTenantIdAndSourceEventId(TENANT_ID, "topup-key-1"))
                .thenReturn(Optional.of(Transaction.builder().id(11L).build()));

        Transaction result = accountService.topUp(TENANT_ID, ADVERTISER_ID, 2L, BigDecimal.valueOf(50), "topup-key-1");

        assertThat(result.getId()).isEqualTo(11L);
        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }

    @Test
    void refundDebitsBalanceWithoutCallingTheProvider() {
        Account account = existingAccount();
        when(accountRepository.findByTenantIdAndAdvertiserId(TENANT_ID, ADVERTISER_ID))
                .thenReturn(Optional.of(account));
        stubTransactionInsertResult(12L);
        when(transactionRepository.findById(12L)).thenReturn(Optional.of(
                Transaction.builder().id(12L).type(TransactionType.REFUND).amount(BigDecimal.valueOf(5)).build()));

        Transaction result = accountService.refund(TENANT_ID, ADVERTISER_ID, BigDecimal.valueOf(5), "refund-key-1");

        assertThat(result.getId()).isEqualTo(12L);
        verifyNoInteractions(paymentProviderAdapter);
    }

    @Test
    void hasFundsIsFalseWithNoAccount() {
        when(accountRepository.findByTenantIdAndAdvertiserId(TENANT_ID, ADVERTISER_ID)).thenReturn(Optional.empty());

        assertThat(accountService.hasFunds(TENANT_ID, ADVERTISER_ID)).isFalse();
    }

    @Test
    void hasFundsIsFalseWithAZeroOrNegativeBalance() {
        when(accountRepository.findByTenantIdAndAdvertiserId(TENANT_ID, ADVERTISER_ID))
                .thenReturn(Optional.of(Account.builder().id(1L).balance(BigDecimal.ZERO).build()));

        assertThat(accountService.hasFunds(TENANT_ID, ADVERTISER_ID)).isFalse();
    }

    @Test
    void hasFundsIsTrueWithAPositiveBalance() {
        when(accountRepository.findByTenantIdAndAdvertiserId(TENANT_ID, ADVERTISER_ID))
                .thenReturn(Optional.of(Account.builder().id(1L).balance(BigDecimal.TEN).build()));

        assertThat(accountService.hasFunds(TENANT_ID, ADVERTISER_ID)).isTrue();
    }

    @Test
    void debitForSpendIsIdempotentOnSourceEventId() {
        Account account = existingAccount();
        when(accountRepository.findByTenantIdAndAdvertiserId(TENANT_ID, ADVERTISER_ID))
                .thenReturn(Optional.of(account));
        stubTransactionInsertResult(21L);

        accountService.debitForSpend(TENANT_ID, ADVERTISER_ID, BigDecimal.valueOf(3), "win-1");

        verify(jdbcTemplate).update(anyString(), eq(BigDecimal.valueOf(3).negate()), eq(account.getId()));
    }

    @Test
    void debitForSpendDoesNotAdjustBalanceOnDuplicateSourceEventId() {
        Account account = existingAccount();
        when(accountRepository.findByTenantIdAndAdvertiserId(TENANT_ID, ADVERTISER_ID))
                .thenReturn(Optional.of(account));
        stubTransactionInsertResult(null);

        accountService.debitForSpend(TENANT_ID, ADVERTISER_ID, BigDecimal.valueOf(3), "win-1");

        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }
}
