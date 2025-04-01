package com.adc.adcampaign.campaign.dto.openrtb;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpenRTBBidResponseDTO {
    private String id; // Bid response ID

    private SeatBid seatbid;

    @Data
    @Builder
    public static class SeatBid {
        private List<Bid> bid;

        @Data
        @Builder
        public static class Bid {
            private String id;
            private String impid;
            private double price; // e.g., $0.05
            private String adm;   // Ad markup or creative URL
            private int w;
            private int h;
        }
    }
}
