package com.adc.adcampaign.exchange.bidscube;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class BidsCubeAuthFilterTest {

    private static final String HEADER = "X-BidsCube-Auth";
    private static final String SECRET = "s3cret";

    private BidsCubeAuthFilter filter;

    @BeforeEach
    void setUp() {
        BidsCubeProperties properties = new BidsCubeProperties();
        properties.setAuthHeaderName(HEADER);
        properties.setAuthSharedSecret(SECRET);
        properties.setMaxRequestBodyBytes(1024);
        filter = new BidsCubeAuthFilter(properties);
    }

    @Test
    void rejectsRequestMissingAuthHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/exchange/bidscube/openrtb2/bidrequest");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void rejectsRequestWithWrongSecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/exchange/bidscube/openrtb2/bidrequest");
        request.addHeader(HEADER, "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void rejectsOversizedRequestBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/exchange/bidscube/openrtb2/bidrequest");
        request.addHeader(HEADER, SECRET);
        request.setContent(new byte[2048]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        verifyNoInteractions(chain);
    }

    @Test
    void allowsRequestWithCorrectSecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/exchange/bidscube/openrtb2/bidrequest");
        request.addHeader(HEADER, SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
