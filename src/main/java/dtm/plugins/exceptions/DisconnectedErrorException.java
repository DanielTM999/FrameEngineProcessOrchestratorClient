package dtm.plugins.exceptions;

import dtm.apps.exceptions.DisplayException;

public class DisconnectedErrorException extends DisplayException {
    public DisconnectedErrorException(String message) {
        super(message);
    }
}
