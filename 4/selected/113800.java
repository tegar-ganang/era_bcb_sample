package br.com.acme.transaction;

import java.math.BigDecimal;
import br.com.acme.transaction.exception.ServiceException;
import br.com.acme.user.User;

public class PaymentServiceImpl implements PaymentService {

    protected ValidationService validationService;

    protected PaymentRepository paymentRepository;

    public void transferFrom(User debtor, User creditor, BigDecimal transferValue) throws ServiceException {
        if (debtor == null) {
            throw new IllegalArgumentException("Debtor must not be null");
        }
        if (creditor == null) {
            throw new IllegalArgumentException("Creditor must not be null");
        }
        if (transferValue == null) {
            throw new IllegalArgumentException("Transfer value must not be null");
        }
        if (validationService.conformsTo(debtor)) {
            BigDecimal debtorBalance = paymentRepository.findBalance(debtor);
            if (debtorBalance != null && debtorBalance.compareTo(transferValue) > 0) {
                paymentRepository.addBalance(debtor, transferValue.negate());
                paymentRepository.addBalance(creditor, transferValue);
            } else {
                throw new ServiceException("Debtor does not have enough balance for this transfer");
            }
        }
    }

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }

    public void setPaymentRepository(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
}
