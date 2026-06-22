package com.example.jhapcham.config;

import com.example.jhapcham.common.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Component
@RestControllerAdvice
public class StandardApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> || body instanceof String || body instanceof byte[]
                || body instanceof Resource || body instanceof StreamingResponseBody) {
            return body;
        }

        String message = "Request completed successfully";
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String method = servletRequest.getServletRequest().getMethod();
            if ("POST".equalsIgnoreCase(method)) {
                message = "Created successfully";
            } else if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                message = "Updated successfully";
            } else if ("DELETE".equalsIgnoreCase(method)) {
                message = "Deleted successfully";
            }
        }

        if (body == null && response instanceof ServletServerHttpResponse servletResponse
                && servletResponse.getServletResponse().getStatus() == HttpStatus.NO_CONTENT.value()) {
            servletResponse.getServletResponse().setStatus(HttpStatus.OK.value());
        }

        return ApiResponse.success(message, body);
    }
}
