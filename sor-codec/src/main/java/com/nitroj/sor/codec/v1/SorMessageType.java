package com.nitroj.sor.codec.v1;

/** Protocol v1 message template identifiers. */
public enum SorMessageType {
    SubmitParentOrder(1),
    CancelParentOrder(2),
    GetOrderStatusRequest(3),
    OrderStatusReply(4),
    WarmupRequest(5),
    WarmupComplete(6),
    IsReadyRequest(7),
    IsReadyReply(8),
    ActivePolicyRequest(9),
    ActivePolicyReply(10),
    RouteDecidedEvent(11),
    ChildOrderEmittedEvent(12),
    FilledEvent(13),
    RejectedEvent(14),
    PolicyPublishedEvent(15),
    SessionStatusChangedEvent(16),
    BackpressureRejectedEvent(17);

    private final int templateId;

    SorMessageType(final int templateId) {
        this.templateId = templateId;
    }

    public int templateId() {
        return templateId;
    }

    public static SorMessageType fromTemplateId(final int templateId) {
        for (SorMessageType type : values()) {
            if (type.templateId == templateId) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown template id " + templateId);
    }
}
