# Osgiliath - Complete Setup Guide

## 🎯 TL;DR - Quick Start

**No API keys needed!** This project is 100% self-contained.

```bash
# Clone and enter directory
git clone <your-repo-url>
cd Osgiliath

# Start database
docker-compose up -d

# Terminal 1: Start backend
cd backend
./mvnw spring-boot:run

# Terminal 2: Start frontend
cd frontend
npm install
cp .env.local.example .env.local  # Already configured!
npm run dev

# Open browser: http://localhost:3000
# Login: testuser / password123
```

---

## ⚠️ IMPORTANT: No API Keys or External Services Required

**This project does NOT require:**
- ❌ OpenAI API keys
- ❌ AWS credentials
- ❌ Third-party API keys
- ❌ Cloud service accounts
- ❌ Payment gateway credentials

**Everything runs locally with:**
- ✅ PostgreSQL (via Docker)
- ✅ Spring Boot (Java backend)
- ✅ Next.js (React frontend)

---

## Prerequisites

### Required Software

1. **Java 17 or higher**
   ```bash
   # Check version
   java -version
   ```
   - Download: https://adoptium.net/
   - Set `JAVA_HOME` if needed (see troubleshooting below)

2. **Node.js 18+ and npm**
   ```bash
   # Check versions
   node -v   # Should be 18.x or higher
   npm -v    # Should be 9.x or higher
   ```
   - Download: https://nodejs.org/

3. **Docker & Docker Compose**
   ```bash
   # Check version
   docker --version
   docker-compose --version
   ```
   - Download: https://www.docker.com/products/docker-desktop

4. **Git**
   ```bash
   git --version
   ```
   - Download: https://git-scm.com/downloads

### Optional (but helpful)

- **Maven 3.9+** (or use included Maven wrapper `./mvnw`)
- **PostgreSQL client** (for database inspection)
- **Postman** or **curl** (for API testing)

---

## Step-by-Step Setup

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd Osgiliath
```

### 2. Start PostgreSQL Database

The project includes a `docker-compose.yml` that starts PostgreSQL with the correct configuration.

```bash
# Start PostgreSQL in background
docker-compose up -d

# Verify it's running
docker ps
# Should see: osgiliath-postgres (Up)

# Check logs if needed
docker-compose logs postgres
```

**Database Configuration** (automatically configured):
- Host: `localhost`
- Port: `5432`
- Database: `osgiliath`
- Username: `osgiliath`
- Password: `osgiliath_password`

### 3. Start the Backend (Spring Boot)

```bash
cd backend

# Build and run (first time or after changes)
./mvnw clean install
./mvnw spring-boot:run

# Or if you have Maven installed:
# mvn clean install
# mvn spring-boot:run
```

**Expected output:**
```
Started OsgiliathApplication in X.XXX seconds
```

**Backend will be available at:**
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- OpenAPI Docs: http://localhost:8080/api/v3/api-docs

**Test it:**
```bash
curl http://localhost:8080/api/health
# Should return: {"status":"UP"}
```

### 4. Configure Frontend Environment

The frontend needs to know where the backend API is.

```bash
cd frontend

# Copy the example env file (already configured for local development)
cp .env.local.example .env.local

# The file contains:
# NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

**That's it! No other environment variables needed.**

### 5. Start the Frontend (Next.js)

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

**Expected output:**
```
- ready started server on 0.0.0.0:3000, url: http://localhost:3000
- Local:        http://localhost:3000
```

**Frontend will be available at:**
- http://localhost:3000

### 6. Access the Application

1. Open your browser: http://localhost:3000
2. You'll see the login page

**Test Credentials** (automatically created by DataSeeder):
- Username: `testuser`
- Password: `password123`

**Or register a new account:**
- Click "Register" link
- Fill in username, email, password
- Submit and login

---

## Project Structure & Environment Files

### Backend Configuration

**File**: `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/osgiliath
    username: osgiliath
    password: osgiliath_password

jwt:
  secret: ${JWT_SECRET:osgiliath-secret-key-change-this-in-production-please-make-it-at-least-256-bits-long}
  expiration: 86400000  # 24 hours
```

**No changes needed for local development!**

For production, set environment variables:
- `DATABASE_URL` or `spring.datasource.url`
- `JWT_SECRET` (for security)

### Frontend Configuration

**File**: `frontend/.env.local` (copied from `.env.local.example`)

```
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

**No other variables needed!**

---

## Verification Checklist

After setup, verify everything is working:

### Backend Verification

```bash
# 1. Health check
curl http://localhost:8080/api/health
# Should return: {"status":"UP"}

# 2. Swagger UI (in browser)
open http://localhost:8080/api/swagger-ui.html
# Should show API documentation

# 3. Test login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
# Should return JWT token
```

### Frontend Verification

1. Open http://localhost:3000
2. Login with `testuser / password123`
3. Navigate to Dashboard
4. Try creating a customer
5. Try creating an invoice

### Database Verification

```bash
# Connect to PostgreSQL
docker exec -it osgiliath-postgres psql -U osgiliath -d osgiliath

# Check tables
\dt

# Check test user
SELECT * FROM users;

# Exit
\q
```

---

## Troubleshooting

### Problem: Backend won't start - "JAVA_HOME not set"

**Mac (with Homebrew Java):**
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./mvnw spring-boot:run
```

**Mac (manual Java install):**
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
./mvnw spring-boot:run
```

**Linux:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./mvnw spring-boot:run
```

**Windows:**
```cmd
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot
mvnw.cmd spring-boot:run
```

**Permanent fix (add to `~/.zshrc` or `~/.bashrc`):**
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
```

### Problem: Port 8080 already in use

**Check what's using port 8080:**
```bash
# Mac/Linux
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

**Stop the process or change backend port:**

Edit `backend/src/main/resources/application.yml`:
```yaml
server:
  port: 8081  # Change to any available port
```

Then update frontend `.env.local`:
```
NEXT_PUBLIC_API_URL=http://localhost:8081/api
```

### Problem: Port 3000 already in use

```bash
# Kill process on port 3000 (Mac/Linux)
lsof -ti:3000 | xargs kill -9

# Or start frontend on different port
PORT=3001 npm run dev
```

### Problem: Port 5432 already in use (PostgreSQL)

```bash
# Check if local PostgreSQL is running
ps aux | grep postgres

# Option 1: Stop local PostgreSQL
brew services stop postgresql  # Mac
sudo service postgresql stop   # Linux

# Option 2: Change Docker port
# Edit docker-compose.yml:
ports:
  - "5433:5432"  # Use 5433 instead

# Then update backend application.yml:
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/osgiliath
```

### Problem: Database connection refused

```bash
# Check PostgreSQL is running
docker ps
# Should see: osgiliath-postgres (Up)

# If not running:
docker-compose up -d

# Check logs
docker-compose logs postgres

# Restart database
docker-compose restart postgres
```

### Problem: Frontend shows "Network Error"

**Cause**: Backend not running or wrong API URL

**Fix:**
1. Check backend is running: `curl http://localhost:8080/api/health`
2. Check `.env.local` has correct URL: `NEXT_PUBLIC_API_URL=http://localhost:8080/api`
3. Restart frontend: `npm run dev`

### Problem: "Failed to login" or "Invalid credentials"

**Cause**: Database not initialized or test user not created

**Fix:**
```bash
# Check database has users
docker exec -it osgiliath-postgres psql -U osgiliath -d osgiliath -c "SELECT * FROM users;"

# If no users, restart backend (DataSeeder will create testuser automatically)
# Ctrl+C to stop backend, then:
./mvnw spring-boot:run
```

### Problem: Maven build fails with compilation errors

```bash
# Clean Maven cache and rebuild
./mvnw clean install -U

# Skip tests if needed
./mvnw clean install -DskipTests
```

### Problem: npm install fails

```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Reinstall
npm install
```

### Problem: Docker can't start - "port is already allocated"

```bash
# Stop all containers
docker-compose down

# Remove containers
docker-compose rm -f

# Start fresh
docker-compose up -d
```

---

## Running Tests

### Backend Tests

```bash
cd backend

# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=CustomerIntegrationTest

# Run integration tests only
./mvnw test -Dtest=*IntegrationTest

# With coverage report
./mvnw test jacoco:report
# Report: target/site/jacoco/index.html
```

### Frontend Type Checking

```bash
cd frontend

# Type check
npm run type-check

# Lint
npm run lint

# Lint and fix
npm run lint:fix
```

---

## Stopping the Application

### Stop All Services

```bash
# Stop backend: Ctrl+C in terminal

# Stop frontend: Ctrl+C in terminal

# Stop database
docker-compose down

# Or keep database running for next time
```

### Clean Stop (remove all data)

```bash
# Stop and remove containers, volumes
docker-compose down -v

# This will delete all database data!
```

---

## Development Workflow

### Making Changes

**Backend Changes:**
1. Edit Java files
2. Stop backend (`Ctrl+C`)
3. Restart: `./mvnw spring-boot:run`
4. Or use IDE with hot reload

**Frontend Changes:**
1. Edit TypeScript/React files
2. Next.js hot reload (automatic)
3. Browser refreshes automatically

**Database Changes:**
1. Edit entities in `backend/src/main/java/com/osgiliath/domain/`
2. Hibernate will auto-update schema on restart (`ddl-auto: update`)

### Adding New Features

1. Backend: Add command/query handlers in `application/`
2. Backend: Add REST endpoints in `api/`
3. Frontend: Add services in `services/`
4. Frontend: Add ViewModels in `viewmodels/`
5. Frontend: Add pages in `app/`

---

## Performance & Optimization

### Backend Performance

**Connection Pooling** (already configured):
- HikariCP (Spring Boot default)
- Max pool size: 10 connections

**Database Indexes** (already created):
- Primary keys (automatic)
- Foreign keys
- `invoices.status`
- `invoices.due_date`
- `invoices.customer_id`

### Frontend Performance

**Next.js Optimizations** (already enabled):
- Automatic code splitting
- Server-side rendering
- Static generation for public pages
- Image optimization

---

## Security Notes

### Development vs Production

**Development** (current setup):
- JWT secret: Default (insecure)
- Database: localhost (insecure password)
- CORS: Allows localhost:3000
- SSL: Not enabled

**Production** (for deployment):
- JWT secret: Strong 256+ bit key (environment variable)
- Database: Managed service with strong password
- CORS: Specific domains only
- SSL: Required (HTTPS)

See `DEPLOYMENT_GUIDE.md` for production setup.

---

## What's Next?

After successful setup:

1. **Explore the Application**:
   - Create customers
   - Create invoices with line items
   - Record payments
   - Export PDFs

2. **Review the Code**:
   - Backend: `backend/src/main/java/com/osgiliath/`
   - Frontend: `frontend/src/`

3. **Read the Documentation**:
   - `TECHNICAL_WRITEUP.md` - Architecture deep dive
   - `DEPLOYMENT_GUIDE.md` - Deploy to production
   - `PERFORMANCE_BENCHMARK_RESULTS.md` - Performance testing

4. **Run Performance Benchmarks**:
   ```bash
   ./performance-benchmark.sh
   ```

5. **Deploy to Production**:
   - Follow `DEPLOYMENT_GUIDE.md`
   - Railway + Vercel (easiest)
   - Or AWS (more control)

---

## Getting Help

### Common Resources

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **Database UI**: Use TablePlus, DBeaver, or pgAdmin
- **API Testing**: Use Postman or curl
- **Logs**: Check terminal output

### Log Locations

- **Backend**: Console output (terminal)
- **Frontend**: Console output + browser console (F12)
- **Database**: `docker-compose logs postgres`

---

## Summary

**You now have:**
- ✅ PostgreSQL running in Docker
- ✅ Spring Boot backend on port 8080
- ✅ Next.js frontend on port 3000
- ✅ Test user account (testuser/password123)
- ✅ Full invoice management system

**No API keys or external services needed!**

**Next steps:**
1. Login and explore the application
2. Review the code and architecture
3. Run tests and benchmarks
4. Deploy to production (if desired)

---

**Need more help?** Check the other documentation files:
- `README.md` - Project overview
- `TECHNICAL_WRITEUP.md` - Architecture details
- `DEPLOYMENT_GUIDE.md` - Production deployment
- `PERFORMANCE_BENCHMARK_RESULTS.md` - Performance metrics
