
package com.college.bookmyslot.dto;

import lombok.Data;
import java.time.LocalDateTime;

    @Data
    public class EventCheckInResponse {

        private String ticketId;
        private String studentName;
        private String eventTitle;

        private boolean checkedIn;
        private LocalDateTime checkInTime;

        public void setMessage(String entryAllowed) {

        }
    }


