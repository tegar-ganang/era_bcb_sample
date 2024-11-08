package br.com.acme.transaction;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import br.com.acme.transaction.PaymentRepository;
import br.com.acme.transaction.PaymentServiceImpl;
import br.com.acme.transaction.ValidationService;
import br.com.acme.transaction.exception.ServiceException;
import br.com.acme.user.User;

public class MockitoTestRefactored {

    private ValidationService validationService;

    private PaymentRepository paymentRepository;

    private PaymentServiceImpl paymentService;

    private User creditor;

    private User debtor;

    private BigDecimal valueToBeTransfered;

    @Before
    public void init() {
        paymentService = new PaymentServiceImpl();
        validationService = mock(ValidationService.class);
        paymentRepository = mock(PaymentRepository.class);
        paymentService.validationService = validationService;
        paymentService.paymentRepository = paymentRepository;
        creditor = new User("joe@phoenix.com");
        debtor = new User("jane@phoenix.com");
        valueToBeTransfered = null;
    }

    @Test
    public void shouldCompleteTransaction() throws Exception {
        valueToBeTransfered = new BigDecimal("90");
        expectDebtorConformsToValidation(true);
        expectDebtorBalance(valueToBeTransfered.add(BigDecimal.ONE));
        paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
        verify(paymentRepository).addBalance(debtor, valueToBeTransfered.negate());
        verify(paymentRepository).addBalance(creditor, valueToBeTransfered);
    }

    @Test
    public void shouldNotCompleteTransactionWhenDebtorDoesNotHaveEnoughBalance() throws ServiceException {
        valueToBeTransfered = new BigDecimal("90");
        expectDebtorConformsToValidation(true);
        expectDebtorBalance(valueToBeTransfered.subtract(BigDecimal.ONE));
        try {
            paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
            fail("Should have failed since debtor does not have enough balance to transfer");
        } catch (ServiceException e) {
            assertTrue(true);
        } finally {
            verifyNoBalanceWasChanged();
        }
    }

    private void verifyNoBalanceWasChanged() {
        verify(paymentRepository, never()).addBalance(debtor, valueToBeTransfered.negate());
        verify(paymentRepository, never()).addBalance(debtor, valueToBeTransfered);
    }

    @Test
    public void shouldNotCompleteTransactionWhenDebtorDoesNotExist() throws ServiceException {
        valueToBeTransfered = new BigDecimal("90");
        expectDebtorConformsToValidation(true);
        expectDebtorBalance(null);
        try {
            paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
            fail("Should have failed since debtor does not exist");
        } catch (ServiceException e) {
            assertTrue(true);
        } finally {
            verifyNoBalanceWasChanged();
        }
    }

    @Test
    public void shouldNotCompleteTransactionWhenDebtorIsNotValid() throws ServiceException {
        valueToBeTransfered = new BigDecimal("90");
        expectDebtorConformsToValidation(false);
        try {
            paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
        } catch (ServiceException e) {
            assertTrue(true);
        } finally {
            verifyNoBalanceWasChanged();
        }
    }

    @Test
    public void oneOfTransacteesOrValueToTransferIsNull() throws ServiceException {
        valueToBeTransfered = new BigDecimal("90");
        try {
            paymentService.transferFrom(null, creditor, valueToBeTransfered);
            fail("Debtor must not be null");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            paymentService.transferFrom(debtor, null, valueToBeTransfered);
            fail("Creditor must not be null");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            paymentService.transferFrom(debtor, creditor, null);
            fail("Transfer value must not be null");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    private void expectDebtorBalance(BigDecimal debtorBalance) {
        when(paymentRepository.findBalance(debtor)).thenReturn(debtorBalance);
    }

    private void expectDebtorConformsToValidation(Boolean conformsTo) throws ServiceException {
        when(validationService.conformsTo(debtor)).thenReturn(conformsTo);
    }
}
