package co.com.accenture.api.producto;

import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class ProductoRouterRest {
    @RouterOperations({ @RouterOperation(path = "/api/productos", method = RequestMethod.POST, beanClass = ProductoHandler.class, beanMethod = "save"),
            @RouterOperation(path = "/api/productos/{id}", method = RequestMethod.GET, beanClass = ProductoHandler.class, beanMethod = "findById"),
            @RouterOperation(path = "/api/productos", method = RequestMethod.GET, beanClass = ProductoHandler.class, beanMethod = "findAll"),
            @RouterOperation(path = "/api/productos/{id}", method = RequestMethod.DELETE, beanClass = ProductoHandler.class, beanMethod = "deleteById"),
            @RouterOperation(path = "/api/productos/{id}", method = RequestMethod.PATCH, beanClass = ProductoHandler.class, beanMethod = "updateName"),
            @RouterOperation(path = "/api/franquicias/{franquiciaId}/productos/max-stock-por-sucursal", method = RequestMethod.GET, beanClass = ProductoHandler.class, beanMethod = "findMaxStockBySucursalForFranquicia"),
    })
    @Bean
    public RouterFunction<ServerResponse> routerFunctionProducto(ProductoHandler handler) {
        return route(POST("/api/productos"), handler::save)
                .and(route(GET("/api/productos/{id}"), handler::findById)
                        .andRoute(GET("/api/productos"), handler::findAll)
                        .andRoute(DELETE("/api/productos/{id}"), handler::deleteById)
                        .andRoute(PATCH("/api/productos/{id}"), handler::updateName)
                        .andRoute(GET("/api/franquicias/{franquiciaId}/productos/max-stock-por-sucursal"),
                                handler::findMaxStockBySucursalForFranquicia));
    }
}
