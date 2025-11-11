# Osgiliath Deployment Guide

Complete guide for deploying the Osgiliath application to production environments.

## Table of Contents

- [Overview](#overview)
- [Pre-Deployment Checklist](#pre-deployment-checklist)
- [Environment Configuration](#environment-configuration)
- [Database Setup](#database-setup)
- [Backend Deployment](#backend-deployment)
- [Frontend Deployment](#frontend-deployment)
- [Docker Deployment](#docker-deployment)
- [Cloud Deployment](#cloud-deployment)
- [Security Hardening](#security-hardening)
- [Monitoring and Logging](#monitoring-and-logging)
- [Backup and Recovery](#backup-and-recovery)
- [Troubleshooting](#troubleshooting)

## Overview

This guide covers deploying Osgiliath to production. The application consists of:

- **Backend**: Spring Boot application (Java 17)
- **Frontend**: Next.js application (Node.js 18+)
- **Database**: PostgreSQL 15

### Deployment Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Load Balancer / CDN               │
│                   (Cloudflare, AWS ELB)             │
└────────────────┬────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
┌───────▼────────┐  ┌────▼───────────┐
│   Frontend     │  │    Backend     │
│   Next.js      │  │  Spring Boot   │
│   (Node 18+)   │  │  (Java 17)     │
│   Port: 3000   │  │  Port: 8080    │
└────────────────┘  └────┬───────────┘
                         │
                ┌────────▼───────────┐
                │   PostgreSQL 15    │
                │   Port: 5432       │
                └────────────────────┘
```

## Pre-Deployment Checklist

### Code Preparation

- [ ] All tests passing
- [ ] Code reviewed and approved
- [ ] No hardcoded secrets or credentials
- [ ] Environment variables properly configured
- [ ] Database migrations tested
- [ ] API documentation up-to-date
- [ ] Frontend build successful
- [ ] Backend JAR builds without errors

### Infrastructure

- [ ] Production database provisioned
- [ ] Server/container resources allocated
- [ ] Domain name configured
- [ ] SSL certificates obtained
- [ ] Firewall rules configured
- [ ] Monitoring tools set up
- [ ] Backup strategy implemented

### Security

- [ ] JWT secret is strong (256+ bits)
- [ ] Database passwords are strong
- [ ] CORS configured for production domain
- [ ] HTTPS enabled
- [ ] Security headers configured
- [ ] Rate limiting enabled
- [ ] SQL injection protection verified

## Environment Configuration

### Backend Environment Variables

Create a `.env` file or set environment variables:

```bash
# Application
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db-host:5432/osgiliath
SPRING_DATASOURCE_USERNAME=osgiliath_prod
SPRING_DATASOURCE_PASSWORD=<strong-password>

# JWT
JWT_SECRET=<256-bit-secret-key>
JWT_EXPIRATION=86400000

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_OSGILIATH=INFO

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

### Frontend Environment Variables

Create `.env.production`:

```bash
NEXT_PUBLIC_API_URL=https://api.yourdomain.com
NODE_ENV=production
```

### Generating Secrets

```bash
# Generate JWT secret (256 bits)
openssl rand -base64 32

# Generate strong password
openssl rand -base64 24
```

## Database Setup

### PostgreSQL Production Setup

#### 1. Create Database and User

```sql
-- Connect as postgres superuser
psql -U postgres

-- Create database
CREATE DATABASE osgiliath_prod;

-- Create user with strong password
CREATE USER osgiliath_prod WITH PASSWORD '<strong-password>';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE osgiliath_prod TO osgiliath_prod;

-- Grant schema privileges
\c osgiliath_prod
GRANT ALL ON SCHEMA public TO osgiliath_prod;

-- Quit
\q
```

#### 2. Configure PostgreSQL

Edit `postgresql.conf`:

```
# Connection settings
max_connections = 100
shared_buffers = 256MB

# Logging
logging_collector = on
log_directory = 'log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_statement = 'all'

# Performance
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB
```

Edit `pg_hba.conf`:

```
# Allow connections from application server
host    osgiliath_prod    osgiliath_prod    10.0.1.0/24    md5
```

Restart PostgreSQL:

```bash
sudo systemctl restart postgresql
```

#### 3. Database Migrations

The application uses Hibernate DDL auto-update. For production, consider:

**Option 1: Flyway Migrations**

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Create migration files in `src/main/resources/db/migration/`:

```sql
-- V1__Initial_Schema.sql
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email_address VARCHAR(255) UNIQUE NOT NULL,
    -- ... rest of schema
);
```

**Option 2: Manual Schema Creation**

Set `spring.jpa.hibernate.ddl-auto=validate` and create schema manually.

## Backend Deployment

### Method 1: JAR Deployment (Traditional)

#### 1. Build the Application

```bash
cd backend
./mvnw clean package -DskipTests

# JAR will be in target/
ls target/osgiliath-backend-1.0.0-SNAPSHOT.jar
```

#### 2. Transfer to Server

```bash
# Using SCP
scp target/osgiliath-backend-1.0.0-SNAPSHOT.jar user@server:/opt/osgiliath/

# Or rsync
rsync -avz target/osgiliath-backend-1.0.0-SNAPSHOT.jar user@server:/opt/osgiliath/
```

#### 3. Create Systemd Service

Create `/etc/systemd/system/osgiliath-backend.service`:

```ini
[Unit]
Description=Osgiliath Backend Service
After=postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=osgiliath
WorkingDirectory=/opt/osgiliath
ExecStart=/usr/bin/java -jar \
    -Xms512m \
    -Xmx1024m \
    -Dspring.profiles.active=production \
    /opt/osgiliath/osgiliath-backend-1.0.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=osgiliath-backend

# Environment variables
Environment="JWT_SECRET=<your-secret>"
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/osgiliath_prod"
Environment="SPRING_DATASOURCE_USERNAME=osgiliath_prod"
Environment="SPRING_DATASOURCE_PASSWORD=<password>"

[Install]
WantedBy=multi-user.target
```

#### 4. Start the Service

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable service to start on boot
sudo systemctl enable osgiliath-backend

# Start service
sudo systemctl start osgiliath-backend

# Check status
sudo systemctl status osgiliath-backend

# View logs
sudo journalctl -u osgiliath-backend -f
```

### Method 2: Docker Deployment

See [Docker Deployment](#docker-deployment) section below.

## Frontend Deployment

### Method 1: Node.js Server

#### 1. Build the Application

```bash
cd frontend

# Install dependencies
npm ci --production

# Build
npm run build

# Output will be in .next/
```

#### 2. Transfer to Server

```bash
# Transfer necessary files
scp -r .next node_modules package.json package-lock.json user@server:/opt/osgiliath-frontend/
```

#### 3. Create Systemd Service

Create `/etc/systemd/system/osgiliath-frontend.service`:

```ini
[Unit]
Description=Osgiliath Frontend Service
After=network.target

[Service]
Type=simple
User=osgiliath
WorkingDirectory=/opt/osgiliath-frontend
ExecStart=/usr/bin/npm start
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=osgiliath-frontend

Environment="NODE_ENV=production"
Environment="PORT=3000"
Environment="NEXT_PUBLIC_API_URL=https://api.yourdomain.com"

[Install]
WantedBy=multi-user.target
```

#### 4. Start the Service

```bash
sudo systemctl daemon-reload
sudo systemctl enable osgiliath-frontend
sudo systemctl start osgiliath-frontend
sudo systemctl status osgiliath-frontend
```

### Method 2: Static Export to CDN

Next.js can be exported as static files:

#### 1. Configure for Static Export

Edit `next.config.js`:

```javascript
module.exports = {
  output: 'export',
  images: {
    unoptimized: true,
  },
};
```

#### 2. Build and Export

```bash
npm run build

# Static files will be in out/
```

#### 3. Deploy to CDN

Upload `out/` directory to:
- **AWS S3 + CloudFront**
- **Netlify**
- **Vercel**
- **Cloudflare Pages**

## Docker Deployment

### Complete Docker Compose Setup

#### 1. Create Dockerfiles

**Backend Dockerfile**:

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn ./.mvn
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/osgiliath-backend-*.jar app.jar

# Create non-root user
RUN addgroup -S osgiliath && adduser -S osgiliath -G osgiliath
USER osgiliath

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Frontend Dockerfile**:

```dockerfile
# frontend/Dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:18-alpine
WORKDIR /app
COPY --from=build /app/.next ./.next
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package.json ./package.json

# Create non-root user
RUN addgroup -S osgiliath && adduser -S osgiliath -G osgiliath
USER osgiliath

EXPOSE 3000
CMD ["npm", "start"]
```

#### 2. Create Production Docker Compose

**docker-compose.prod.yml**:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: osgiliath-postgres-prod
    environment:
      POSTGRES_DB: osgiliath_prod
      POSTGRES_USER: osgiliath_prod
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - osgiliath-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U osgiliath_prod"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: osgiliath-backend-prod
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/osgiliath_prod
      SPRING_DATASOURCE_USERNAME: osgiliath_prod
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: production
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - osgiliath-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: osgiliath-frontend-prod
    environment:
      NEXT_PUBLIC_API_URL: http://backend:8080/api
      NODE_ENV: production
    ports:
      - "3000:3000"
    depends_on:
      - backend
    networks:
      - osgiliath-network
    restart: unless-stopped

volumes:
  postgres_data:
    driver: local

networks:
  osgiliath-network:
    driver: bridge
```

#### 3. Create Environment File

Create `.env`:

```bash
DB_PASSWORD=<strong-db-password>
JWT_SECRET=<256-bit-secret>
```

#### 4. Deploy with Docker Compose

```bash
# Build and start
docker-compose -f docker-compose.prod.yml up -d --build

# View logs
docker-compose -f docker-compose.prod.yml logs -f

# Stop
docker-compose -f docker-compose.prod.yml down

# Stop and remove volumes
docker-compose -f docker-compose.prod.yml down -v
```

## Cloud Deployment

### AWS Deployment

#### Architecture

```
Route 53 (DNS)
    ↓
Application Load Balancer
    ↓
    ├─→ EC2 Instances (Backend) ←→ RDS PostgreSQL
    └─→ CloudFront ←→ S3 (Frontend Static Files)
```

#### Backend on EC2

1. **Launch EC2 Instance**
   - AMI: Amazon Linux 2023
   - Instance Type: t3.medium (2 vCPU, 4 GB RAM)
   - Security Group: Allow 8080 from ALB

2. **Install Java and Deploy**
   ```bash
   sudo yum install java-17-amazon-corretto
   # Follow JAR deployment steps above
   ```

#### Database on RDS

1. **Create RDS PostgreSQL Instance**
   - Engine: PostgreSQL 15
   - Instance Class: db.t3.micro (for testing)
   - Storage: 20 GB gp3
   - Multi-AZ: Yes (for production)

2. **Configure Security Group**
   - Allow 5432 from backend EC2 security group

3. **Update Backend Configuration**
   ```bash
   SPRING_DATASOURCE_URL=jdbc:postgresql://rds-endpoint:5432/osgiliath
   ```

#### Frontend on S3 + CloudFront

1. **Build and Export Frontend**
   ```bash
   npm run build
   # Upload out/ to S3 bucket
   ```

2. **Create S3 Bucket**
   - Enable static website hosting
   - Upload built files

3. **Create CloudFront Distribution**
   - Origin: S3 bucket
   - Enable HTTPS
   - Custom domain with SSL certificate

### Azure Deployment

#### Architecture

```
Azure DNS
    ↓
Azure Application Gateway
    ↓
    ├─→ App Service (Backend) ←→ Azure Database for PostgreSQL
    └─→ Static Web Apps (Frontend)
```

#### Backend on App Service

1. **Create App Service**
   - Runtime: Java 17
   - Plan: Basic B1 or higher

2. **Deploy JAR**
   ```bash
   az webapp deployment source config-zip \
     --resource-group osgiliath-rg \
     --name osgiliath-backend \
     --src target/osgiliath-backend-1.0.0-SNAPSHOT.jar
   ```

3. **Configure Environment Variables**
   - In Azure Portal: App Service → Configuration → Application settings

#### Frontend on Static Web Apps

1. **Deploy to Static Web Apps**
   ```bash
   npm run build
   az staticwebapp create \
     --name osgiliath-frontend \
     --resource-group osgiliath-rg \
     --source ./out \
     --location "East US"
   ```

## Security Hardening

### Backend Security

#### 1. Update application.yml for Production

```yaml
spring:
  jpa:
    show-sql: false  # Don't log SQL in production
    open-in-view: false

logging:
  level:
    root: WARN
    com.osgiliath: INFO
    org.springframework.security: WARN
```

#### 2. Enable HTTPS

Use reverse proxy (Nginx, Apache) with SSL:

**Nginx Configuration**:

```nginx
server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    ssl_certificate /etc/ssl/certs/your-cert.crt;
    ssl_certificate_key /etc/ssl/private/your-key.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### 3. Configure Security Headers

Add to Spring Security configuration:

```java
http.headers()
    .contentSecurityPolicy("default-src 'self'")
    .and()
    .frameOptions().deny()
    .xssProtection()
    .and()
    .httpStrictTransportSecurity()
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000);
```

### Database Security

```sql
-- Remove test accounts
DROP USER IF EXISTS test;

-- Restrict user permissions
REVOKE ALL ON DATABASE osgiliath_prod FROM PUBLIC;
GRANT CONNECT ON DATABASE osgiliath_prod TO osgiliath_prod;

-- Enable SSL connections
-- In postgresql.conf:
ssl = on
ssl_cert_file = '/path/to/server.crt'
ssl_key_file = '/path/to/server.key'
```

## Monitoring and Logging

### Application Monitoring

#### Enable Spring Boot Actuator

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Configure endpoints in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

#### Prometheus + Grafana

1. **Add Micrometer Prometheus**
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   ```

2. **Configure Prometheus**
   ```yaml
   # prometheus.yml
   scrape_configs:
     - job_name: 'osgiliath-backend'
       metrics_path: '/api/actuator/prometheus'
       static_configs:
         - targets: ['localhost:8080']
   ```

### Logging

#### Centralized Logging with ELK

1. **Configure Logback**
   ```xml
   <!-- src/main/resources/logback-spring.xml -->
   <configuration>
       <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
           <destination>logstash-host:5000</destination>
           <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
       </appender>

       <root level="INFO">
           <appender-ref ref="LOGSTASH" />
       </root>
   </configuration>
   ```

#### Application Logs

```bash
# View backend logs
sudo journalctl -u osgiliath-backend -f

# View frontend logs
sudo journalctl -u osgiliath-frontend -f

# Docker logs
docker logs -f osgiliath-backend-prod
```

## Backup and Recovery

### Database Backup

#### Automated Backups

```bash
#!/bin/bash
# backup-db.sh

BACKUP_DIR="/var/backups/osgiliath"
DATE=$(date +%Y%m%d_%H%M%S)
FILENAME="osgiliath_backup_$DATE.sql"

# Create backup
pg_dump -U osgiliath_prod -h localhost osgiliath_prod > "$BACKUP_DIR/$FILENAME"

# Compress
gzip "$BACKUP_DIR/$FILENAME"

# Delete backups older than 30 days
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +30 -delete

echo "Backup completed: $FILENAME.gz"
```

Schedule with cron:

```bash
# Run daily at 2 AM
0 2 * * * /opt/scripts/backup-db.sh
```

#### Restore from Backup

```bash
# Decompress
gunzip osgiliath_backup_20241107_020000.sql.gz

# Restore
psql -U osgiliath_prod -h localhost osgiliath_prod < osgiliath_backup_20241107_020000.sql
```

### Application Backup

```bash
# Backup application files
tar -czf osgiliath-app-backup.tar.gz /opt/osgiliath/

# Backup configuration
tar -czf osgiliath-config-backup.tar.gz /etc/systemd/system/osgiliath-*
```

## Troubleshooting

### Backend Not Starting

```bash
# Check logs
sudo journalctl -u osgiliath-backend -n 100

# Common issues:
# 1. Database connection
# 2. Port already in use
# 3. Missing environment variables
# 4. Insufficient memory
```

### Database Connection Issues

```bash
# Test connection
psql -U osgiliath_prod -h localhost -d osgiliath_prod

# Check PostgreSQL is running
sudo systemctl status postgresql

# Check firewall
sudo ufw status
```

### Out of Memory

```bash
# Increase Java heap size
-Xms1024m -Xmx2048m

# Monitor memory
htop
free -h
```

### SSL Certificate Issues

```bash
# Check certificate expiry
openssl x509 -in /path/to/cert.crt -noout -enddate

# Renew Let's Encrypt certificate
certbot renew
```

---

**For production support, ensure you have:**
- [ ] Monitoring dashboards set up
- [ ] Alert rules configured
- [ ] Backup/restore procedure tested
- [ ] Disaster recovery plan documented
- [ ] On-call rotation established
