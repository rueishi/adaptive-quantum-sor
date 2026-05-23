package com.nitroj.sor.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class HttpTestClient {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private HttpTestClient() {}

    static Response get(final String uri) throws Exception {
        final HttpResponse<String> response = CLIENT.send(
                HttpRequest.newBuilder(URI.create(uri)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body(),
                response.headers().firstValue("Content-Type").orElse(""));
    }

    record Response(int status, String body, String contentType) {
    }
}
