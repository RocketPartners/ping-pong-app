package com.example.javapingpongelo.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response with data payload
 * @param <T> Type of data being returned
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiDataResponse<T> {
    private boolean success;
    private String message;
    private T data;

    /**
     * Create a success response with data and message
     */
    public static <T> ApiDataResponse<T> success(String message, T data) {
        return new ApiDataResponse<>(true, message, data);
    }

    /**
     * Create an error response with message
     */
    public static <T> ApiDataResponse<T> error(String message) {
        return new ApiDataResponse<>(false, message, null);
    }
}