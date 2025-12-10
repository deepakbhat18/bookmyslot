package com.college.bookmyslot.dto;

public class TeacherUsageDto {

    private Long teacherId;
    private String teacherName;
    private long totalSlots;
    private long totalBookings;

    public Long getTeacherId() { return teacherId; }

    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }

    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public long getTotalSlots() { return totalSlots; }

    public void setTotalSlots(long totalSlots) { this.totalSlots = totalSlots; }

    public long getTotalBookings() { return totalBookings; }

    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }
}
