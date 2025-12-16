package com.example.order_service.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestClient {

    private final RestTemplate restTemplate;

    /* =========================================================
       GENERIC EXCHANGE
       ========================================================= */

    public <T> T exchange(
            HttpMethod method,
            String baseUrl,
            String path,
            Map<String, ?> pathVariables,
            Map<String, ?> queryParams,
            Object body,
            Class<T> responseType,
            Map<String, String> headers
    ) {
        URI uri = buildUri(baseUrl, path, pathVariables, queryParams);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }

        HttpEntity<?> requestEntity =
                body != null
                        ? new HttpEntity<>(body, httpHeaders)
                        : new HttpEntity<>(httpHeaders);

        ResponseEntity<T> response =
                restTemplate.exchange(
                        uri,
                        method,
                        requestEntity,
                        responseType
                );

        return response.getBody();
    }

    /* =========================================================
       CONVENIENCE METHODS
       ========================================================= */

    public <T> T get(
            String baseUrl,
            String path,
            Map<String, ?> pathVariables,
            Map<String, ?> queryParams,
            Class<T> responseType
    ) {
        return exchange(
                HttpMethod.GET,
                baseUrl,
                path,
                pathVariables,
                queryParams,
                null,
                responseType,
                null
        );
    }

    public <T> T post(
            String baseUrl,
            String path,
            Map<String, ?> pathVariables,
            Object body,
            Class<T> responseType
    ) {
        return exchange(
                HttpMethod.POST,
                baseUrl,
                path,
                pathVariables,
                null,
                body,
                responseType,
                null
        );
    }

    public <T> T put(
            String baseUrl,
            String path,
            Map<String, ?> pathVariables,
            Object body,
            Class<T> responseType
    ) {
        return exchange(
                HttpMethod.PUT,
                baseUrl,
                path,
                pathVariables,
                null,
                body,
                responseType,
                null
        );
    }

    public void delete(
            String baseUrl,
            String path,
            Map<String, ?> pathVariables
    ) {
        exchange(
                HttpMethod.DELETE,
                baseUrl,
                path,
                pathVariables,
                null,
                null,
                Void.class,
                null
        );
    }

    /* =========================================================
       URI BUILDER
       ========================================================= */

    private URI buildUri(
            String baseUrl,
            String path,
            Map<String, ?> pathVariables,
            Map<String, ?> queryParams
    ) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUri(URI.create(baseUrl)).path(path);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }

        return pathVariables != null
                ? builder.buildAndExpand(pathVariables).toUri()
                : builder.build().toUri();
    }
}
