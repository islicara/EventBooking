package com.isli.dto;

import com.isli.entity.WaitlistStatus;
import lombok.Builder;

@Builder
public class WaitlistDTO {
    private Long id;
    private WaitlistStatus status;
    private Long position;
    private Long eventId;
    private Long userId;

    public WaitlistDTO() {}

    public WaitlistDTO(Long id, WaitlistStatus status, Long position, Long eventId, Long userId) {
        this.id = id;
        this.status = status;
        this.position = position;
        this.eventId = eventId;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
