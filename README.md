# 🚀 Notification Dispatcher - Portfolio Project

Uma arquitetura de **Microserviços Poliglota** que demonstra padrões avançados de software engineering:
- **Idempotência** e **Rate Limiting** (Redis)
- **Comunicação Assíncrona** (Redis Pub/Sub)
- **Segregação de Workloads** (CPU-bound vs I/O-bound)
- **Clean Architecture** e **Domain-Driven Design**

## 📋 Stack Tecnológico

| Serviço | Tecnologia | Responsabilidade |
|---------|-----------|------------------|
| **A** | Java 21 + Spring Boot 3.3 | API REST, Rate Limiting, Idempotência, Auditoria |
| **B** | Node.js 20 + Express + Socket.IO | Real-time Push Notifications via WebSocket |
| **C** | .NET 8 | Worker Service para Renderização de Templates (CPU-Bound) |
| **Infra** | PostgreSQL + Redis | Persistência e Messaging |

## 🏗️ Arquitetura

┌─────────────────────────────────────────────────────────────┐ │ CLIENT REQUISITIONS │ └────────────────┬────────────────────────────────────────────┘ │ ┌────────▼────────┐ │ JAVA SERVICE │ (8080) │ Rate Limiter │ │ Idempotency │ ─────────────► PostgreSQL (5432) │ Audit Logger │ └────┬────┬───┬───┘ │ │ │ ┌───────┘ │ └──────────────┐ │ │ │ PUSH EMAIL SMS channel:push:request channel:email:request channel:sms:request │ │ │ │ └──────┬───────────┘ │ │ │ ┌────────▼─────────┐ │ │ .NET WORKER │ (CPU-Bound) │ │ Template Engine │ │ │ String Rendering │ │ └────────┬─────────┘ │ │ │ channel:delivery:log │ │ ▼ │ ┌─────────────────────────────▼────────┐ │ REDIS PUB/SUB BROKER │ └──────────────┬───────────────────────┘ │ ┌─────────▼─────────┐ │ NODE.JS SERVICE │ (3000) │ Socket.IO Server │ │ (Real-time Push) │ └─────────┬─────────┘ │ ┌─────────▼──────────┐ │ BROWSER CLIENT │ │ WebSocket Listen │ └────────────────────┘


## 🚀 Quick Start

### Pré-requisitos
- Docker & Docker Compose
- (Opcional) cURL para testar API

### 1️⃣ Clonar / Setup

```bash
cd notification-dispatcher
docker-compose up --build