package br.com.acme.transaction;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import br.com.acme.transaction.PaymentRepository;
import br.com.acme.transaction.PaymentServiceImpl;
import br.com.acme.transaction.ValidationService;
import br.com.acme.transaction.exception.ServiceException;
import br.com.acme.user.User;

public class MockitoTestBDD {

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
        given(validationService.conformsTo(debtor)).willReturn(Boolean.TRUE);
        given(paymentRepository.findBalance(debtor)).willReturn(valueToBeTransfered.add(BigDecimal.ONE));
        paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
        verify(paymentRepository).addBalance(debtor, valueToBeTransfered.negate());
        verify(paymentRepository).addBalance(creditor, valueToBeTransfered);
    }

    @Test
    public void shouldNotCompleteTransactionWhenDebtorDoesNotHaveEnoughBalance() throws ServiceException {
        valueToBeTransfered = new BigDecimal("90");
        given(validationService.conformsTo(debtor)).willReturn(Boolean.TRUE);
        given(paymentRepository.findBalance(debtor)).willReturn(valueToBeTransfered.subtract(BigDecimal.ONE));
        try {
            paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
            fail("Should have failed since debtor does not have enough balance to transfer");
        } catch (ServiceException e) {
            assertTrue(true);
        } finally {
            verify(paymentRepository, never()).addBalance(debtor, valueToBeTransfered.negate());
            verify(paymentRepository, never()).addBalance(debtor, valueToBeTransfered);
        }
    }

    @Test
    public void shouldNotCompleteTransactionWhenDebtorDoesNotExist() throws ServiceException {
        valueToBeTransfered = new BigDecimal("90");
        given(validationService.conformsTo(debtor)).willReturn(Boolean.TRUE);
        given(paymentRepository.findBalance(debtor)).willReturn(null);
        try {
            paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
            fail("Should have failed since debtor does not exist");
        } catch (ServiceException e) {
            assertTrue(true);
        } finally {
            verify(paymentRepository, never()).addBalance(debtor, valueToBeTransfered.negate());
            verify(paymentRepository, never()).addBalance(debtor, valueToBeTransfered);
        }
    }

    @Test
    public void shouldNotCompleteTransactionWhenDebtorIsNotValid() throws ServiceException {
        valueToBeTransfered = new BigDecimal("90");
        given(validationService.conformsTo(debtor)).willReturn(Boolean.FALSE);
        try {
            paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
        } catch (ServiceException e) {
            assertTrue(true);
        } finally {
            verify(paymentRepository, never()).addBalance(debtor, valueToBeTransfered.negate());
            verify(paymentRepository, never()).addBalance(debtor, valueToBeTransfered);
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
}
