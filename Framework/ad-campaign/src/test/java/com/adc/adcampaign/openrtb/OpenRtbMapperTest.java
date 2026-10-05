package com.adc.adcampaign.openrtb;

import com.adc.adcampaign.bidding.model.AuctionRequest;
import com.adc.adcampaign.bidding.model.BidDecision;
import com.adc.adcampaign.openrtb.dto.BidRequest;
import com.adc.adcampaign.openrtb.dto.BidResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRtbMapperTest {

    private final OpenRtbMapper mapper = new OpenRtbMapper();

    @Test
    void mapsRequestFieldsIntoNormalizedAuctionRequest() {
        BidRequest request = new BidRequest();
        request.setId("req-1");
        request.setTmax(120L);

        BidRequest.Imp imp = new BidRequest.Imp();
        imp.setId("imp-1");
        BidRequest.Banner banner = new BidRequest.Banner();
        banner.setW(300);
        banner.setH(250);
        imp.setBanner(banner);
        imp.setBidfloor(0.5);
        request.setImp(List.of(imp));

        BidRequest.Device device = new BidRequest.Device();
        device.setDevicetype("mobile");
        BidRequest.Geo geo = new BidRequest.Geo();
        geo.setCountry("US");
        device.setGeo(geo);
        request.setDevice(device);

        BidRequest.User user = new BidRequest.User();
        user.setId("user-1");
        request.setUser(user);

        long deadline = System.nanoTime() + 1_000_000L;
        AuctionRequest auctionRequest = mapper.toAuctionRequest(request, "bidscube", 10L, 150, deadline);

        assertThat(auctionRequest.getRequestId()).isEqualTo("req-1");
        assertThat(auctionRequest.getExchange()).isEqualTo("bidscube");
        assertThat(auctionRequest.getTmaxMillis()).isEqualTo(120L);
        assertThat(auctionRequest.getUserId()).isEqualTo("user-1");
        assertThat(auctionRequest.getDeviceType()).isEqualTo("mobile");
        assertThat(auctionRequest.getGeoCountry()).isEqualTo("US");
        assertThat(auctionRequest.getDeadlineNanos()).isEqualTo(deadline);
        assertThat(auctionRequest.getImpressions()).hasSize(1);
        assertThat(auctionRequest.getImpressions().get(0).getImpId()).isEqualTo("imp-1");
        assertThat(auctionRequest.getImpressions().get(0).getWidth()).isEqualTo(300);
        assertThat(auctionRequest.getImpressions().get(0).getHeight()).isEqualTo(250);
        assertThat(auctionRequest.getImpressions().get(0).getBidFloor()).isEqualByComparingTo("0.5");
    }

    @Test
    void fallsBackToDefaultTmaxWhenRequestOmitsIt() {
        BidRequest request = new BidRequest();
        request.setId("req-2");
        BidRequest.Imp imp = new BidRequest.Imp();
        imp.setId("imp-1");
        request.setImp(List.of(imp));

        AuctionRequest auctionRequest = mapper.toAuctionRequest(request, "bidscube", 10L, 150, System.nanoTime());

        assertThat(auctionRequest.getTmaxMillis()).isEqualTo(150L);
    }

    @Test
    void buildsSpecShapedBidResponseWithSeatAndBurl() {
        BidDecision decision = BidDecision.builder()
                .impId("imp-1")
                .campaignId(7L)
                .creativeId(42L)
                .price(BigDecimal.valueOf(1.25))
                .adMarkup("https://cdn.example/creative.jpg")
                .width(300)
                .height(250)
                .build();

        BidResponse response = mapper.toBidResponse("req-1", "bidscube",
                decision, "https://example.com/win?price=${AUCTION_PRICE}");

        assertThat(response.getId()).isEqualTo("req-1");
        assertThat(response.getSeatbid()).hasSize(1);
        BidResponse.SeatBid seatBid = response.getSeatbid().get(0);
        assertThat(seatBid.getSeat()).isEqualTo("bidscube");
        assertThat(seatBid.getBid()).hasSize(1);
        BidResponse.Bid bid = seatBid.getBid().get(0);
        assertThat(bid.getImpid()).isEqualTo("imp-1");
        assertThat(bid.getPrice()).isEqualTo(1.25);
        assertThat(bid.getCid()).isEqualTo("7");
        assertThat(bid.getCrid()).isEqualTo("42");
        assertThat(bid.getBurl()).isEqualTo("https://example.com/win?price=${AUCTION_PRICE}");
    }
}
