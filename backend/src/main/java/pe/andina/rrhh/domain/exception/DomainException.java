package pe.andina.rrhh.domain.exception;

public class DomainException extends RuntimeException {

    public enum Code {
        NOT_FOUND,
        BAD_REQUEST,
        FORBIDDEN,
        UNAUTHORIZED,
        CONFLICT
    }

    private final Code code;

    public DomainException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    public static DomainException notFound(String message) {
        return new DomainException(Code.NOT_FOUND, message);
    }

    public static DomainException badRequest(String message) {
        return new DomainException(Code.BAD_REQUEST, message);
    }

    public static DomainException forbidden(String message) {
        return new DomainException(Code.FORBIDDEN, message);
    }

    public static DomainException unauthorized(String message) {
        return new DomainException(Code.UNAUTHORIZED, message);
    }

    public static DomainException conflict(String message) {
        return new DomainException(Code.CONFLICT, message);
    }
}
