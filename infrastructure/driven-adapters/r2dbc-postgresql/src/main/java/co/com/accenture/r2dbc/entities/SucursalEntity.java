package co.com.accenture.r2dbc.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("sucursal")
public class SucursalEntity {
    @Id
    private UUID id;

    private String name;

    @Column("franquicia_id")
    private UUID franquiciaId;

    @Version
    private Long version;
}
