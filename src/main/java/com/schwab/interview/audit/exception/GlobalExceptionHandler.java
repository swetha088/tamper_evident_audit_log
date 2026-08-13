package com.schwab.interview.audit.exception;
import com.schwab.interview.audit.dto.AuditDtos.ErrorResponse; import jakarta.servlet.http.HttpServletRequest; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(AuditNotFoundException.class) ResponseEntity<ErrorResponse> notFound(AuditNotFoundException e,HttpServletRequest r){return error(HttpStatus.NOT_FOUND,"NOT_FOUND",e,r);}
 @ExceptionHandler({IllegalArgumentException.class,org.springframework.web.bind.MethodArgumentNotValidException.class}) ResponseEntity<ErrorResponse> invalid(Exception e,HttpServletRequest r){return error(HttpStatus.BAD_REQUEST,"INVALID_REQUEST",e,r);}
 @ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unexpected(Exception e,HttpServletRequest r){return error(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR",new RuntimeException("An unexpected error occurred"),r);}
 private ResponseEntity<ErrorResponse> error(HttpStatus status,String code,Exception e,HttpServletRequest r){return ResponseEntity.status(status).body(new ErrorResponse(code,e.getMessage(),r.getHeader("X-Correlation-Id")));}
}
