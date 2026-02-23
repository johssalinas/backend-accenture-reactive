package co.com.accenture.model.idempotency;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IdempotencyRecord {
    private String clientId;
    private String idempotencyKey;
    private String requestHash;
    private IdempotencyStatus status;
    private String responsePayload;
}
