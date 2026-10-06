package com.example.tiketbioskop.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Unique / foreign key violation from the DB (duplicate value, or data still referenced)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public void handleDataIntegrityViolation(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(),
                "Data conflicts with existing data: duplicate value or still referenced by other data.");
    }

    // ?sort= names a field that doesn't exist
    @ExceptionHandler(PropertyReferenceException.class)
    public void handleUnknownSortProperty(PropertyReferenceException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
}
