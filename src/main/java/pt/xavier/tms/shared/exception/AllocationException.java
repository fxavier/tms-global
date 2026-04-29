package pt.xavier.tms.shared.exception;

public class AllocationException extends BusinessException {

    public AllocationException(String message) {
        super("ALLOCATION_BLOCKED", message);
    }

    public AllocationException(String code, String message) {
        super(code, message);
    }
}
