package com.college.bookmyslot.dto;

public class CreateSlotRequest {

    private Long teacherId;
    private String date;       // "2025-12-10"
    private String startTime;  // "10:00"
    private String endTime;    // "10:30"

    public Long getTeacherId() { return teacherId; }

    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public String getDate() { return date; }

    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }

    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }

    public void setEndTime(String endTime) { this.endTime = endTime; }
}
