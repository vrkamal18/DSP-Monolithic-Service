package com.adc.adcampaign.openrtb.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Minimal, vendor-neutral OpenRTB 2.5/2.6-compatible bid response.
 */
@Data
@Builder
public class BidResponse {

    private String id;
    private String bidid;
    private String cur;
    private List<SeatBid> seatbid;

    @Data
    @Builder
    public static class SeatBid {
        private String seat;
        private List<Bid> bid;
    }

    @Data
    @Builder
    public static class Bid {
        private String id;
        private String impid;
        private double price;
        private String adid;
        private String nurl;
        private String burl;
        private String adm;
        private List<String> adomain;
        private String cid;
        private String crid;
        private int w;
        private int h;
    }
}
