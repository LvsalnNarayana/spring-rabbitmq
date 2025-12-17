Below is a **clean, production-quality `rabbitmq-installation.md`** written exactly for a **portfolio microservice project**.

* ✅ Uses **Docker (recommended)**
* ✅ Shows **Spring Boot integration**
* ✅ Uses **spring-boot-starter-amqp**
* ✅ Includes **application.properties**
* ✅ Clearly marked **diagram placeholders**
* ✅ No images generated
* ✅ Professional, interview-ready language

You can place this file at:
`docs/rabbitmq-installation.md`

---

# 🐇 RabbitMQ – Installation & Spring Boot Integration (Docker)

This document explains how to:

1. Install and run RabbitMQ using Docker
2. Access the RabbitMQ Management Web UI
3. Connect RabbitMQ to Spring Boot microservices
4. Configure messaging using `spring-boot-starter-amqp`
5. Verify message flow

---

## 1. Why Docker for RabbitMQ?

Using Docker provides:

* Zero local installation friction
* Repeatable environments
* Easy teardown and recreation
* Version control
* Ideal for microservice architectures

For local development and portfolio projects, **Docker is the preferred approach**.

---

## 2. RabbitMQ Docker Image

We use the official image with Management UI enabled:

```
rabbitmq:3-management
```

This image includes:

* AMQP broker
* Management Web UI
* Metrics and monitoring endpoints

---

## 3. Running RabbitMQ with Docker

### 3.1 Single Command (Quick Start)

```bash
docker run -d \
  --hostname rabbitmq \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

### Port Explanation

| Port  | Purpose                              |
| ----- | ------------------------------------ |
| 5672  | AMQP protocol (used by applications) |
| 15672 | Management Web UI                    |

---

## 4. RabbitMQ Management Web UI

Once the container is running, open:

```
http://localhost:15672
```

### Default Credentials

| Field    | Value |
| -------- | ----- |
| Username | guest |
| Password | guest |

### What You Can See in the UI

* Exchanges
* Queues
* Bindings
* Message rates
* Consumers
* Channels
* Connections

[[DIAGRAM PLACEHOLDER — RabbitMQ Web UI Overview]]

---

## 5. Recommended Docker Compose Setup

For microservices, **Docker Compose** is cleaner.

### docker-compose.yml

```yaml
version: "3.8"

services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    networks:
      - microservices-net

networks:
  microservices-net:
    driver: bridge
```

Start RabbitMQ:

```bash
docker-compose up -d
```

---

## 6. Connecting RabbitMQ to Spring Boot

Spring Boot integrates with RabbitMQ using **Spring AMQP**.

---

## 7. Add Spring Boot Dependency

In each microservice (`order`, `product`, `message-processor`, etc.):

### Maven Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

This starter provides:

* `RabbitTemplate`
* Listener containers
* Connection management
* Message converters

---

## 8. application.properties Configuration

### Basic Configuration

```properties
# ===============================
# RabbitMQ Connection
# ===============================
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# ===============================
# Listener Configuration
# ===============================
spring.rabbitmq.listener.simple.acknowledge-mode=auto
spring.rabbitmq.listener.simple.prefetch=10
spring.rabbitmq.listener.simple.concurrency=1
spring.rabbitmq.listener.simple.max-concurrency=5

# ===============================
# Publisher Configuration
# ===============================
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
```

---

## 9. Connection Flow (Spring Boot → RabbitMQ)

[[DIAGRAM PLACEHOLDER — Spring Boot to RabbitMQ Connection Flow]]

```
Spring Boot App
 └── ConnectionFactory
      └── TCP Connection
           └── Channel
                └── Exchange / Queue
```

---

## 10. RabbitTemplate (Producer)

Spring provides `RabbitTemplate` for publishing messages.

### How it Works

* Uses connection pooling
* Uses channels internally
* Handles retries and confirms
* Converts messages automatically

[[DIAGRAM PLACEHOLDER — RabbitTemplate Publish Flow]]

---

## 11. @RabbitListener (Consumer)

Consumers are created using annotations.

```java
@RabbitListener(queues = "order.status.queue")
public void handleMessage(String message) {
    // process message
}
```

Spring automatically:

* Creates listener containers
* Manages threads
* Handles acknowledgements
* Restarts consumers on failure

[[DIAGRAM PLACEHOLDER — Message Consumption Flow]]

---

## 12. Message Serialization Strategy

### Recommended Format

* JSON
* UTF-8
* Explicit schema

Spring uses:

* `Jackson2JsonMessageConverter` (recommended)
* Or plain String messages

---

## 13. RabbitMQ Objects Auto-Creation

When using Spring AMQP:

* Exchanges
* Queues
* Bindings

can be **declared as beans** and auto-created on startup.

This ensures:

* Infrastructure as code
* No manual UI setup
* Consistent environments

[[DIAGRAM PLACEHOLDER — Queue & Exchange Auto Declaration]]

---

## 14. Verifying RabbitMQ Setup

### Step-by-step Validation

1. Start RabbitMQ container
2. Start Spring Boot service
3. Open Web UI
4. Verify:

    * Connections
    * Channels
    * Queues
    * Message flow

[[DIAGRAM PLACEHOLDER — Message Flow Verification]]

---

## 15. Common Troubleshooting

### RabbitMQ Not Connecting

* Check Docker container status
* Verify ports
* Check credentials
* Ensure firewall not blocking

### Messages Not Consumed

* Queue name mismatch
* Listener not running
* Ack mode misconfigured
* Binding missing

---

## 16. Recommended Practices

* One connection per service
* Multiple channels per connection
* Durable queues for business data
* Publisher confirms enabled
* Avoid transactions
* Use DLX for retries
* Use fanout for analytics
* Topic exchanges for workflows

---

## 17. How This Fits in Microservices

* REST → synchronous commands
* RabbitMQ → asynchronous events
* Clear ownership per service
* Loose coupling
* High resilience

[[DIAGRAM PLACEHOLDER — Microservices + RabbitMQ Architecture]]

---

## End of `rabbitmq-installation.md`

---

### What’s Next (Paused)

You’re perfectly set up to continue with:

* `README.md` (project architecture & flows)
* Mermaid diagrams
* Interview notes
* Kafka comparison follow-up
* Production hardening

When ready, tell me **what to generate next**.
