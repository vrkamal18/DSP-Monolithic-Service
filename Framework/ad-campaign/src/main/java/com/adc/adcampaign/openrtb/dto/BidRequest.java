package com.adc.adcampaign.openrtb.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Minimal, vendor-neutral OpenRTB 2.5/2.6-compatible bid request. Only the
 * fields the bidding pipeline actually consumes are modeled; unknown fields
 * are ignored by Jackson rather than rejected.
 */
@Data
public class BidRequest {

    @NotBlank
    private String id;

    @NotEmpty
    @Valid
    private List<Imp> imp;

    @Valid
    private Site site;

    @Valid
    private App app;

    @Valid
    private Device device;

    @Valid
    private User user;

    /** Auction type: 1 = first price, 2 = second price. */
    private Integer at;

    /** Maximum time, in milliseconds, the exchange allows for a response. */
    private Long tmax;

    private List<String> cur;

    private Map<String, Object> ext;

    @Data
    public static class Imp {
        @NotBlank
        private String id;

        @Valid
        private Banner banner;

        private Double bidfloor;

        private String bidfloorcur;
    }

    @Data
    public static class Banner {
        private Integer w;
        private Integer h;
        private Integer pos;
        private List<String> mimes;
    }

    @Data
    public static class Site {
        private String id;
        private String domain;
        private String page;
        private Publisher publisher;
    }

    @Data
    public static class App {
        private String id;
        private String bundle;
        private Publisher publisher;
    }

    @Data
    public static class Publisher {
        private String id;
        private String name;
    }

    @Data
    public static class Device {
        private String ua;
        private String ip;
        private Geo geo;
        private String devicetype;
        private String os;
        private String make;
        private String model;
    }

    @Data
    public static class Geo {
        private String country;
        private String region;
        private String city;
    }

    @Data
    public static class User {
        private String id;
        private String buyeruid;
        private Integer yob;
        private String gender;
    }
}
