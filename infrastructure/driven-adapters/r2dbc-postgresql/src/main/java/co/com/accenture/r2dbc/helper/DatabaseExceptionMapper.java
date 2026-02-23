package co.com.accenture.r2dbc.helper;

import co.com.accenture.model.exception.TechnicalException;
import co.com.accenture.model.exception.message.TechnicalErrorMessage;
import io.r2dbc.spi.R2dbcException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;

public final class DatabaseExceptionMapper {

    private DatabaseExceptionMapper() {
    }

    public static Throwable map(Throwable throwable) {
        if (throwable instanceof TechnicalException) {
            return throwable;
        }
        if (throwable instanceof DataIntegrityViolationException) {
            return new TechnicalException(throwable, TechnicalErrorMessage.DATABASE_CONSTRAINT_ERROR);
        }
        if (throwable instanceof TransientDataAccessResourceException || throwable instanceof R2dbcException) {
            return new TechnicalException(throwable, TechnicalErrorMessage.DATABASE_CONNECTION_ERROR);
        }
        return new TechnicalException(throwable, TechnicalErrorMessage.DATABASE_OPERATION_ERROR);
    }
}