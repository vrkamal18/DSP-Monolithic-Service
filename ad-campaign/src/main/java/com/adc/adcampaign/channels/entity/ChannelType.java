package com.adc.adcampaign.channels.entity;

/**
 * The adapter registry from service/documents/02-module-specifications.md
 * §10 — "Display, native, video, audio, OTT/CTV, DOOH, and social adapter
 * registry." Actually pushing to a real social/OTT platform is Connectors'
 * job (not built); this is the type registry Channels owns.
 */
public enum ChannelType {
    DISPLAY,
    NATIVE,
    VIDEO,
    AUDIO,
    OTT_CTV,
    DOOH,
    SOCIAL
}
