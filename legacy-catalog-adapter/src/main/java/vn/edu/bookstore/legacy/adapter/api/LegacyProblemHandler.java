
package vn.edu.bookstore.legacy.adapter.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.bookstore.legacy.adapter.soap.LegacyCatalogException;

@RestControllerAdvice
public class LegacyProblemHandler {

    @ExceptionHandler(LegacyCatalogException.class)
    ProblemDetail onLegacyCatalog(
            LegacyCatalogException e,
            HttpServletRequest request) {

        HttpStatus status;
        URI type;
        String title;

        switch (e.kind()) {
            case NOT_FOUND -> {
                status = HttpStatus.NOT_FOUND;
                type = ProblemTypes.BOOK_NOT_FOUND;
                title = "Không tìm thấy sách";
            }
            case INVALID_REQUEST -> {
                status = HttpStatus.BAD_REQUEST;
                type = ProblemTypes.VALIDATION_FAILED;
                title = "Yêu cầu không hợp lệ";
            }
            case UPSTREAM_TIMEOUT -> {
                status = HttpStatus.GATEWAY_TIMEOUT;
                type = ProblemTypes.UPSTREAM_UNAVAILABLE;
                title = "Dịch vụ SOAP hết thời gian chờ";
            }
            case UPSTREAM_UNAVAILABLE -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                type = ProblemTypes.UPSTREAM_UNAVAILABLE;
                title = "Dịch vụ SOAP không khả dụng";
            }
            case UPSTREAM_ERROR -> {
                status = HttpStatus.BAD_GATEWAY;
                type = ProblemTypes.UPSTREAM_UNAVAILABLE;
                title = "Lỗi từ dịch vụ SOAP";
            }
            default -> throw new IllegalStateException(
                    "Loại lỗi chưa được xử lý: " + e.kind());
        }

        ProblemDetail p = problem(status, type, title, e.getMessage());
        p.setInstance(URI.create(request.getRequestURI()));
        p.setProperty("legacyCode", e.legacyCode());
        return p;
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail onInvalid(
            InvalidRequestException e,
            HttpServletRequest request) {

        ProblemDetail p = problem(
                HttpStatus.BAD_REQUEST,
                ProblemTypes.VALIDATION_FAILED,
                "Yêu cầu không hợp lệ",
                e.getMessage());

        p.setInstance(URI.create(request.getRequestURI()));
        return p;
    }

    private static ProblemDetail problem(
            HttpStatus status,
            URI type,
            String title,
            String detail) {

        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setType(type);
        p.setTitle(title);
        return p;
    }
}

