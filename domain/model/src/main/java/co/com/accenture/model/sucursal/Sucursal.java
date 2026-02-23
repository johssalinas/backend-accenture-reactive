package co.com.accenture.model.sucursal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Sucursal {
    private UUID id;
    private String name;
    private UUID franquiciaId;
}
