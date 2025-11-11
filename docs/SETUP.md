# Osgiliath Setup Guide

Complete step-by-step instructions for setting up the Osgiliath application locally.

## Table of Contents

- [System Requirements](#system-requirements)
- [Installation Steps](#installation-steps)
- [Database Setup](#database-setup)
- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [First-Time User Guide](#first-time-user-guide)
- [Verification](#verification)
- [Troubleshooting](#troubleshooting)
- [Development Tools](#development-tools)

## System Requirements

### Required Software

| Software | Minimum Version | Recommended Version | Download Link |
|----------|----------------|---------------------|---------------|
| Java JDK | 17 | 17 or 21 | [Adoptium](https://adoptium.net/) |
| Node.js | 18.0 | 20.x LTS | [nodejs.org](https://nodejs.org/) |
| npm | 9.0 | 10.x | Included with Node.js |
| Docker | 20.10 | Latest | [docker.com](https://www.docker.com/products/docker-desktop) |
| Docker Compose | 2.0 | Latest | Included with Docker Desktop |
| Git | 2.30 | Latest | [git-scm.com](https://git-scm.com/) |
| Maven | 3.9 | Latest | [maven.apache.org](https://maven.apache.org/) (optional) |

### Hardware Requirements

- **CPU**: 2+ cores recommended
- **RAM**: 8GB minimum, 16GB recommended
- **Disk Space**: 5GB free space
- **Operating System**: macOS, Linux, or Windows 10/11

### Optional Tools

- **IDE**: IntelliJ IDEA, Eclipse, or VS Code
- **API Client**: Postman, Insomnia, or cURL
- **Database Client**: pgAdmin, DBeaver, or DataGrip
- **Git Client**: GitHub Desktop, GitKraken, or SourceTree

## Installation Steps

### Step 1: Verify Prerequisites

Check installed versions:

```bash
# Check Java version
java -version
# Should show Java 17 or higher

# Check Node.js version
node --version
# Should show v18.0.0 or higher

# Check npm version
npm --version
# Should show 9.0.0 or higher

# Check Docker version
docker --version
# Should show Docker version 20.10 or higher

# Check Docker Compose version
docker-compose --version
# Should show version 2.0 or higher

# Check Git version
git --version
# Should show Git version 2.30 or higher
```

### Step 2: Clone the Repository

```bash
# Clone the repository
git clone <repository-url>

# Navigate to project directory
cd Osgiliath

# Verify project structure
ls -la
# Should see: backend/, frontend/, docs/, docker-compose.yml, README.md
```

## Database Setup

### Option 1: Using Docker Compose (Recommended)

The easiest way to set up PostgreSQL is using Docker Compose.

#### 1. Review Docker Compose Configuration

```bash
cat docker-compose.yml
```

Configuration includes:
- PostgreSQL 15 Alpine image
- Port: 5432
- Database: osgiliath
- Username: osgiliath
- Password: osgiliath_password

#### 2. Start PostgreSQL

```bash
# Start PostgreSQL in detached mode
docker-compose up -d

# Expected output:
# [+] Running 2/2
#  ✔ Network osgiliath_default          Created
#  ✔ Container osgiliath-postgres       Started
```

#### 3. Verify PostgreSQL is Running

```bash
# Check container status
docker-compose ps

# Expected output:
# NAME                   COMMAND                  SERVICE    STATUS
# osgiliath-postgres     "docker-entrypoint..."   postgres   Up

# Check logs
docker-compose logs postgres

# Test connection
docker-compose exec postgres psql -U osgiliath -d osgiliath -c "SELECT version();"
```

#### 4. Access PostgreSQL (Optional)

```bash
# Connect to PostgreSQL shell
docker-compose exec postgres psql -U osgiliath -d osgiliath

# Inside PostgreSQL shell:
osgiliath=# \dt          # List tables (empty initially)
osgiliath=# \l           # List databases
osgiliath=# \q           # Quit
```

### Option 2: Local PostgreSQL Installation

If you prefer to install PostgreSQL locally:

#### 1. Install PostgreSQL

**macOS (using Homebrew)**:
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Ubuntu/Debian**:
```bash
sudo apt update
sudo apt install postgresql-15 postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**Windows**:
Download installer from [postgresql.org](https://www.postgresql.org/download/windows/)

#### 2. Create Database and User

```bash
# Connect as postgres user
sudo -u postgres psql

# Inside PostgreSQL shell, run:
CREATE DATABASE osgiliath;
CREATE USER osgiliath WITH PASSWORD 'osgiliath_password';
GRANT ALL PRIVILEGES ON DATABASE osgiliath TO osgiliath;
\q
```

#### 3. Verify Connection

```bash
psql -U osgiliath -d osgiliath -h localhost
```

### Database Cleanup (if needed)

```bash
# Stop and remove containers
docker-compose down

# Remove database volume (deletes all data)
docker-compose down -v

# Restart fresh
docker-compose up -d
```

## Backend Setup

### Step 1: Navigate to Backend Directory

```bash
cd backend
```

### Step 2: Review Configuration

Check `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/osgiliath
    username: osgiliath
    password: osgiliath_password
```

### Step 3: Build the Backend

Using Maven wrapper (recommended):
```bash
# Clean and install dependencies
./mvnw clean install

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Total time: XX s
```

Or using system Maven:
```bash
mvn clean install
```

### Step 4: Run the Backend

**Option 1: Using Maven**:
```bash
./mvnw spring-boot:run
```

**Option 2: Using JAR file**:
```bash
# Build JAR
./mvnw clean package -DskipTests

# Run JAR
java -jar target/osgiliath-backend-1.0.0-SNAPSHOT.jar
```

### Step 5: Verify Backend is Running

The backend should start on port 8080. Look for this log message:
```
Started OsgiliathApplication in X.XXX seconds
```

Test endpoints:
```bash
# Health check (if actuator enabled)
curl http://localhost:8080/api/actuator/health

# Swagger UI (should open in browser)
open http://localhost:8080/api/swagger-ui.html
```

### Backend Environment Variables (Optional)

For custom configuration:

```bash
# Create .env file in backend directory
cat > .env << EOF
JWT_SECRET=your-custom-secret-key-here-at-least-256-bits
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/osgiliath
SPRING_DATASOURCE_USERNAME=osgiliath
SPRING_DATASOURCE_PASSWORD=osgiliath_password
EOF

# Run with environment variables
source .env && ./mvnw spring-boot:run
```

## Frontend Setup

### Step 1: Navigate to Frontend Directory

```bash
cd ../frontend
```

### Step 2: Install Dependencies

```bash
# Install npm packages
npm install

# Expected output:
# added XXX packages in XX s
```

If you encounter issues:
```bash
# Clear npm cache and retry
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### Step 3: Configure Environment

```bash
# Copy example environment file
cp .env.local.example .env.local

# Verify configuration
cat .env.local
```

Should contain:
```
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### Step 4: Run the Frontend

```bash
# Start development server
npm run dev

# Expected output:
# - Local:   http://localhost:3000
# - Ready in X.X s
```

### Step 5: Verify Frontend is Running

Open browser to: `http://localhost:3000`

You should see the Osgiliath login page.

### Frontend Build (Optional)

To test production build:

```bash
# Build for production
npm run build

# Start production server
npm start
```

### Frontend Scripts

```bash
npm run dev          # Start development server
npm run build        # Build for production
npm start            # Start production server
npm run lint         # Run ESLint
npm run type-check   # Run TypeScript type checking
```

## First-Time User Guide

### Step 1: Access the Application

Open browser to `http://localhost:3000`

### Step 2: Register a New User

1. Click on "Register" or "Sign Up" link
2. Fill in registration form:
   - Username: your_username
   - Email: your@email.com
   - Password: SecurePassword123
3. Click "Register"

### Step 3: Login

1. Enter your username and password
2. Click "Login"
3. You'll receive a JWT token and be redirected to dashboard

### Step 4: Create Your First Customer

1. Navigate to "Customers" page
2. Click "New Customer" button
3. Fill in customer details:
   - Name: Acme Corporation
   - Email: contact@acme.com (must be unique)
   - Phone: +1-555-0123 (optional)
   - Address: 123 Main St, City, State 12345 (optional)
4. Click "Save"

### Step 5: Create Your First Invoice

1. Navigate to "Invoices" page
2. Click "New Invoice" button
3. Select customer: Acme Corporation
4. Set invoice details:
   - Invoice Number: INV-001 (auto-generated or custom)
   - Issue Date: Today
   - Due Date: 30 days from today
5. Add line items:
   - Description: Consulting Services
   - Quantity: 10
   - Unit Price: 150.00
   - Click "Add Line Item"
6. Review calculated totals:
   - Subtotal: $1,500.00
   - Tax (10%): $150.00
   - Total: $1,650.00
7. Click "Save" (invoice created in DRAFT status)

### Step 6: Send the Invoice

1. Open the invoice you just created
2. Review all details
3. Click "Send Invoice" button
4. Status changes from DRAFT to SENT
5. Balance Due is now $1,650.00

### Step 7: Record a Payment

1. On the invoice detail page, click "Record Payment"
2. Fill in payment details:
   - Payment Date: Today
   - Amount: $1,650.00
   - Payment Method: Bank Transfer
   - Reference Number: REF-12345 (optional)
3. Click "Save"
4. Invoice status changes to PAID
5. Balance Due is now $0.00

## Verification

### Complete System Check

Run these checks to ensure everything is working:

#### 1. Database Check
```bash
docker-compose ps
# STATUS should be "Up"

docker-compose exec postgres psql -U osgiliath -d osgiliath -c "\dt"
# Should list tables: customers, invoices, line_items, payments, users
```

#### 2. Backend Check
```bash
# API health
curl http://localhost:8080/api/actuator/health
# Should return: {"status":"UP"}

# Swagger UI
curl -I http://localhost:8080/api/swagger-ui.html
# Should return: HTTP/1.1 200
```

#### 3. Frontend Check
```bash
# Homepage
curl -I http://localhost:3000
# Should return: HTTP/1.1 200
```

#### 4. Integration Check
```bash
# Register a test user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!"
  }'

# Should return: 200 OK with user data and JWT token
```

## Troubleshooting

### Database Issues

#### Problem: Cannot connect to PostgreSQL

**Solution 1**: Check if container is running
```bash
docker-compose ps
docker-compose logs postgres
```

**Solution 2**: Restart PostgreSQL
```bash
docker-compose restart postgres
```

**Solution 3**: Check port is not in use
```bash
lsof -i :5432
# If another process is using port 5432, stop it or change port in docker-compose.yml
```

#### Problem: "database does not exist"

**Solution**: Database will be created automatically by Spring Boot if `ddl-auto: update` is set.

#### Problem: Authentication failed

**Solution**: Check credentials in application.yml match docker-compose.yml

### Backend Issues

#### Problem: Port 8080 already in use

**Solution**: Find and kill process using port 8080
```bash
# macOS/Linux
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

Or change port in application.yml:
```yaml
server:
  port: 8081
```

#### Problem: Java version mismatch

**Solution**: Check Java version
```bash
java -version
# Should be 17 or higher

# If wrong version, set JAVA_HOME
export JAVA_HOME=/path/to/jdk-17
```

#### Problem: Maven build fails

**Solution 1**: Clean Maven cache
```bash
./mvnw clean
rm -rf ~/.m2/repository
./mvnw install
```

**Solution 2**: Skip tests
```bash
./mvnw clean install -DskipTests
```

#### Problem: "Could not find or load main class"

**Solution**: Rebuild the project
```bash
./mvnw clean compile
./mvnw spring-boot:run
```

### Frontend Issues

#### Problem: Port 3000 already in use

**Solution**: Kill process or change port
```bash
# Kill process on port 3000
lsof -i :3000
kill -9 <PID>

# Or change port
PORT=3001 npm run dev
```

#### Problem: npm install fails

**Solution 1**: Clear npm cache
```bash
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

**Solution 2**: Use different package manager
```bash
# Using yarn
yarn install
yarn dev

# Using pnpm
pnpm install
pnpm dev
```

#### Problem: "Cannot connect to backend"

**Solution 1**: Verify backend is running
```bash
curl http://localhost:8080/api/actuator/health
```

**Solution 2**: Check .env.local file
```bash
cat .env.local
# Should contain: NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

**Solution 3**: Check CORS configuration in backend SecurityConfig.java

#### Problem: TypeScript errors

**Solution**: Run type check
```bash
npm run type-check
# Fix any type errors shown
```

### Common Issues

#### Problem: "403 Forbidden" on API calls

**Solution**: Check JWT token
1. Verify you're logged in
2. Check token in browser localStorage
3. Token may be expired (24 hour expiration)
4. Log out and log back in

#### Problem: CORS errors in browser console

**Solution**: Check SecurityConfig.java CORS configuration:
```java
.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return config;
}))
```

## Development Tools

### Recommended IDE Setup

#### IntelliJ IDEA

1. Open backend folder as Maven project
2. Enable annotation processing (for Lombok)
3. Install plugins: Lombok, Spring Boot
4. Open frontend folder as separate project
5. Install plugins: ESLint, Prettier

#### VS Code

Install extensions:
- Java Extension Pack
- Spring Boot Extension Pack
- ESLint
- Prettier
- Tailwind CSS IntelliSense
- PostgreSQL (for database)

### Database Management

**pgAdmin** (GUI):
```bash
# Using Docker
docker run -d \
  -p 5050:80 \
  -e PGADMIN_DEFAULT_EMAIL=admin@example.com \
  -e PGADMIN_DEFAULT_PASSWORD=admin \
  dpage/pgadmin4

# Access at: http://localhost:5050
```

**DBeaver** (GUI):
Download from [dbeaver.io](https://dbeaver.io/)

**psql** (CLI):
```bash
# Connect to database
docker-compose exec postgres psql -U osgiliath -d osgiliath

# Useful commands:
\dt              # List tables
\d customers     # Describe table
\l               # List databases
\du              # List users
\q               # Quit
```

### API Testing

**Swagger UI**:
```
http://localhost:8080/api/swagger-ui.html
```

**Postman**:
1. Import OpenAPI spec from: `http://localhost:8080/api/v3/api-docs`
2. Set up environment with base URL: `http://localhost:8080/api`
3. Add authorization header: `Bearer <your-jwt-token>`

**cURL Examples**:
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Get customers (with token)
curl http://localhost:8080/api/customers \
  -H "Authorization: Bearer <your-jwt-token>"
```

## Next Steps

Now that your environment is set up:

1. Review [Architecture Documentation](ARCHITECTURE.md) to understand system design
2. Check [API Documentation](API.md) for complete endpoint reference
3. Read [Development Guide](DEVELOPMENT.md) for coding standards and workflow
4. See [Deployment Guide](DEPLOYMENT.md) for production deployment

## Support

If you encounter issues not covered in this guide:

1. Check application logs (backend console output)
2. Check browser console (frontend errors)
3. Review existing documentation in `docs/` folder
4. Check Docker logs: `docker-compose logs`
