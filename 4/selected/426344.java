package br.com.acme.transaction;

import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import br.com.acme.transaction.PaymentRepository;
import br.com.acme.transaction.PaymentServiceImpl;
import br.com.acme.transaction.ValidationService;
import br.com.acme.transaction.exception.ServiceException;
import br.com.acme.user.User;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

public class EasyMockTest {

    private ValidationService mockValidation;

    private PaymentRepository mockRepository;

    private PaymentServiceImpl paymentService;

    private User creditor;

    private User debtor;

    @Before
    public void init() {
        paymentService = new PaymentServiceImpl();
        mockValidation = createMock(ValidationService.class);
        mockRepository = createMock(PaymentRepository.class);
        paymentService.validationService = mockValidation;
        paymentService.paymentRepository = mockRepository;
        creditor = new User("joe@phoenix.com");
        debtor = new User("jane@phoenix.com");
    }

    @Test
    public void shouldCompleteTransaction() throws Exception {
        BigDecimal valueToBeTransfered = new BigDecimal("90");
        debtorIsAbleToTransfer(true);
        transacteeHasBalance(debtor, valueToBeTransfered.add(BigDecimal.ONE));
        debtorHasItsBalanceDeducted(valueToBeTransfered);
        creditorHasItsBalanceCredited(valueToBeTransfered);
        replayAll();
        paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
        verifyAll();
    }

    @Test
    public void shouldNotCompleteTransactionWhenDebtorDoesNotHaveEnoughBalance() throws ServiceException {
        BigDecimal valueToBeTransfered = new BigDecimal("90");
        debtorIsAbleToTransfer(true);
        transacteeHasBalance(debtor, valueToBeTransfered.subtract(BigDecimal.ONE));
        replayAll();
        try {
            paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
            fail("Should have failed since debtor does not have enough balance to transfer");
        } catch (ServiceException e) {
            assertTrue(true);
        } finally {
            verifyAll();
        }
    }

    @Test
    public void shouldNotCompleteTransactionWhenDebtorDoesNotExist() throws ServiceException {
        BigDecimal valueToBeTransfered = new BigDecimal("90");
        debtorIsAbleToTransfer(true);
        transacteeHasBalance(debtor, null);
        replayAll();
        try {
            paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
            fail("Should have failed since debtor does not exist");
        } catch (ServiceException e) {
            assertTrue(true);
        } finally {
            verifyAll();
        }
    }

    @Test
    public void shouldNotCompleteTransactionWhenDebtorIsNotValid() throws ServiceException {
        BigDecimal valueToBeTransfered = new BigDecimal("90");
        debtorIsAbleToTransfer(false);
        replayAll();
        try {
            paymentService.transferFrom(debtor, creditor, valueToBeTransfered);
        } catch (ServiceException e) {
            assertTrue(true);
        } finally {
            verifyAll();
        }
    }

    @Test
    public void oneOfTransacteesOrValueToTransferIsNull() throws ServiceException {
        BigDecimal valueToBeTransfered = new BigDecimal("90");
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

    private void replayAll() {
        replay(mockRepository, mockValidation);
    }

    private void verifyAll() {
        verify(mockRepository, mockValidation);
    }

    private void creditorHasItsBalanceCredited(BigDecimal transferValue) {
        mockRepository.addBalance(creditor, transferValue);
        expectLastCall().once();
    }

    private void debtorHasItsBalanceDeducted(BigDecimal transferValue) {
        mockRepository.addBalance(debtor, transferValue.negate());
        expectLastCall().once();
    }

    private void transacteeHasBalance(User platformUser, BigDecimal debtorBalance) {
        expect(mockRepository.findBalance(platformUser)).andReturn(debtorBalance);
    }

    private void debtorIsAbleToTransfer(Boolean isAbleToTransfer) throws ServiceException {
        expect(mockValidation.conformsTo(debtor)).andReturn(isAbleToTransfer);
    }
}
