package com.college.bookmyslot.dto;

public class DailyCountDto {

    private String date; // "2025-12-10"
    private long count;

    public DailyCountDto() {}

    public DailyCountDto(String date, long count) {
        this.date = date;
        this.count = count;
    }

    public String getDate() { return date; }

    public void setDate(String date) { this.date = date; }

    public long getCount() { return count; }

    public void setCount(long count) { this.count = count; }
}
