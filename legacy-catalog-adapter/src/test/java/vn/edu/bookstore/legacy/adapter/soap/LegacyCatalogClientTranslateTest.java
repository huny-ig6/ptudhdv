package vn.edu.bookstore.legacy.adapter.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import org.junit.jupiter.api.Test;
import vn.edu.bookstore.legacy.adapter.soap.generated.BookCatalogFault;
import vn.edu.bookstore.legacy.adapter.soap.generated.ErrorDetail;

class LegacyCatalogClientTranslateTest {

    @Test
    void bookNotFoundDuocDichThanhNotFound() {
        ErrorDetail detail = new ErrorDetail();
        detail.setCode("BOOK_NOT_FOUND");
        detail.setMessage("Không tìm thấy sách");

        BookCatalogFault fault = new BookCatalogFault("Không tìm thấy sách", detail);

        LegacyCatalogException result = LegacyCatalogClient.translate(fault);

        assertEquals(LegacyCatalogException.Kind.NOT_FOUND, result.kind());
        assertEquals("BOOK_NOT_FOUND", result.legacyCode());
        assertSame(fault, result.getCause());
    }

    @Test
    void invalidIsbnDuocDichThanhInvalidRequest() {
        ErrorDetail detail = new ErrorDetail();
        detail.setCode("INVALID_ISBN");
        detail.setMessage("ISBN không hợp lệ");

        BookCatalogFault fault = new BookCatalogFault("ISBN không hợp lệ", detail);

        LegacyCatalogException result = LegacyCatalogClient.translate(fault);

        assertEquals(LegacyCatalogException.Kind.INVALID_REQUEST, result.kind());
        assertEquals("INVALID_ISBN", result.legacyCode());
    }

    @Test
    void loiSoapKhongXacDinhDuocDichThanhUpstreamError() {
        ErrorDetail detail = new ErrorDetail();
        detail.setCode("INTERNAL_ERROR");
        detail.setMessage("Lỗi hệ thống");

        BookCatalogFault fault = new BookCatalogFault("Lỗi hệ thống", detail);

        LegacyCatalogException result = LegacyCatalogClient.translate(fault);

        assertEquals(LegacyCatalogException.Kind.UPSTREAM_ERROR, result.kind());
    }

    @Test
    void loiKetNoiSoapDuocDichThanhUpstreamUnavailable() {
        WebServiceException cause = new WebServiceException("Connection refused");

        LegacyCatalogException result = LegacyCatalogClient.unavailable(cause);

        assertEquals(LegacyCatalogException.Kind.UPSTREAM_UNAVAILABLE, result.kind());
        assertEquals("UPSTREAM_UNAVAILABLE", result.legacyCode());
        assertSame(cause, result.getCause());
    }
}

