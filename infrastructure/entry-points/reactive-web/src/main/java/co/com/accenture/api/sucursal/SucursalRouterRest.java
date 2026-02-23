package co.com.accenture.api.sucursal;

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
public class SucursalRouterRest {
    @RouterOperations({ @RouterOperation(path = "/api/sucursales", method = RequestMethod.POST, beanClass = SucursalHandler.class, beanMethod = "save"),
            @RouterOperation(path = "/api/sucursales/{id}", method = RequestMethod.GET, beanClass = SucursalHandler.class, beanMethod = "findById"),
            @RouterOperation(path = "/api/sucursales", method = RequestMethod.GET, beanClass = SucursalHandler.class, beanMethod = "findAll"),
            @RouterOperation(path = "/api/sucursales/{id}", method = RequestMethod.DELETE, beanClass = SucursalHandler.class, beanMethod = "deleteById"),
            @RouterOperation(path = "/api/sucursales/{id}", method = RequestMethod.PATCH, beanClass = SucursalHandler.class, beanMethod = "updateName"),
    })
    @Bean
    public RouterFunction<ServerResponse> routerFunctionSucursal(SucursalHandler handler) {
        return route(POST("/api/sucursales"), handler::save)
                .and(route(GET("/api/sucursales/{id}"), handler::findById)
                        .andRoute(GET("/api/sucursales"), handler::findAll)
                        .andRoute(DELETE("/api/sucursales/{id}"), handler::deleteById)
                        .andRoute(PATCH("/api/sucursales/{id}"), handler::updateName));
    }
}
