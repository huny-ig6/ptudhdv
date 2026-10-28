package vn.edu.bookstore.legacy.adapter.api;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.bookstore.legacy.adapter.soap.LegacyCatalogClient;

/** API REST bọc dịch vụ SOAP kế thừa: /api/v1/legacy/books */
@RestController
@RequestMapping("/api/v1/legacy/books")
public class LegacyBookController {

    private static final Pattern ISBN_PATTERN =
            Pattern.compile("^97[89][0-9]{10}$");

    private final LegacyCatalogClient client;

    public LegacyBookController(LegacyCatalogClient client) {
        this.client = client;
    }

    @GetMapping("/{isbn}")
    public BookView getBook(@PathVariable String isbn) {
        if (!ISBN_PATTERN.matcher(isbn).matches()) {
            throw new InvalidRequestException(
                    "ISBN không hợp lệ; ISBN phải gồm 13 chữ số và bắt đầu bằng 978 hoặc 979");
        }

        return BookView.from(client.getBook(isbn));
    }

    @GetMapping
    public List<BookView> search(
            @RequestParam("q") String keyword,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        if (keyword == null || keyword.trim().length() < 2) {
            throw new InvalidRequestException(
                    "Từ khóa tìm kiếm phải có ít nhất 2 ký tự");
        }

        if (limit < 1 || limit > 50) {
            throw new InvalidRequestException(
                    "limit phải nằm trong khoảng từ 1 đến 50");
        }

        return client.search(keyword.trim(), limit)
                .stream()
                .map(BookView::from)
                .toList();
    }
}
