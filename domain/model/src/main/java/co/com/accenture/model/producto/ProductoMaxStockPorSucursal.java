package co.com.accenture.model.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProductoMaxStockPorSucursal {
    private UUID sucursalId;
    private String sucursalName;
    private UUID productoId;
    private String productoName;
    private Integer stock;
}
