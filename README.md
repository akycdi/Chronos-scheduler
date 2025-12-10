# Chronos - Distributed Job Scheduling System

## Overview

Chronos is a robust and scalable distributed job scheduling system built with Spring Boot. It provides comprehensive functionality for scheduling, managing, and monitoring both one-time and recurring jobs. The system is designed with scalability and reliability in mind, supporting various job types and automatic retry mechanisms.

## Features

### Core Features

1. **Job Submission**
   - Submit jobs for immediate or scheduled execution
   - Support for various job types (HTTP requests, shell scripts, Python scripts, Java classes, custom jobs)
   - Flexible scheduling with cron expressions or ISO datetime

2. **Recurring Jobs**
   - Support for recurring jobs with cron expressions
   - Automatic calculation of next run times
   - Version management for schedule changes

3. **Job Management**
   - View job status and details
   - Cancel running or scheduled jobs
   - Reschedule jobs with new schedules
   - Filter jobs by owner

4. **Failure Handling**
   - Automatic retry mechanism with configurable retry limits
   - Dead letter queue for failed jobs
   - User notifications for persistent failures

5. **Logging and Monitoring**
   - Comprehensive logging of all job executions
   - Prometheus metrics integration
   - Health check endpoints
   - System statistics and monitoring APIs
6. **Admin Dashboard (New)**
   - Web-based UI for job management
   - Real-time status monitoring
   - Job execution history visualization

## Architecture

### System Flow

```
Client → API Gateway → Auth & Validation → Scheduler Service (persistent store + queue) → Distributed Workers (execute jobs)
```

### Components

1. **Scheduler Service (Central)**
   - Converts schedules (cron) to concrete run-time events
   - Stores next-run metadata
   - Enqueues tasks to message queue
   - Ensures idempotency and schedule versioning

2. **API Layer (REST)**
   - Accepts job submission
   - CRUD operations for jobs
   - Status queries and audit logs
   - Paged listing with filters

3. **Dispatcher / Queue**
   - Durable message queue (RabbitMQ)
   - Supports delayed messages and visibility timeouts
   - Dead letter queue for failed messages

4. **Persistence**
   - PostgreSQL for job definitions, runs, retries, and state transitions
   - Indexed tables for efficient querying

5. **Monitoring & Logging**
   - Centralized logs
   - Prometheus metrics
   - Actuator endpoints

## Tech Stack

- **API Framework**: Spring Boot 3.2.0
- **Database**: PostgreSQL
- **Message Queue**: RabbitMQ
- **Authentication**: JWT (JSON Web Tokens)
- **Monitoring**: Micrometer + Prometheus
- **Cron Parsing**: CronUtils
- **HTTP Client**: WebFlux (Reactive)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- RabbitMQ 3.8+
- Node.js 18+ (for Frontend UI)

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE chronos_db;
```

### 2. RabbitMQ Setup

Ensure RabbitMQ is running. Default configuration:
- Host: localhost
- Port: 5672
- Username: guest
- Password: guest

### 3. Configuration

Update `src/main/resources/application.yml` with your database and RabbitMQ credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chronos_db
    username: your_username
    password: your_password
  
  rabbitmq:
    host: localhost
    port: 5672
    username: your_rabbitmq_username
    password: your_rabbitmq_password
```

### 4. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on port 8080 by default.
### 5. Frontend UI Setup

Navigate to the `chronos-ui` directory and start the development server:

```bash
cd chronos-ui
npm install
npm run dev
```

The UI will be available at `http://localhost:5173` (default Vite port).

## API Documentation

### Authentication

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

**Note:** Default users:
- Username: `admin`, Password: `admin` (Admin role)
- Username: `user`, Password: `user` (User role)

### Job Management APIs

#### Create Job
```http
POST /api/jobs
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Daily Report",
  "owner": "user@example.com",
  "type": "HTTP_REQUEST",
  "description": "Generate daily report",
  "schedule": "0 0 9 * * ?",
  "isRecurring": true,
  "maxRetries": 3,
  "jobData": "{\"url\": \"https://api.example.com/report\", \"method\": \"POST\", \"headers\": {\"Content-Type\": \"application/json\"}, \"body\": {\"date\": \"2024-01-01\"}}"
}
```

**Job Types:**
- `HTTP_REQUEST`: Execute HTTP requests
- `SHELL_SCRIPT`: Execute shell scripts
- `PYTHON_SCRIPT`: Execute Python scripts
- `JAVA_CLASS`: Execute Java classes
- `CUSTOM`: Custom job execution

**Schedule Formats:**
- Cron expression (for recurring jobs): `0 0 9 * * ?` (daily at 9 AM)
- ISO datetime (for one-time jobs): `2024-01-01T09:00:00`

**Response:**
```json
{
  "id": 1,
  "name": "Daily Report",
  "owner": "user@example.com",
  "type": "HTTP_REQUEST",
  "status": "SCHEDULED",
  "nextRunTime": "2024-01-01T09:00:00",
  "isRecurring": true,
  "maxRetries": 3,
  "currentRetries": 0,
  "version": 1,
  "createdAt": "2024-01-01T08:00:00"
}
```

#### Get Job
```http
GET /api/jobs/{id}
Authorization: Bearer {token}
```

#### List Jobs
```http
GET /api/jobs?owner=user@example.com&page=0&size=20
Authorization: Bearer {token}
```

#### Cancel Job
```http
POST /api/jobs/{id}/cancel
Authorization: Bearer {token}
```

#### Reschedule Job
```http
POST /api/jobs/{id}/reschedule
Authorization: Bearer {token}
Content-Type: application/json

{
  "schedule": "0 0 10 * * ?",
  "isRecurring": true
}
```

### Job Run APIs

#### Get Job Runs
```http
GET /api/jobs/{jobId}/runs?page=0&size=20
Authorization: Bearer {token}
```

#### Get Job Run Details
```http
GET /api/jobs/{jobId}/runs/{runId}
Authorization: Bearer {token}
```

### Monitoring APIs

#### System Statistics
```http
GET /api/monitoring/stats
```

#### Health Check
```http
GET /api/health
```

#### Actuator Endpoints
- `/actuator/health` - Application health
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

## Job Data Format

### HTTP_REQUEST Job Type
```json
{
  "url": "https://api.example.com/endpoint",
  "method": "GET|POST|PUT|DELETE",
  "headers": {
    "Authorization": "Bearer token",
    "Content-Type": "application/json"
  },
  "body": {
    "key": "value"
  }
}
```

### SHELL_SCRIPT Job Type
```json
{
  "script": "echo 'Hello World' && date"
}
```

### PYTHON_SCRIPT Job Type
```json
{
  "script": "print('Hello World')",
  "scriptPath": "/path/to/script.py"  // Optional: use scriptPath instead of script
}
```

### JAVA_CLASS Job Type
```json
{
  "className": "com.example.MyJobClass"
}
```

## Design Decisions

### 1. Database Schema
- **Jobs Table**: Stores job definitions with indexes on `owner`, `status`, and `nextRunTime` for efficient querying
- **Job Runs Table**: Tracks execution history with indexes on `job_id`, `status`, and `startedAt`
- **Versioning**: Jobs have a version field to track schedule changes and ensure idempotency

### 2. Message Queue
- **RabbitMQ**: Chosen for reliability and durability
- **Dead Letter Queue**: Handles messages that fail after retries
- **Durable Queues**: Ensures message persistence

### 3. Scheduling Strategy
- **Polling-based**: Scheduler polls database every 5 seconds (configurable) for jobs ready to execute
- **Cron Parsing**: Uses CronUtils library for robust cron expression parsing
- **Next Run Calculation**: Calculated and stored in database for efficient querying

### 4. Retry Mechanism
- **Configurable Retries**: Each job can have custom max retry count
- **Exponential Backoff**: Retries scheduled with delay (currently 5 seconds, can be enhanced)
- **Status Tracking**: Jobs move to RETRYING status during retries

### 5. Security
- **JWT Authentication**: Stateless authentication using JWT tokens
- **Role-based Access**: Support for different user roles (currently in-memory, can be extended to database)
- **Secure by Default**: All endpoints require authentication except health and monitoring

### 6. Monitoring
- **Prometheus Integration**: Comprehensive metrics for job execution times, success/failure rates
- **Actuator Endpoints**: Standard Spring Boot actuator endpoints for health and metrics
- **Custom Metrics**: Active jobs, queued jobs, job submission/completion counters

## Scaling Considerations

### Current Architecture
- Single scheduler instance
- Multiple worker instances (via RabbitMQ consumers)
- Centralized database

### Future Enhancements

1. **Distributed Scheduler**
   - Move from single scheduler to distributed scheduler
   - Use leader election for scheduler coordination
   - Shard jobs across multiple scheduler instances

2. **Database Sharding**
   - Shard jobs table by owner or job ID
   - Implement read replicas for better read performance

3. **Kubernetes Deployment**
   - Auto-scaling worker pods based on queue depth
   - Horizontal pod autoscaling
   - Resource limits and requests

4. **Worker Scaling**
   - Group workers by job type
   - Scale workers based on execution time
   - Priority queues for different job types

5. **Admin Dashboard**
   - Web UI for job management
   - Real-time monitoring dashboard
   - Job execution visualization

## Error Handling

- **Validation Errors**: Returns 400 with detailed field errors
- **Authentication Errors**: Returns 401 for invalid credentials
- **Not Found Errors**: Returns 404 for missing resources
- **Server Errors**: Returns 500 with error details (in development)

## Logging

Logs are written to:
- Console (INFO level)
- File: `logs/chronos.log`

Log levels:
- `com.chronos`: INFO
- `org.springframework.amqp`: DEBUG

## Metrics

Available Prometheus metrics:
- `chronos.job.execution.time` - Job execution duration
- `chronos.job.execution` - Job execution counter (by status and type)
- `chronos.job.submitted` - Total jobs submitted
- `chronos.job.completed` - Total jobs completed
- `chronos.job.failed` - Total jobs failed
- `chronos.job.active` - Currently active jobs
- `chronos.job.queued` - Jobs in queue

## Testing

### Manual Testing

1. **Start the application**
2. **Login to get JWT token**
3. **Create a job**
4. **Monitor job execution via logs and metrics**

### Automated Testing

#### Stress Testing
A Python script is provided to stress test the system by generating concurrent job submissions.

```bash
# Install python dependencies (if any standard libs are insufficient)
# The script uses standard libraries but ensure you have Python 3.8+

python stress_test.py
```

Configuration variables (concurrency, number of requests) can be modified directly in the `stress_test.py` file.

#### Postman Tests
- `chronos_postman_collection.json`: Complete API collection.
- `chronos_automated_tests.json`: Automated test scenarios.

Import these into Postman to run integration tests against the running API.

### Example cURL Commands


```bash
# Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

# Create a job
curl -X POST http://localhost:8080/api/jobs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Job",
    "owner": "test@example.com",
    "type": "HTTP_REQUEST",
    "schedule": "0 * * * * ?",
    "isRecurring": true,
    "jobData": "{\"url\": \"https://httpbin.org/get\", \"method\": \"GET\"}"
  }'

# Get jobs
curl -X GET http://localhost:8080/api/jobs \
  -H "Authorization: Bearer $TOKEN"

# Get metrics
curl http://localhost:8080/actuator/prometheus
```

## Contributing

This is a project submission. For questions or issues, please contact the development team.

## License

This project is developed as part of an academic/professional assignment.

## Author

Arun Kumar Borru - BEL-12

---

**Note**: This system is designed for production use but may require additional hardening for enterprise deployments, including:
- Database connection pooling optimization
- Enhanced security configurations
- Production-grade notification services (email, Slack, etc.)
- Comprehensive test coverage
- Docker containerization
- CI/CD pipeline setup

