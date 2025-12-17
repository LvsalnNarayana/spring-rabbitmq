# 🧩 Event-Driven Microservices with RabbitMQ (Spring Boot)

This repository demonstrates a **real-world, event-driven microservices architecture** using **Spring Boot**, **RabbitMQ**, and **REST-based service communication**.

The project focuses on **decoupled communication**, **domain-driven responsibilities**, and **asynchronous event processing**, while still keeping the system simple and understandable for learning and portfolio purposes.

---

## 📌 High-Level Overview

The system consists of **four independent microservices**:

1. **Order Service**
2. **Product Service**
3. **Notification Service**
4. **Message Processor Service**

The services communicate using:

* **REST** for synchronous operations
* **RabbitMQ (AMQP)** for asynchronous domain events

RabbitMQ acts as the **event backbone** of the system.

---

## 🧠 Why This Project?

This project demonstrates:

* Practical RabbitMQ usage (not just theory)
* Event-driven thinking
* Separation of concerns
* Realistic service boundaries
* How analytics, notifications, and workflows evolve naturally from events
* Clean Spring Boot integration

This is intentionally designed as a **micro-level learning project**, not an over-engineered enterprise system.

---

## 🏗️ Architecture Overview

### Microservices

| Service              | Responsibility                               |
| -------------------- | -------------------------------------------- |
| Order Service        | Order lifecycle, payment state, order status |
| Product Service      | Product catalog, pricing, inventory          |
| Notification Service | User notifications (in-app, extensible)      |
| Message Processor    | Event consumer, orchestration, notifications |

---

### Communication Model

| Type     | Purpose                                |
| -------- | -------------------------------------- |
| REST     | Commands, queries, validations         |
| RabbitMQ | Domain events, fanout, async workflows |

---

⚠️ **[MERMAID PLACEHOLDER — High Level Architecture Diagram]**

---

## 🐇 Role of RabbitMQ in This Project

RabbitMQ enables:

* Loose coupling between services
* Asynchronous event processing
* Fanout for analytics
* Event replay and debugging
* Failure isolation

Instead of services calling each other directly for every action, **services publish facts**, and interested services react.

Example:

> “Order confirmed” is an event, not a command.

---

## 🔄 Event Flow Summary

1. Order Service publishes **order status events**
2. Product Service publishes **inventory events**
3. Message Processor consumes events
4. Notifications are created based on meaningful events
5. Analytics events are logged asynchronously

---

⚠️ **[MERMAID PLACEHOLDER — End-to-End Event Flow]**

---

## 🧩 Service-by-Service Breakdown

---

## 🛒 Order Service

### Responsibilities

* Create orders
* Update order status
* Update payment status
* Fetch order details
* Publish domain events

### Key Events Emitted

| Event                  | Exchange                          |
| ---------------------- | --------------------------------- |
| Order Created          | `order.status.topic.exchange`     |
| Order Confirmed        | `order.status.topic.exchange`     |
| Order Cancelled        | `order.status.topic.exchange`     |
| Payment Status Updated | `payment.status.topic.exchange`   |
| Analytics Events       | `order.analytics.fanout.exchange` |

---

### REST Endpoints

```
POST   /v1/orders
GET    /v1/orders/{orderId}
GET    /v1/orders?status=CONFIRMED
PATCH  /v1/orders/{orderId}/status
PATCH  /v1/orders/{orderId}/payment-status
```

---

### Order Creation Flow

1. Validate request
2. Fetch product prices via Product Service (REST)
3. Calculate total
4. Persist order
5. Publish order status event
6. Publish analytics event

⚠️ **[MERMAID PLACEHOLDER — Order Creation Sequence]**

---

## 📦 Product Service

### Responsibilities

* Product catalog management
* Inventory control
* Pricing updates
* Batch operations for orders
* Inventory events

---

### REST Endpoints

```
POST   /api/v1/products
GET    /api/v1/products/{id}
POST   /api/v1/products/batch
GET    /api/v1/products/status/{status}
GET    /api/v1/products/category/{category}
PUT    /api/v1/products/{id}/activate
PUT    /api/v1/products/{id}/deactivate
PUT    /api/v1/products/{id}/price
PUT    /api/v1/products/{id}/reduce
PUT    /api/v1/products/{id}/increase
POST   /api/v1/products/inventory/reduce-bulk
```

---

### Inventory Events

| Event                 | Meaning               |
| --------------------- | --------------------- |
| INVENTORY_REDUCED     | Stock decreased       |
| OUT_OF_STOCK          | Quantity reached zero |
| PRODUCT_BACK_IN_STOCK | Restock detected      |

These events are **published**, not directly acted upon.

---

⚠️ **[MERMAID PLACEHOLDER — Product Inventory Event Flow]**

---

## 🔔 Notification Service

### Responsibilities

* Store notifications
* Track delivery state
* Support multiple notification types
* Provide user-centric APIs

---

### REST Endpoints

```
POST   /api/v1/notifications
GET    /api/v1/notifications/{id}
GET    /api/v1/notifications/user/{userId}
GET    /api/v1/notifications/status/{status}
PUT    /api/v1/notifications/{id}/read
PUT    /api/v1/notifications/{id}/sent
```

---

### Supported Notification Types

```
ORDER_CREATED
ORDER_SHIPPED
PAYMENT_FAILED
PAYMENT_SUCCESS
PRODUCT_OUT_OF_STOCK
GENERIC
```

Notifications are **created only for meaningful user-facing events**, not every internal state change.

---

## ⚙️ Message Processor Service

### Why This Service Exists

This service:

* Consumes RabbitMQ events
* Interprets business meaning
* Triggers notifications
* Logs analytics
* Acts as an orchestration layer

It prevents:

* Business logic leakage into producers
* Tight coupling between services

---

### Queues Consumed

| Queue                            | Purpose            |
| -------------------------------- | ------------------ |
| order.status.queue               | Order lifecycle    |
| order.analytics.queue            | Analytics          |
| payment.status.queue             | Payment events     |
| products.analytics.queue         | Product analytics  |
| products.notification.user.queue | User notifications |

---

### Example: Order Status → Notification

* `ORDER_CONFIRMED` → Notify user
* `ORDER_SHIPPED` → Notify user
* `ORDER_CANCELLED` → Notify user
* `ORDER_ANALYTICS` → No notification

⚠️ **[MERMAID PLACEHOLDER — Message Processor Decision Flow]**

---

## 🔗 REST + RabbitMQ Together

| Scenario             | Communication   |
| -------------------- | --------------- |
| Fetch product prices | REST            |
| Reduce inventory     | REST            |
| Order status updates | RabbitMQ        |
| Analytics            | RabbitMQ        |
| Notifications        | RabbitMQ → REST |

This hybrid model reflects **real production systems**.
