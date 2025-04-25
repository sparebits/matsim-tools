/*
 * GlobalExceptionHandler
 * @author : neiko.neikov
 * @created : 22.04.25 г., Tuesday
 */
package com.sparebits.matsim;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                "An unexpected error occurred.",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // You can add more specific handlers:
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                "Bad Request",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
