package co.com.accenture.api.franquicia;

import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class FranquiciaRouterRest {
    @RouterOperations({@RouterOperation(path = "/api/franquicias", method = RequestMethod.POST, beanClass = FranquiciaHandler.class, beanMethod = "save"),
            @RouterOperation(path = "/api/franquicias/{id}", method = RequestMethod.GET, beanClass = FranquiciaHandler.class, beanMethod = "findById"),
            @RouterOperation(path = "/api/franquicias", method = RequestMethod.GET, beanClass = FranquiciaHandler.class, beanMethod = "findAll"),
            @RouterOperation(path = "/api/franquicias/{id}", method = RequestMethod.DELETE, beanClass = FranquiciaHandler.class, beanMethod = "deleteById"),
            @RouterOperation(path = "/api/franquicias/{id}", method = RequestMethod.PATCH, beanClass = FranquiciaHandler.class, beanMethod = "updateName"),
    })
    @Bean
    public RouterFunction<ServerResponse> routerFunction(FranquiciaHandler handler) {
        return route(POST("/api/franquicias"), handler::save)
                .and(route(GET("/api/franquicias/{id}"), handler::findById)
                .andRoute(GET("/api/franquicias"), handler::findAll)
                .andRoute(DELETE("/api/franquicias/{id}"), handler::deleteById)
                .andRoute(PATCH("/api/franquicias/{id}"), handler::updateName));
    }
}
