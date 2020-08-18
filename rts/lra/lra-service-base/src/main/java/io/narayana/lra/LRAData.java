package io.narayana.lra;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class LRAData {
    private String lraId;
    private String clientId;
    private String status;
    private boolean isClosed;
    private boolean isCancelled;
    private boolean isRecovering;
    private boolean isActive;
    private boolean isTopLevel;
    private long startTime;
    private long finishTime;

    public LRAData() {}

    public LRAData(String lraId, String clientId, String status,
                           boolean isClosed, boolean isCancelled, boolean isRecovering,
                           boolean isActive, boolean isTopLevel,
                           long startTime, long finishTime) {
        this.lraId = lraId;
        this.clientId = clientId;
        this.status = status;
        this.isClosed = isClosed;
        this.isCancelled = isCancelled;
        this.isRecovering = isRecovering;
        this.isActive = isActive;
        this.isTopLevel = isTopLevel;
        this.startTime = startTime;
        this.finishTime = finishTime;
    }

    public String getLraId() {
        return this.lraId;
    }

    public void setLraId(String lraId) {
        this.lraId = lraId;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public boolean isRecovering() {
        return this.isRecovering;
    }

    public void setRecovering(boolean recovering) {
        isRecovering = recovering;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isTopLevel() {
        return this.isTopLevel;
    }

    public void setTopLevel(boolean topLevel) {
        isTopLevel = topLevel;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof LRAData)) {
            return false;
        } else {
            LRAData lraData = (LRAData) o;
            return this.getLraId().equals(lraData.getLraId());
        }
    }

    public int hashCode() {
        return this.getLraId().hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "lraId='" + lraId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", status='" + status + '\'' +
                ", isClosed=" + isClosed +
                ", isCancelled=" + isCancelled +
                ", isRecovering=" + isRecovering +
                ", isActive=" + isActive +
                ", isTopLevel=" + isTopLevel +
                ", startTime=" + startTime +
                ", finishTime=" + finishTime +
                '}';
    }

}
