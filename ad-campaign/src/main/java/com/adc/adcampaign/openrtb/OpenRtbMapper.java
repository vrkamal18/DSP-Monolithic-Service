package com.adc.adcampaign.openrtb;

import com.adc.adcampaign.bidding.model.AuctionRequest;
import com.adc.adcampaign.bidding.model.BidDecision;
import com.adc.adcampaign.bidding.model.ImpressionOpportunity;
import com.adc.adcampaign.openrtb.dto.BidRequest;
import com.adc.adcampaign.openrtb.dto.BidResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Translates between the OpenRTB wire format and the internal, vendor-neutral
 * auction model. Business modules (bidding) never see {@link BidRequest} /
 * {@link BidResponse} directly.
 */
@Component
public class OpenRtbMapper {

    public AuctionRequest toAuctionRequest(BidRequest request, String exchange, Long tenantId,
                                            long defaultTmaxMillis, long deadlineNanos) {
        List<ImpressionOpportunity> impressions = request.getImp().stream()
                .map(imp -> ImpressionOpportunity.builder()
                        .impId(imp.getId())
                        .width(imp.getBanner() != null ? imp.getBanner().getW() : null)
                        .height(imp.getBanner() != null ? imp.getBanner().getH() : null)
                        .bidFloor(imp.getBidfloor() != null ? BigDecimal.valueOf(imp.getBidfloor()) : null)
                        .bidFloorCurrency(imp.getBidfloorcur())
                        .build())
                .collect(Collectors.toList());

        String deviceType = request.getDevice() != null ? request.getDevice().getDevicetype() : null;
        String geoCountry = request.getDevice() != null && request.getDevice().getGeo() != null
                ? request.getDevice().getGeo().getCountry()
                : null;
        String userId = request.getUser() != null ? request.getUser().getId() : null;
        long tmax = request.getTmax() != null ? request.getTmax() : defaultTmaxMillis;

        return AuctionRequest.builder()
                .requestId(request.getId())
                .exchange(exchange)
                .tenantId(tenantId)
                .impressions(impressions)
                .tmaxMillis(tmax)
                .currencies(request.getCur())
                .userId(userId)
                .deviceType(deviceType)
                .geoCountry(geoCountry)
                .deadlineNanos(deadlineNanos)
                .build();
    }

    public BidResponse toBidResponse(String requestId, String seat, BidDecision decision, String burl) {
        BidResponse.Bid bid = BidResponse.Bid.builder()
                .id(UUID.randomUUID().toString())
                .impid(decision.getImpId())
                .price(decision.getPrice().doubleValue())
                .adid(String.valueOf(decision.getCreativeId()))
                .crid(String.valueOf(decision.getCreativeId()))
                .cid(String.valueOf(decision.getCampaignId()))
                .adm(decision.getAdMarkup())
                .burl(burl)
                .w(decision.getWidth())
                .h(decision.getHeight())
                .build();

        BidResponse.SeatBid seatBid = BidResponse.SeatBid.builder()
                .seat(seat)
                .bid(List.of(bid))
                .build();

        return BidResponse.builder()
                .id(requestId)
                .bidid(UUID.randomUUID().toString())
                .cur("USD")
                .seatbid(List.of(seatBid))
                .build();
    }
}
