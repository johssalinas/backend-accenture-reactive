package co.com.accenture.model.exception;

import co.com.accenture.model.exception.message.TechnicalErrorMessage;
import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {

    private final TechnicalErrorMessage technicalErrorMessage;

    public TechnicalException(TechnicalErrorMessage technicalErrorMessage) {
        super(technicalErrorMessage.getDescription());
        this.technicalErrorMessage = technicalErrorMessage;
    }

    public TechnicalException(Throwable cause, TechnicalErrorMessage technicalErrorMessage) {
        super(technicalErrorMessage.getDescription(), cause);
        this.technicalErrorMessage = technicalErrorMessage;
    }
}