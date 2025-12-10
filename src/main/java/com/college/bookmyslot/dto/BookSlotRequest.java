package com.college.bookmyslot.dto;

public class BookSlotRequest {

    private Long slotId;
    private Long studentId;

    public Long getSlotId() { return slotId; }

    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public Long getStudentId() { return studentId; }

    public void setStudentId(Long studentId) { this.studentId = studentId; }
}
