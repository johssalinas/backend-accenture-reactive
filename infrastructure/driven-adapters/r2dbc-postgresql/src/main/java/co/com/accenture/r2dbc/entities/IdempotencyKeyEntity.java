package co.com.accenture.r2dbc.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("idempotency_key")
public class IdempotencyKeyEntity {
    @Id
    private UUID id;

    private String clientId;
    private String idempotencyKey;
    private String requestHash;
    private String status;
    private String responsePayload;
}
