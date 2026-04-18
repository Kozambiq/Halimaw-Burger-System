# Halimaw Burger — AGENTS.md

## Project Overview
- **Project name**: Halimaw Burger
- **Type**: Desktop POS/management application for a burger shop  
- **Stack**: JavaFX 17, MySQL (local dev, Aiven for future), Maven, JDK 17
- **Root package**: `com.myapp.halimawburgersystem`

---

## Quick Start

```bash
# Run the app
mvn javafx:run

# Compile
mvn clean compile
```

---

## Project Structure

```
src/main/
├── java/
│   ├── com/myapp/
│   │   ├── halimawburgersystem/
│   │   │   ├── Main.java           # Entry point
│   │   │   ├── LoginController.java
│   │   │   └── DashboardController.java
│   │   └── util/
│   │       └── DatabaseConnection.java
│   └── module-info.java
├── resources/
│   ├── fxml/
│   │   ├── login.fxml
│   │   └── dashboard.fxml
│   ├── css/
│   │   └── login.css             # All styling
│   └── com/myapp/halimawburgersystem/
│       └── db.properties          # DB credentials (gitignored)
```

---

## Database

### Local Dev (current)
- MySQL on localhost:3306
- Database: `burgerhq`
- Credentials: in `db.properties`

### Aiven (future)
When ready, update `db.properties`:
```properties
db.host=<aiven-host>
db.port=3306
db.name=burgerhq
db.user=<username>
db.password=<password>
db.ssl=true
db.ssl.mode=REQUIRED
```

### Schema
See `schema.md` at project root for full DB schema.

---

## Important Rules

### 1. DB credentials - NEVER commit
- `db.properties` is gitignored
- Never hardcode credentials in code
- Never push credentials to GitHub

### 2. Scene Navigation
- All scene switching through `Main.java` methods:
  - `Main.showLogin()` 
  - `Main.showDashboard()`

### 3. FXML + CSS Only
- All UI in `.fxml` files (resources/fxml/)
- All styling in `login.css` (resources/css/)
- No programmatic UI building

### 4. Controller Pattern
- Controllers handle UI events only
- Business logic goes in `service/` layer (future)
- Never load FXML in controllers directly

---

## Running Tests

```bash
# Build
mvn clean package

# Or run directly
mvn javafx:run
```

---

## Git Commands

```bash
# Commit all changes
git add -A
git commit -m "Your message"
git push origin master
```

**Important**: If GitHub blocks push for secrets in `db.properties`, the secret must have been committed before. Fix by:
1. Change the password in `db.properties` to placeholder values
2. Amend commit or create new commit
3. Push again

---

## Aiven Database (Future)

When switching to Aiven:
1. Update `db.properties` with Aiven credentials
2. Set `db.ssl=true` and `db.ssl.mode=REQUIRED`
3. Download CA cert if needed for SSL connections