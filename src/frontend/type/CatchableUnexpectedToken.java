package frontend.type;

public class CatchableUnexpectedToken extends RuntimeException {
    public CatchableUnexpectedToken(String message) {
        super(message);
    }
}
