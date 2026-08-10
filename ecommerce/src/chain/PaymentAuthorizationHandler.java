package chain;

import exceptions.PaymentDeclinedException;

/**
 * Last link in the chain: mocks an issuer decline for a specific test
 * payment method token so the design has a way to exercise a failed
 * checkout without wiring an actual payment gateway.
 */
public class PaymentAuthorizationHandler extends OrderValidationHandler {

    private static final String DECLINED_METHOD = "INVALID_CARD";

    @Override
    protected void validate(OrderValidationContext context) {
        if (DECLINED_METHOD.equals(context.getPaymentMethod())) {
            throw new PaymentDeclinedException(
                    "Payment authorization declined for method " + context.getPaymentMethod());
        }
    }
}
