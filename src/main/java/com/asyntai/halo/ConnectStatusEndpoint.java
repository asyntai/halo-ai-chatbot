package com.asyntai.halo;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * GET /apis/console.api.asyntai.halo.run/v1alpha1/connections/{state}
 *
 * <p>The "Connect Asyntai" tab calls this endpoint while the sign-in window is
 * open. The plugin asks asyntai.com on the server side and returns plain JSON:
 * {@code {"connected":false}} or {@code {"connected":true,"siteId":...,
 * "scriptUrl":...,"accountEmail":...}}. The role template in
 * extensions/role-templates.yaml grants it.</p>
 */
@Component
public class ConnectStatusEndpoint implements CustomEndpoint {

    public static final String API_VERSION = "console.api.asyntai.halo.run/v1alpha1";

    private final ConnectStatusClient client =
        new ConnectStatusClient(ConnectStatusClient.originFromSystem());

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route(GET("/connections/{state}"), this::status);
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(API_VERSION);
    }

    Mono<ServerResponse> status(ServerRequest request) {
        String state = request.pathVariable("state");
        if (!ConnectStatusClient.isValidState(state)) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", "invalid_state"));
        }
        return client.fetch(state)
            .flatMap(s -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-store")
                .bodyValue(s))
            .onErrorResume(e -> ServerResponse.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", "asyntai_unreachable")));
    }
}
