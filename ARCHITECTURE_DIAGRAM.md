# 🏗️ Sprint Approve - Архитектурные диаграммы

## 📊 Общая архитектура

```mermaid
graph TB
    subgraph "Client Layer"
        FE[Frontend<br/>React + TypeScript<br/>:3000]
    end

    subgraph "API Layer"
        GW[API Gateway<br/>Spring Cloud Gateway<br/>:8080<br/>JWT Validation]
    end

    subgraph "Service Discovery"
        EUR[Eureka Server<br/>:8761]
    end

    subgraph "Microservices"
        AUTH[Auth Service<br/>:8084<br/>JWT Generation]
        TEAM[Team Service<br/>:8081<br/>Users & Teams]
        SPRINT[Sprint Service<br/>:8082<br/>Sprints]
        TASK[Task Service<br/>:8083<br/>Tasks & Files]
        NOTIF[Notification Service<br/>:8085<br/>FCM Push]
    end

    subgraph "Data Layer"
        TEAMDB[(team_db<br/>PostgreSQL<br/>:5432)]
        SPRINTDB[(sprint_db<br/>PostgreSQL<br/>:5433)]
        TASKDB[(task_db<br/>PostgreSQL<br/>:5434)]
        NOTIFDB[(notification_db<br/>PostgreSQL<br/>:5435)]
        MINIO[MinIO<br/>File Storage<br/>:9000]
    end

    subgraph "External Services"
        FCM[Firebase Cloud<br/>Messaging]
    end

    FE -->|HTTP/HTTPS| GW
    GW -->|Register/Login| AUTH
    GW -->|JWT Protected| TEAM
    GW -->|JWT Protected| SPRINT
    GW -->|JWT Protected| TASK
    GW -->|JWT Protected| NOTIF

    AUTH -.->|Service Discovery| EUR
    TEAM -.->|Service Discovery| EUR
    SPRINT -.->|Service Discovery| EUR
    TASK -.->|Service Discovery| EUR
    NOTIF -.->|Service Discovery| EUR
    GW -.->|Service Discovery| EUR

    AUTH -->|Feign Client| TEAM
    TASK -->|Feign Client| SPRINT
    TASK -->|Feign Client| TEAM
    SPRINT -->|Feign Client| TASK
    SPRINT -->|Feign Client| TEAM

    TEAM --> TEAMDB
    SPRINT --> SPRINTDB
    TASK --> TASKDB
    TASK --> MINIO
    NOTIF --> NOTIFDB
    NOTIF --> FCM

    style FE fill:#61dafb
    style GW fill:#6db33f
    style EUR fill:#ff6b6b
    style AUTH fill:#ffd93d
    style TEAM fill:#6bcf7f
    style SPRINT fill:#a29bfe
    style TASK fill:#fd79a8
    style NOTIF fill:#fdcb6e
    style FCM fill:#ff9f43
```

---

## 🔄 Workflow: Создание и одобрение задачи

```mermaid
sequenceDiagram
    participant U as User (Frontend)
    participant GW as API Gateway
    participant TS as Task Service
    participant SS as Sprint Service
    participant NS as Notification Service
    participant DB as task_db
    participant FCM as Firebase

    U->>GW: POST /api/tasks (JWT)
    GW->>GW: Validate JWT
    GW->>TS: Forward request + X-User-Id
    TS->>DB: INSERT task (status: CREATED)
    DB-->>TS: Task created
    TS->>SS: PATCH /sprints/{id}/recalculate-status
    SS-->>TS: Sprint status updated
    TS->>NS: Send notification (TASK_ASSIGNED)
    NS->>FCM: Push notification
    FCM-->>U: "Задача назначена"
    TS-->>GW: Task created
    GW-->>U: 201 Created

    Note over U,FCM: Задача отправлена на проверку

    U->>GW: PATCH /api/tasks/1/submit (JWT)
    GW->>TS: Forward request
    TS->>DB: UPDATE task (status: ON_REVIEW)
    TS->>SS: Recalculate sprint status
    SS-->>TS: Sprint status: ON_REVIEW
    TS->>NS: Send notification (TASK_FOR_REVIEW)
    NS->>FCM: Push to approver
    FCM-->>U: "Требуется проверка"
    TS-->>U: Task updated

    Note over U,FCM: Аппрувер одобряет задачу

    U->>GW: PATCH /api/tasks/1/approve (JWT)
    GW->>TS: Forward request
    TS->>DB: UPDATE task (status: APPROVED)
    TS->>SS: Recalculate sprint status
    SS-->>TS: Sprint status: APPROVED
    TS->>NS: Send notification (TASK_APPROVED)
    NS->>FCM: Push to creator
    FCM-->>U: "Задача одобрена ✅"
    TS-->>U: Task approved
```

---

## 🔐 JWT Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant GW as API Gateway
    participant AUTH as Auth Service
    participant TEAM as Team Service
    participant DB as team_db

    Note over U,DB: Регистрация

    U->>GW: POST /api/auth/register
    GW->>AUTH: Forward (no JWT check)
    AUTH->>TEAM: POST /api/users (Feign)
    TEAM->>DB: INSERT user (BCrypt password)
    DB-->>TEAM: User created
    TEAM-->>AUTH: UserDto
    AUTH->>AUTH: Generate JWT (Access + Refresh)
    AUTH-->>GW: Tokens + UserDto
    GW-->>U: 201 Created + Tokens

    Note over U,DB: Вход

    U->>GW: POST /api/auth/login
    GW->>AUTH: Forward
    AUTH->>TEAM: GET /api/users/email/{email}
    TEAM->>DB: SELECT user
    DB-->>TEAM: User data
    TEAM-->>AUTH: UserDto
    AUTH->>AUTH: Verify password (BCrypt)
    AUTH->>AUTH: Generate JWT
    AUTH-->>U: Tokens + UserDto

    Note over U,DB: Защищённый запрос

    U->>GW: GET /api/tasks (Authorization: Bearer token)
    GW->>GW: Validate JWT signature
    GW->>GW: Extract userId, email, role
    GW->>GW: Add headers: X-User-Id, X-User-Email, X-User-Role
    GW->>TASK: Forward with headers
    TASK->>TASK: SecurityContext.get()
    TASK-->>U: Tasks data
```

---

## 📁 File Upload Flow (MinIO)

```mermaid
sequenceDiagram
    participant U as User
    participant GW as API Gateway
    participant TS as Task Service
    participant MINIO as MinIO
    participant DB as task_db

    U->>GW: POST /api/artifacts/upload<br/>(multipart/form-data)
    GW->>TS: Forward file
    TS->>TS: Generate UUID filename
    TS->>MINIO: PUT /bucket/uuid.ext
    MINIO-->>TS: File stored
    TS->>TS: Generate presigned URL (60 min)
    TS->>DB: INSERT artifact metadata
    DB-->>TS: Artifact created
    TS-->>U: ArtifactDto + downloadUrl

    Note over U,MINIO: Скачивание файла

    U->>GW: GET /api/artifacts/1/download-url
    GW->>TS: Forward request
    TS->>DB: SELECT artifact
    DB-->>TS: Artifact metadata
    TS->>TS: Generate new presigned URL
    TS-->>U: {url: "https://minio/...", expiresIn: "60 minutes"}
    
    U->>MINIO: GET presigned URL
    MINIO-->>U: File content
```

---

## 🔔 Push Notification Flow (FCM)

```mermaid
sequenceDiagram
    participant U as User (Browser)
    participant FE as Frontend
    participant GW as API Gateway
    participant NS as Notification Service
    participant DB as notification_db
    participant FCM as Firebase Cloud Messaging

    Note over U,FCM: Регистрация FCM токена

    U->>FE: Allow notifications
    FE->>FE: Request FCM token
    FE->>FCM: Get token
    FCM-->>FE: FCM token
    FE->>GW: POST /api/notifications/register-token
    GW->>NS: Forward request
    NS->>DB: INSERT/UPDATE user_fcm_token
    DB-->>NS: Token saved
    NS-->>FE: Success

    Note over U,FCM: Отправка уведомления

    TS->>NS: Send notification (Feign)<br/>{userId, title, body, data}
    NS->>DB: SELECT fcm_token WHERE user_id
    DB-->>NS: FCM token
    NS->>NS: Build FCM message
    NS->>FCM: Send push notification
    FCM-->>U: Push notification
    U->>U: Show notification
    U->>FE: Click notification
    FE->>FE: Navigate to task
```

---

## 🔗 Service Integration (OpenFeign)

```mermaid
graph LR
    subgraph "Auth Service"
        AUTH[Auth Service]
    end

    subgraph "Team Service"
        TEAM[Team Service]
    end

    subgraph "Sprint Service"
        SPRINT[Sprint Service]
    end

    subgraph "Task Service"
        TASK[Task Service]
    end

    AUTH -->|"getUserByEmail()<br/>createUser()"| TEAM
    TASK -->|"recalculateSprintStatus()"| SPRINT
    TASK -->|"getUserById()"| TEAM
    SPRINT -->|"getTasksBySprintId()"| TASK
    SPRINT -->|"getTeamById()<br/>getUserById()"| TEAM

    style AUTH fill:#ffd93d
    style TEAM fill:#6bcf7f
    style SPRINT fill:#a29bfe
    style TASK fill:#fd79a8
```

---

## 🗄️ Database Schema

```mermaid
erDiagram
    USERS ||--o{ TEAMS : "belongs to"
    TEAMS ||--o{ SPRINTS : "has"
    SPRINTS ||--o{ TASKS : "contains"
    TASKS ||--o{ ARTIFACTS : "has"
    TASKS ||--o{ COMMENTS : "has"
    USERS ||--o{ USER_FCM_TOKENS : "has"

    USERS {
        bigint id PK
        varchar email UK
        varchar name
        varchar password
        bigint team_id FK
        varchar role
        timestamp created_at
        timestamp updated_at
    }

    TEAMS {
        bigint id PK
        varchar name
        text description
        timestamp created_at
        timestamp updated_at
    }

    SPRINTS {
        bigint id PK
        varchar name
        bigint team_id FK
        date start_date
        date end_date
        varchar status
        bigint created_by FK
        timestamp created_at
        timestamp updated_at
    }

    TASKS {
        bigint id PK
        varchar title
        text description
        bigint sprint_id FK
        bigint assigned_to FK
        bigint approver_id FK
        bigint created_by FK
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    ARTIFACTS {
        bigint id PK
        varchar name
        varchar url
        bigint task_id FK
        bigint uploaded_by FK
        varchar file_type
        bigint file_size
        timestamp created_at
    }

    COMMENTS {
        bigint id PK
        bigint task_id FK
        bigint author_id FK
        text content
        timestamp created_at
        timestamp updated_at
    }

    USER_FCM_TOKENS {
        bigint id PK
        bigint user_id FK UK
        varchar fcm_token
        timestamp created_at
        timestamp updated_at
    }
```

---

## 🐳 Docker Infrastructure

```mermaid
graph TB
    subgraph "Docker Compose Network: sprint-approve-network"
        subgraph "Databases"
            TEAMDB[team-db<br/>PostgreSQL:5432]
            SPRINTDB[sprint-db<br/>PostgreSQL:5433]
            TASKDB[task-db<br/>PostgreSQL:5434]
            NOTIFDB[notification-db<br/>PostgreSQL:5435]
        end

        subgraph "Storage"
            MINIO[MinIO<br/>:9000 API<br/>:9001 Console]
        end

        subgraph "Volumes"
            V1[team-db-data]
            V2[sprint-db-data]
            V3[task-db-data]
            V4[notification-db-data]
            V5[minio-data]
        end

        TEAMDB -.->|Persistent| V1
        SPRINTDB -.->|Persistent| V2
        TASKDB -.->|Persistent| V3
        NOTIFDB -.->|Persistent| V4
        MINIO -.->|Persistent| V5
    end

    subgraph "Spring Boot Services (Host)"
        EUR[Eureka Server<br/>:8761]
        GW[API Gateway<br/>:8080]
        AUTH[Auth Service<br/>:8084]
        TEAM[Team Service<br/>:8081]
        SPRINT[Sprint Service<br/>:8082]
        TASK[Task Service<br/>:8083]
        NOTIF[Notification Service<br/>:8085]
    end

    TEAM --> TEAMDB
    SPRINT --> SPRINTDB
    TASK --> TASKDB
    TASK --> MINIO
    NOTIF --> NOTIFDB

    style TEAMDB fill:#336791
    style SPRINTDB fill:#336791
    style TASKDB fill:#336791
    style NOTIFDB fill:#336791
    style MINIO fill:#c72c48
```

---

## 📊 Technology Stack Overview

```mermaid
mindmap
  root((Sprint Approve))
    Backend
      Java 17
      Spring Boot 3.2.0
        Spring Cloud
          Gateway
          Eureka
          OpenFeign
        Spring Security
        Spring Data JPA
      PostgreSQL 15
      MinIO
      Firebase Admin SDK
      JWT JJWT
      BCrypt
      Lombok
      Maven
    Frontend
      React 18
      TypeScript 5
      Vite 5
      TailwindCSS 3
      Axios
      React Router 6
      Lucide React
      Firebase SDK
      Service Workers
    Infrastructure
      Docker
      Docker Compose
      PostgreSQL Containers
      MinIO Container
      Docker Volumes
      Docker Network
    Security
      JWT Tokens
      BCrypt Hashing
      API Gateway Filter
      Security Context
      .gitignore Protection
```

---

## 🎯 Deployment Architecture

```mermaid
graph TB
    subgraph "Production Environment"
        subgraph "Load Balancer"
            LB[Nginx/Traefik<br/>HTTPS/SSL]
        end

        subgraph "Application Layer"
            GW1[API Gateway<br/>Instance 1]
            GW2[API Gateway<br/>Instance 2]
            
            TEAM1[Team Service<br/>Instance 1]
            TEAM2[Team Service<br/>Instance 2]
            
            TASK1[Task Service<br/>Instance 1]
            TASK2[Task Service<br/>Instance 2]
        end

        subgraph "Data Layer"
            PGMASTER[(PostgreSQL<br/>Master)]
            PGREPLICA[(PostgreSQL<br/>Replica)]
            MINIOS3[MinIO Cluster<br/>S3 Compatible]
        end

        subgraph "Monitoring"
            PROM[Prometheus]
            GRAF[Grafana]
        end
    end

    LB --> GW1
    LB --> GW2
    
    GW1 --> TEAM1
    GW1 --> TASK1
    GW2 --> TEAM2
    GW2 --> TASK2
    
    TEAM1 --> PGMASTER
    TEAM2 --> PGMASTER
    TASK1 --> PGMASTER
    TASK2 --> PGMASTER
    
    PGMASTER -.->|Replication| PGREPLICA
    
    TASK1 --> MINIOS3
    TASK2 --> MINIOS3
    
    PROM -.->|Scrape| GW1
    PROM -.->|Scrape| TEAM1
    GRAF -.->|Query| PROM

    style LB fill:#2ecc71
    style PGMASTER fill:#e74c3c
    style PGREPLICA fill:#95a5a6
    style MINIOS3 fill:#c72c48
```

---

**Используйте эти диаграммы в презентации для визуализации архитектуры!**

Диаграммы можно отобразить в:
- GitHub (поддерживает Mermaid)
- VS Code (с расширением Mermaid)
- Mermaid Live Editor: https://mermaid.live/
