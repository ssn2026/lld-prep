package command;

import model.TransactionReceipt;

/**
 * Command pattern: a withdrawal or deposit is encapsulated as an object
 * carrying its own parameters, so AtmService can execute it uniformly and
 * log whatever receipt comes back without switching on transaction type.
 * Adding a new transaction kind (e.g. a transfer) is a new command class,
 * not a new branch in AtmService.
 */
public interface TransactionCommand {
    TransactionReceipt execute();
}
