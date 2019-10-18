package logic.manager.Exceptions;

public class FailedToCreateRepositoryException extends Exception {
    public FailedToCreateRepositoryException(String error){
        super(error);
    }
}
