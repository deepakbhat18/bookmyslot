# 📅 BookMySlot — College Slot & Event Booking Backend

> A full-featured, production-deployed RESTful backend for managing teacher-student slot bookings and college club event management — built with Spring Boot 3, MySQL,and ZXing QR.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=java)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.x-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?logo=mysql)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)

---

## 🧠 Architecture Overview

BookMySlot follows a **Layered Monolithic Architecture** — specifically the **Controller → Service → Repository** (CSR) pattern, which is a clean, horizontally scalable approach well-suited for a domain-scoped application of this complexity.

### Architectural Layers

```
┌──────────────────────────────────────────────────────────────────┐
│                        CLIENT (React)                            │
└───────────────────────────────┬──────────────────────────────────┘
                                │ HTTP / JSON
┌───────────────────────────────▼──────────────────────────────────┐
│                      CONTROLLER LAYER                            │
│  AuthController │ SlotController │ EventController │ AdminCtrl   │
│  PaymentController │ AnalyticsController │ CalendarController    │
│  (Maps HTTP verbs → DTOs → delegates to Service Layer)           │
└───────────────────────────────┬──────────────────────────────────┘
                                │
┌───────────────────────────────▼──────────────────────────────────┐
│                       SERVICE LAYER                              │
│  EventBookingService │ PaymentService │ EventService             │
│  SlotBookingService │ EventCheckInService │ EmailService         │
│  (Business logic, @Transactional, domain validation)             │
└───────────────────────────────┬──────────────────────────────────┘
                                │
┌───────────────────────────────▼──────────────────────────────────┐
│                     REPOSITORY LAYER (JPA)                       │
│  UserRepository │ EventRepository │ SlotBookingRepository        │
│  EventBookingRepository │ PaymentRepository │ etc.               │
│  (Spring Data JPA — JPQL + custom queries via @Query)            │
└───────────────────────────────┬──────────────────────────────────┘
                                │
┌───────────────────────────────▼──────────────────────────────────┐
│                    MySQL Database                                │
└──────────────────────────────────────────────────────────────────┘
```

### Request Flow

```
Client Request
    → SessionAuthInterceptor (preHandle — validates HttpSession)
        → @RestController (validates DTO, extracts session attrs)
            → @Service (transactional business logic, domain guards)
                → @Repository (JPA → Hibernate → MySQL)
                    ← Entity / DTO
                ← DTO mapped via mapper methods
            ← ApiResponse<T> wrapper
        ← JSON response
    ← HTTP 200 / 4xx
```

### Key Design Decisions

- **Session-based auth over JWT** — uses `HttpSession` with server-side session storage, suitable for same-origin deployments with `allowCredentials(true)` CORS config.
- **DTO separation** — all request/response objects are explicit DTOs; JPA entities never leak to the API boundary.
- **`@Transactional` on booking** — event booking uses `SELECT ... FOR UPDATE` (`findByIdForUpdate`) to prevent double-booking under concurrent requests.
- **`@EnableAsync` + `@EnableRetry`** — email dispatch is fire-and-forget with retry semantics, ensuring booking confirmation is non-blocking and resilient.
- **`@EnableScheduling`** — event reminder emails are dispatched via a `@Scheduled` job that polls bookings starting within the next hour.

---

## 🚀 Features

### 👩‍🎓 Student Features
- Self-registration with **USN** (University Seat Number) and email OTP verification (10-minute expiry)
- Email-verified login via server-side session
- Forgot password / reset password via OTP flow
- Browse **published events** (paginated) without authentication
- Book **free events** with automatic duplicate and **time-conflict detection** (cross-checks both event bookings and teacher slot bookings)
- Book **paid events** via Razorpay payment order → HMAC-SHA256 signature verification → auto-confirm booking
- Cancel bookings with smart **refund status logic** (REFUNDED if cancelled before event start, NOT_ELIGIBLE after)
- View **My Bookings** including QR code (Base64 PNG) for each ticket
- View personal **calendar** of all slot and event bookings
- Receive **automated email reminders** 1 hour before an event

### 👨‍🏫 Teacher Features
- Register as `TEACHER` role
- Create available **time slots** (date + start/end time)
- View **calendar** of all own slots and who has booked them

### 🏛️ Club Staff Features
- Admin-provisioned accounts (auto-verified, credentials emailed)
- Create and manage **events** (title, description, venue, date/time, total slots, FREE/PAID, registration deadline, event poster upload)
- Upload event **poster images** (stored on filesystem, served via static resource handler)
- Perform **QR-based check-in** at the venue — validates ticket, enforces club ownership, prevents re-entry
- View bookings and attendance per event

### 🔑 Admin Features
- Create and manage **clubs** (name, description, email, status)
- Provision **club staff** accounts with welcome email including credentials
- Deactivate club staff (enforces one active staff per club)
- Admin-initiated **password resets** for staff via OTP
- **Approve or reject** events submitted by clubs (DRAFT → PUBLISHED / CANCELLED)
- View **system-wide analytics**: total users, students, teachers, slots, bookings
- View **daily booking trends** (last N days, configurable)
- View **teacher utilization** (slots created vs. booked per teacher)
- View **per-event analytics**: seats sold, revenue, check-in rate, attendance percentage


## 📂 Project Structure

```
src/main/java/com/college/bookmyslot/
│
├── BookmyslotApplication.java       # Entry point; enables Scheduling, Async, Retry
│
├── config/
│   └── WebConfig.java               # CORS config (localhost + Vercel), static resource
│                                    # handler for /uploads/**, interceptor registration
│
├── interceptor/
│   └── SessionAuthInterceptor.java  # Pre-handle filter: validates HttpSession for all
│                                    # /api/** routes, whitelists /api/auth & /api/public
│
├── controller/                      # HTTP layer — thin, delegates to service/repo
│   ├── AuthController.java          # Register, OTP verify, login, logout, forgot/reset pwd
│   ├── SlotController.java          # Teacher slot CRUD, student slot booking
│   ├── EventController.java         # Club staff: create/update events, poster upload
│   ├── PublicEventController.java   # Unauthenticated: paginated published event listing
│   ├── EventBookingController.java  # Book free event, cancel booking, my bookings
│   ├── EventCheckInController.java  # QR-based venue check-in
│   ├── PaymentController.java       # Razorpay order creation + signature verification
│   ├── CalendarController.java      # Teacher & student calendar views
│   ├── NotificationController.java  # Per-user notifications + unread count
│   ├── AdminClubController.java     # Admin: club CRUD, club staff creation
│   ├── AdminEventController.java    # Admin: pending events, approve/reject
│   ├── AdminStaffController.java    # Admin: staff CRUD, deactivate, reset password
│   ├── AnalyticsController.java     # Admin: user/slot/booking overview + daily chart data
│   └── EventAnalyticsController.java# Admin: per-event revenue, attendance analytics
│
├── service/                         # Business logic layer
│   ├── EventBookingService.java     # Core booking logic: conflict detection, seat locking,
│   │                                # UUID ticket generation, QR code embedding, email dispatch
│   ├── PaymentService.java          # Razorpay order creation + HMAC-SHA256 verification
│   ├── EventService.java            # Event lifecycle management
│   ├── SlotBookingService.java      # Teacher slot booking + status transitions
│   ├── EventCheckInService.java     # Ticket validation + club ownership check
│   └── EmailService.java            # Transactional email templates (OTP, booking, reminder,
│                                    # welcome, staff credentials) via JavaMailSender
│
├── model/                           # JPA entities (database schema)
│   ├── User.java                    # Roles: STUDENT, TEACHER, CLUB, ADMIN; OTP fields; USN
│   ├── Club.java                    # College club with status (ACTIVE/INACTIVE)
│   ├── TeacherSlot.java             # Slot with AVAILABLE / BOOKED / BLOCKED status
│   ├── SlotBooking.java             # Student ↔ TeacherSlot mapping; BOOKED/CANCELLED
│   ├── Event.java                   # Full event entity: type (FREE/PAID),
│   │                                # status (DRAFT/APPROVED/PUBLISHED/CANCELLED), poster
│   ├── EventBooking.java            # Student ↔ Event mapping: ticketId, paid, checkedIn,
│   │                                # BookingStatus, RefundStatus
│   ├── EventTicket.java             # Ticket with QR data, PaymentStatus, CheckInStatus
│   ├── Payment.java                 # Razorpay order tracking: orderId, paymentId, status
│   └── Notification.java            # Per-user in-app notification with read/unread flag
│
├── repository/                      # Spring Data JPA interfaces
│   ├── UserRepository.java          # findByEmail, existsByClubAndActive
│   ├── EventRepository.java         # findByStatus, findByStatusOrderByEventDateAsc (pageable),
│   │                                # findByIdForUpdate (SELECT FOR UPDATE)
│   ├── EventBookingRepository.java  # existsOverlappingEventBooking (custom JPQL),
│   │                                # findUpcomingBookings (for scheduler)
│   ├── SlotBookingRepository.java   # existsOverlappingSlotBooking (custom JPQL)
│   ├── TeacherSlotRepository.java   # findByDateAndStatus, findByTeacher
│   ├── PaymentRepository.java       # findByOrderId, findByEvent
│   ├── ClubRepository.java          # existsByName, existsByEmail
│   ├── EventTicketRepository.java   # uniqueConstraint(event_id, student_id)
│   └── NotificationRepository.java  # findByUserOrderByCreatedAtDesc, countByUserAndReadFlagFalse
│
├── dto/                             # Request/Response DTOs (API contract layer)
│   ├── ApiResponse.java             # Generic wrapper: { success, message, data }
│   ├── RegisterRequest/LoginRequest/LoginResponse
│   ├── OtpVerifyRequest/ResendOtpRequest
│   ├── ForgotPasswordRequest/ResetPasswordRequest
│   ├── CreateSlotRequest/BookSlotRequest
│   ├── EventCreateRequest/EventUpdateRequest/EventResponse/EventListResponse
│   ├── EventBookingRequest/EventBookingResponse/MyEventBookingResponse
│   ├── EventCheckInRequest/EventCheckInResponse
│   ├── PaymentCreateRequest/PaymentResponse/PaymentVerifyRequest
│   ├── CalendarEventDto, NotificationDto, DailyCountDto
│   ├── TeacherUsageDto, EventAnalyticsDto
│   └── PublicEventResponse/PublicEventDetailResponse
│
├── scheduler/
│   └── EventReminderScheduler.java  # @Scheduled(fixedRate=30s) — scans bookings
│                                    # starting within 1 hour, dispatches reminder emails
│
└── exception/
    └── ApiError.java                # Error response envelope
```

---



### Role-Based Access Control

| Role | Capabilities |
|---|---|
| `STUDENT` | Book slots/events, view calendar, cancel bookings, view notifications |
| `TEACHER` | Create/manage availability slots, view own calendar |
| `CLUB` | Create/manage events for their club, check in attendees, upload posters |
| `ADMIN` | Full system access — manage clubs, staff, users, approve events, analytics |



### CORS Configuration

Configured for:
- `http://localhost:5173` (local Vite dev server)
- `https://bookmyslot-frontend-drab.vercel.app` (production frontend)

With `allowCredentials(true)` to support cookie-based session transport.

---

## 🔄 API Design

The API follows **REST conventions** with resource-based URL structure, appropriate HTTP verbs, and a consistent `ApiResponse<T>` envelope for all responses:

```json
{
  "success": true,
  "message": "Event booked successfully",
  "data": { ... }
}
```

### API Reference

#### 🔑 Auth — `/api/auth`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register as STUDENT or TEACHER with OTP verification |
| POST | `/verify-otp` | Verify email OTP |
| POST | `/resend-otp` | Resend OTP |
| POST | `/login` | Login — creates session |
| POST | `/logout` | Invalidate session |
| POST | `/forgot-password` | Send password reset OTP |
| POST | `/reset-password` | Reset password with OTP |
| PUT | `/users/{id}/deactivate` | Deactivate user account |

#### 📅 Teacher Slots — `/api/slots`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/create` | Teacher creates an available slot |
| GET | `/available?date=` | List available slots by date |
| POST | `/book` | Student books a slot |
| GET | `/student/{studentId}` | All bookings for a student |
| GET | `/teacher/{teacherId}` | All bookings for a teacher |

#### 🎉 Events — `/api/events`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Club staff creates event (session-authenticated) |
| PUT | `/{eventId}` | Update event details |
| GET | `/?date=` | List events by date |
| GET | `/{eventId}` | Get event details |
| POST | `/{eventId}/poster` | Upload event poster (multipart/form-data) |
| GET | `/staff/{staffUserId}` | All events for a club staff member |

#### 🌐 Public Events — `/api/public/events` *(no auth required)*
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/?page=&size=` | Paginated published events |
| GET | `/{eventId}` | Published event detail |

#### 🎟️ Event Bookings — `/api/events/bookings`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/book` | Book a free event (with conflict detection) |
| POST | `/cancel/{bookingId}` | Cancel booking (refund status auto-set) |
| GET | `/my` | Authenticated user's bookings with QR codes |

#### ✅ Event Check-In — `/api/events/checkin`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Club staff scans QR and checks in attendee |

#### 💳 Payments — `/api/payments`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/create` | Create Razorpay order for paid event |
| POST | `/verify` | Verify HMAC signature → confirm booking |

#### 🗓️ Calendar — `/api/calendar`
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/teacher/{teacherId}` | All slots + bookings for teacher calendar |
| GET | `/student/{studentId}` | All bookings for student calendar |

#### 🔔 Notifications — `/api/notifications`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/create/{userId}` | Create notification for user |
| GET | `/user/{userId}` | List notifications (newest first) |
| POST | `/user/{userId}/mark-read` | Mark all as read |
| GET | `/user/{userId}/unread-count` | Count of unread notifications |

#### 🔑 Admin — `/api/admin`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/clubs` | Create club |
| GET | `/clubs` | List all clubs |
| PUT | `/clubs/{clubId}/status` | Toggle club status |
| GET | `/clubs/{clubId}/events` | Club event listing |
| POST | `/clubs/staff` | Provision club staff |
| GET | `/staff` | List all club staff |
| GET | `/staff/{staffId}` | Staff details |
| PUT | `/staff/{staffId}/deactivate` | Deactivate staff |
| POST | `/staff/{staffId}/reset-password` | Trigger staff OTP reset |
| GET | `/events/pending` | List DRAFT events awaiting approval |
| PUT | `/events/{eventId}/approve` | Approve → PUBLISHED |
| PUT | `/events/{eventId}/reject` | Reject → CANCELLED |
| GET | `/analytics/overview` | System-wide stats |
| GET | `/analytics/daily-bookings?days=` | Daily booking trend |
| GET | `/analytics/teacher-usage` | Teacher slot utilization |
| GET | `/event-analytics` | All events: revenue, attendance rate |
| GET | `/event-analytics/{eventId}` | Per-event analytics |


---

## ⚙️ Setup Instructions

### Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.x running locally
- A Gmail account with App Password enabled (for SMTP)
- Razorpay account (for payment testing)

### 1. Clone & Configure

```bash
git clone https://github.com/deepakbhat18/bookmyslot.git
cd bookmyslot
```

Create a `.env` file in the project root:

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/bookmyslot?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=yourpassword

# Mail (Gmail SMTP)
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your_app_password

# Razorpay
RAZORPAY_KEY_ID=rzp_test_xxxxx
RAZORPAY_KEY_SECRET=your_secret_here

# Optional: server port override
PORT=8080
```

### 2. Build & Run

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/bookmyslot-0.0.1-SNAPSHOT.jar

# Or use the Maven wrapper
./mvnw spring-boot:run
```

### 3. Run with Docker

```bash
# Build image
docker build -t bookmyslot:latest .

# Run container
docker run -p 8080:8080 --env-file .env bookmyslot:latest
```

### 4. Verify

- API base: `http://localhost:8080/api`

---
## 🎓 About This Project


Built during my learning journey with Spring Boot.

There's still a lot to learn — but I challenged myself to work with
real-world concepts like OTP auth, QR codes, and Docker deployment to
go beyond basic CRUD and understand how backends actually work.

> Still learning, still building 🚀 — Deepak Bhat
