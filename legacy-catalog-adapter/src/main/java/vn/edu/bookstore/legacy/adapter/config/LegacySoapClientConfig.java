package vn.edu.bookstore.legacy.adapter.config;

import jakarta.xml.ws.BindingProvider;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.bookstore.legacy.adapter.soap.SoapLoggingHandler;
import vn.edu.bookstore.legacy.adapter.soap.generated.BookCatalogPortType;
import vn.edu.bookstore.legacy.adapter.soap.generated.BookCatalogService;

@Configuration
public class LegacySoapClientConfig {

    /** Thuộc tính timeout của Metro (Jakarta XML WS RI), đơn vị mili giây. */
    static final String CONNECT_TIMEOUT = "com.sun.xml.ws.connect.timeout";
    static final String REQUEST_TIMEOUT = "com.sun.xml.ws.request.timeout";

    @Bean
    BookCatalogPortType bookCatalogPort(LegacySoapProperties props) {
        URL wsdl = LegacySoapClientConfig.class.getResource("/wsdl/BookCatalogService.wsdl");
        BookCatalogPortType port = new BookCatalogService(wsdl).getBookCatalogPort();

        BindingProvider bindingProvider = (BindingProvider) port;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();

        requestContext.put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                props.endpoint());

        requestContext.put(
                CONNECT_TIMEOUT,
                (int) props.connectTimeout().toMillis());

        requestContext.put(
                REQUEST_TIMEOUT,
                (int) props.readTimeout().toMillis());

        if (props.logMessages()) {
            List<jakarta.xml.ws.handler.Handler> handlerChain =
                    new ArrayList<>(bindingProvider.getBinding().getHandlerChain());

            handlerChain.add(new SoapLoggingHandler());

            bindingProvider.getBinding().setHandlerChain(handlerChain);
        }

        return port;
    }
}
