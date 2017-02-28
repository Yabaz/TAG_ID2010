package dsv.pis.gotag.exceptions;

/**
 * Created by YannL on 28/02/2017.
 */

import java.util.UUID;

public class NoSuchAgentException extends Exception {

    private static final long serialVersionUID = 5034388446362600923L;
    private UUID unknownName;

    public NoSuchAgentException(UUID unknownName) {
        super("No such agent in the Bailiff " + unknownName);
        this.unknownName = unknownName;
    }

    public UUID getUnknownName() {
        return unknownName;
    }
}
