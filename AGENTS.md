# AGENTS.md - Halimaw Burger System

## Project Overview
- **Language:** Java 14
- **UI Framework:** JavaFX 21
- **Database:** MariaDB via JDBC
- **Build Tool:** Maven

## Build Commands

```bash
# Run the application
mvn clean javafx:run

# Build
mvn clean package
```

## Project Structure

```
src/main/
├── java/com/myapp/
│   ├── halimawburgersystem/   # Controllers (LoginController, DashboardController, etc.)
│   ├── dao/                   # Data Access Objects (OrderDAO, MenuItemDAO, etc.)
│   ├── model/                 # POJOs (Order, User, Staff, etc.)
│   └── util/                  # Utilities (DatabaseConnection, EnvLoader, etc.)
└── resources/
    ├── fxml/                   # FXML views
    ├── css/                    # Stylesheets
    └── com/myapp/halimawburgersystem/db.properties  # Database config
```

## Configuration

- **Database config:** `src/main/resources/com/myapp/halimawburgersystem/db.properties`
- **Environment vars:** `.env` file in root (contains GEMINI_API_KEY - do not commit)
- **Entry point:** `com.myapp.halimawburgersystem.Main`

## Key Patterns

1. **Controllers** handle JavaFX logic and coordinate between views and DAOs
2. **DAOs** handle all database operations using raw JDBC
3. **Models** are simple POJOs
4. **FXML files** located in `src/main/resources/fxml/` and mapped to controllers via `Main.java`
5. Scene switching uses static cache in `Main.java` (`showLogin()`, `showDashboard()`, etc.)

## Important Notes

- No tests exist in this project
- No static analysis or linting configured
- `.gitignore` excludes `target/` and `.idea/`
- The API key in `.env` should NOT be committed (already in .gitignore)

## Code Review Context

When analyzing this codebase, apply these guidelines:

- **Security:** Check for hardcoded secrets, SQL injection in DAO files, proper password handling
- **Architecture:** Controllers are large (~30-50KB files) - assess single responsibility
- **Database:** Raw JDBC with no ORM - review connection management and SQL injection risks
- **JavaFX:** Controllers contain business logic mixed with UI code - typical for JavaFX MVC