package vn.edu.bookstore.legacy.adapter.soap;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import java.net.SocketTimeoutException;
import java.util.List;
import org.springframework.stereotype.Component;
import vn.edu.bookstore.legacy.adapter.soap.generated.Book;
import vn.edu.bookstore.legacy.adapter.soap.generated.BookCatalogFault;
import vn.edu.bookstore.legacy.adapter.soap.generated.BookCatalogPortType;
import vn.edu.bookstore.legacy.adapter.soap.generated.GetBookRequest;
import vn.edu.bookstore.legacy.adapter.soap.generated.SearchBooksRequest;

/**
 * Tầng chống ăn mòn (anti-corruption layer): gọi BookCatalogService và chuyển
 * kiểu dữ liệu + lỗi SOAP sang mô hình nội bộ LegacyBook / LegacyCatalogException.
 */
@Component
public class LegacyCatalogClient {

    private final BookCatalogPortType port;

    public LegacyCatalogClient(BookCatalogPortType port) {
        this.port = port;
    }

    public LegacyBook getBook(String isbn) {
        GetBookRequest request = new GetBookRequest();
        request.setIsbn(isbn);

        try {
            Book book = port.getBook(request).getBook();
            if (book == null) {
                throw new LegacyCatalogException(
                        LegacyCatalogException.Kind.UPSTREAM_ERROR,
                        "EMPTY_RESPONSE",
                        "Dịch vụ SOAP không trả về thông tin sách",
                        null);
            }
            return toModel(book);
        } catch (BookCatalogFault e) {
            throw translate(e);
        } catch (SOAPFaultException e) {
            throw translate(e);
        } catch (WebServiceException e) {
            throw unavailable(e);
        }
    }

    public List<LegacyBook> search(String keyword, int maxResults) {
        SearchBooksRequest request = new SearchBooksRequest();
        request.setKeyword(keyword);
        request.setMaxResults(maxResults);

        try {
            return port.searchBooks(request).getBook()
                    .stream()
                    .map(LegacyCatalogClient::toModel)
                    .toList();
        } catch (BookCatalogFault e) {
            throw translate(e);
        } catch (SOAPFaultException e) {
            throw translate(e);
        } catch (WebServiceException e) {
            throw unavailable(e);
        }
    }

    /** BOOK_NOT_FOUND → NOT_FOUND; INVALID_ISBN/INVALID_REQUEST → INVALID_REQUEST; còn lại → UPSTREAM_ERROR. */
    static LegacyCatalogException translate(BookCatalogFault fault) {
        var detail = fault.getFaultInfo();
        String code = detail == null || detail.getCode() == null
                ? "UNKNOWN_FAULT" : detail.getCode();
        String message = detail == null || detail.getMessage() == null
                ? "Dịch vụ SOAP trả về lỗi không xác định" : detail.getMessage();

        LegacyCatalogException.Kind kind = switch (code) {
            case "BOOK_NOT_FOUND" -> LegacyCatalogException.Kind.NOT_FOUND;
            case "INVALID_ISBN", "INVALID_REQUEST" ->
                    LegacyCatalogException.Kind.INVALID_REQUEST;
            default -> LegacyCatalogException.Kind.UPSTREAM_ERROR;
        };

        return new LegacyCatalogException(kind, code, message, fault);
    }

    /** faultcode Client → INVALID_REQUEST, Server → UPSTREAM_ERROR. */
    static LegacyCatalogException translate(SOAPFaultException e) {
        String faultCode = e.getFault() == null ? null : e.getFault().getFaultCode();
        String message = e.getFault() == null
                ? "Dịch vụ SOAP trả về SOAP Fault" : e.getFault().getFaultString();

        boolean clientFault = faultCode != null
                && (faultCode.endsWith(":Client") || faultCode.equals("Client")
                || faultCode.endsWith(":Sender") || faultCode.equals("Sender"));

        return new LegacyCatalogException(
                clientFault
                        ? LegacyCatalogException.Kind.INVALID_REQUEST
                        : LegacyCatalogException.Kind.UPSTREAM_ERROR,
                faultCode == null ? "SOAP_FAULT" : faultCode,
                message,
                e);
    }

    /** Phân biệt hết thời gian chờ (SocketTimeoutException) với không kết nối được. */
    static LegacyCatalogException unavailable(WebServiceException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return new LegacyCatalogException(
                        LegacyCatalogException.Kind.UPSTREAM_TIMEOUT,
                        "UPSTREAM_TIMEOUT",
                        "Hết thời gian chờ phản hồi từ dịch vụ SOAP",
                        e);
            }
            cause = cause.getCause();
        }

        return new LegacyCatalogException(
                LegacyCatalogException.Kind.UPSTREAM_UNAVAILABLE,
                "UPSTREAM_UNAVAILABLE",
                "Không thể kết nối đến dịch vụ SOAP",
                e);
    }

    /** Ánh xạ kiểu SOAP → mô hình nội bộ. */
    static LegacyBook toModel(Book b) {
        Integer authorId = b.getAuthorId() == null ? null : b.getAuthorId().intValue();
        Integer year = b.getPublishedYear() == null ? null : b.getPublishedYear().getYear();
        return new LegacyBook(b.getIsbn(), b.getTitle(), b.getAuthor(), authorId, year,
                b.getPrice().getAmount(), b.getPrice().getCurrency().value());
    }
}
