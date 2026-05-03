# Code Audit Report - Halimaw Burger System

## Executive Summary

The codebase is a JavaFX-based POS system for a burger restaurant. It follows a basic MVC-like pattern with Controllers, DAOs, and Models. The application has **moderate technical debt** with several areas requiring attention. The most critical concerns are around security (hardcoded credentials), code duplication (repeated navigation logic across controllers), and excessively large controller files that violate single responsibility.

---

## 1. Security Issues

### 1.1 Hardcoded Database Credentials

| Attribute | Value |
|-----------|-------|
| **Location** | `src/main/resources/com/myapp/halimawburgersystem/db.properties` |
| **Severity** | **Critical** |

The database credentials are stored in plain text:

```
db.user=root
db.password=kozambiq09
```

**Suggested Fix:** Move credentials to environment variables or use a secure credential management approach. The `.env` pattern used for `GEMINI_API_KEY` should be extended to database credentials.

---

### 1.2 Plain Text Password Fallback

| Attribute | Value |
|-----------|-------|
| **Location** | `src/main/java/com/myapp/dao/UserDAO.java:122` |
| **Severity** | **High** |

```java
// Plain text comparison for backwards compatibility
return password.equals(storedHash);
```

This allows fallback to plain text comparison, creating a severe security vulnerability if any legacy password hashes exist in the database.

**Suggested Fix:** Remove this fallback entirely. Force migration of all passwords to BCrypt format.

---

### 1.3 Unused Import - Reflection

| Attribute | Value |
|-----------|-------|
| **Location** | `src/main/java/com/myapp/dao/UserDAO.java:7` |
| **Severity** | **Low** |

```java
import java.lang.reflect.Method;
```

This import is never used.

**Suggested Fix:** Remove the unused import.

---

### 1.4 .env API Key Exposure

| Attribute | Value |
|-----------|-------|
| **Location** | `.env` (root directory) |
| **Severity** | **Medium** |

The `GEMINI_API_KEY` is stored in a plain text `.env` file:

```
aa
```

While this file is in `.gitignore`, it represents a secret that could be accidentally committed or exposed.

**Suggested Fix:** Consider using a proper secrets management approach or ensuring strict `.gitignore` enforcement.

---

## 2. Redundant Code

### 2.1 Duplicate `setActiveNav()` Method

| Attribute | Value |
|-----------|-------|
| **Location** | Repeated in 9+ controllers |
| **Severity** | **High** |

The `setActiveNav()` method is duplicated verbatim in:

- `StaffController.java` (line 971)
- `CombosController.java` (line 154)
- `InventoryController.java` (line 164)
- `MenuItemsController.java` (line 492)
- `OrdersController.java` (line 470)
- `KitchenController.java` (line 254)
- `DashboardController.java` (line 429)
- `CashierController.java` (line 833)
- `SalesReportController.java` (line 87)

This method handles navigation highlighting and is ~25 lines of identical code in each controller.

**Suggested Fix:** Extract to a base controller class or utility class.

---

### 2.2 Duplicate `clearAllHighlights()` Method

| Attribute | Value |
|-----------|-------|
| **Location** | Repeated in 7+ controllers |
| **Severity** | **High** |

The `clearAllHighlights()` method is duplicated in the same controllers as above (~10 lines each).

**Suggested Fix:** Same as above - extract to shared location.

---

### 2.3 Duplicate Navigation Button Declarations

| Attribute | Value |
|-----------|-------|
| **Location** | Every controller |
| **Severity** | **Medium** |

Every controller declares the same 8 navigation buttons:

```java
@FXML private Button btnDashboard;
@FXML private Button btnOrders;
@FXML private Button btnKitchen;
@FXML private Button btnMenuItems;
@FXML private Button btnCombos;
@FXML private Button btnInventory;
@FXML private Button btnSales;
@FXML private Button btnStaff;
```

**Suggested Fix:** Create a base controller or use FXML inheritance with fx:include.

---

### 2.4 Duplicate SQL Queries in OrderDAO

| Attribute | Value |
|-----------|-------|
| **Location** | `OrderDAO.java` |
| **Severity** | **Low** |

Lines 67-99 (`findByDateRange`) and lines 138-171 (`findAll`) contain nearly identical logic with duplicated GROUP_CONCAT subqueries.

**Suggested Fix:** Extract common query logic to a helper method.

---

## 3. Unnecessary Code

### 3.1 No Test Files

| Attribute | Value |
|-----------|-------|
| **Location** | Entire project |
| **Severity** | **Info** |

No test files exist in this project. While not "unnecessary," this represents significant risk for a production system.

---

### 3.2 Incomplete Scene Cache Cleanup

| Attribute | Value |
|-----------|-------|
| **Location** | `Main.java` |
| **Severity** | **Low** |

`Main.java` caches FXML scenes but only provides `clearCookCache()`, `clearCashierCache()`, `clearInventoryCache()`, etc. Some cached scenes like `loginRoot` are never explicitly cleared.

---

### 3.3 Unused Fields in Controllers

| Attribute | Value |
|-----------|-------|
| **Location** | Various controllers |es FXML scenes but only provides
| **Severity** | **Low** |

Some controllers may have unused FXML-injected fields or local variables.

---

## 4. Bad Practices

### 4.1 Inline CSS Styles Repeated

| Attribute | Value |
|-----------|-------|
| **Location** | `LoginController.java:36-37, 118-119` |
| **Severity** | **Medium** |

```java
String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; ...";
String errorBorderStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; ...";
```

These style strings are defined twice in the same file.

**Suggested Fix:** Define as constants at class level.

---

### 4.2 Inadequate Email Validation

| Attribute | Value |
|-----------|-------|
| **Location** | `LoginController.java:88-90` |
| **Severity** | **Medium** |

```java
private boolean isValidEmail(String email) {
    return email.contains("@") && email.indexOf('@') < email.lastIndexOf('.')
        && email.lastIndexOf('.') > email.indexOf('@');
}
```

This validation is too simplistic and allows invalid emails like `a@b.c` or `@.`.

**Suggested Fix:** Use `java.util.regex.Pattern` for proper email validation.

---

### 4.3 Inconsistent Error Handling

| Attribute | Value |
|-----------|-------|
| **Location** | Multiple DAOs |
| **Severity** | **Medium** |

DAOs catch exceptions but only log to `System.err`, returning default values or null without any differentiation between "not found" and "database error."

**Suggested Fix:** Consider using a Result type or specific exception types.

---

### 4.4 No Input Validation in StaffDAO Insert

| Attribute | Value |
|-----------|-------|
| **Location** | `StaffDAO.java:145` |
| **Severity** | **Medium** |

The `insert()` method doesn't validate inputs like email format, password strength, or name length before inserting into the database.

---

### 4.5 Magic Strings Throughout

| Attribute | Value |
|-----------|-------|
| **Location** | Multiple files |
| **Severity** | **Low** |

Status values like `"Active"`, `"Off Shift"`, `"Break"`, `"Disabled"` are used as magic strings throughout the codebase without constants.

**Suggested Fix:** Create an enum or constants class for status values.

---

## 5. Code Structure

### 5.1 Controller Size Violations

| **Severity** | **High** |

| Controller | Lines | Assessment |
|------------|-------|------------|
| InventoryController | 1186 | Too large - multiple responsibilities |
| MenuItemsController | 1118 | Too large - multiple responsibilities |
| StaffController | 1093 | Too large - multiple responsibilities |
| CashierController | 844 | Large but may be justified |
| CombosController | 763 | Large but may be justified |

These controllers violate the Single Responsibility Principle. They handle:

- UI event handling
- Business logic
- Database operations via DAO calls
- UI state management
- Navigation

**Suggested Fix:** Extract business logic to service classes. Use FXML `fx:include` for common UI components.

---

### 5.2 No Base Controller Class

| **Severity** | **High** |

With 9+ controllers sharing identical `setActiveNav()` and `clearAllHighlights()` logic, there should be a base controller class.

**Suggested Fix:**

```java
public abstract class BaseController {
    protected void setActiveNav(String navName) { ... }
    protected void clearAllHighlights() { ... }
}
```

---

### 5.3 Business Logic Mixed with UI Code

| **Severity** | **Medium** |

Controllers contain business logic that should be in service classes. Example from `InventoryController.java:239-297`:

- `reserveIngredientsForOrder()` logic in controller
- Direct calls to `IngredientDAO` and `MenuItemDAO`

**Suggested Fix:** Create an `InventoryService` class to handle these operations.

---

### 5.4 No Interfaces for DAOs

| **Severity** | **Low** |

DAOs are concrete classes with no interface abstraction. This makes testing and future changes more difficult.

---

### 5.5 MenuItemDAO Inner Class

| **Severity** | **Low** |

| **Location** | `MenuItemDAO.java:17-35` |

`MenuItemIngredient` is a public inner class that should be a top-level class or moved to the model package.

---

## 6. File & Folder Structure

### 6.1 Flat Controller Package

| **Severity** | **Low** |

| **Location** | `src/main/java/com/myapp/halimawburgersystem/` |

All 13 controllers are in the same package. While functionally acceptable, a structure like:

```
halimawburgersystem/
  controllers/
  views/
```

Would be cleaner.

---

### 6.2 FXML Co-location

| **Severity** | **Info** |

| **Location** | `src/main/resources/fxml/` vs controllers |

FXML files are in `resources/fxml/` while controllers are in `java/halimawburgersystem/`. This is standard for JavaFX but makes it harder to see controller-view relationships at a glance.

---

### 6.3 Missing Service Layer

| **Severity** | **Medium** |

The project has:

- `dao/` - Data access
- `model/` - POJOs
- `util/` - Utilities

But missing:

- `service/` - Business logic

Business logic is currently embedded in Controllers.

---

### 6.4 Proper Package Structure

| **Severity** | **Info** |

The `dao/`, `model/`, and `util/` packages are properly structured and follow conventions. Only the controller package is flat.

---

## Summary

### Overall Health: **Fair**

The application functions but has significant technical debt:

| Category | Assessment |
|----------|------------|
| Security | Needs attention (hardcoded credentials, password fallback) |
| Redundancy | High (duplicated navigation logic) |
| Unnecessary | Low |
| Bad Practices | Moderate (large controllers, inline styles) |
| Code Structure | Needs improvement (no base class, mixed responsibilities) |
| File Structure | Acceptable (minor improvements possible) |

### Top 5 Priority Issues

1. **Critical:** Remove hardcoded database credentials from `db.properties`
2. **Critical:** Remove plain text password fallback in `UserDAO`
3. **High:** Extract duplicated `setActiveNav()`/`clearAllHighlights()` to base class
4. **High:** Break up large controllers (InventoryController, MenuItemsController, StaffController)
5. **Medium:** Add proper email validation

### Recurring Patterns

- **Duplicated navigation logic** across all controllers (9+ occurrences)
- **Magic strings** for status values used everywhere
- **Large controller classes** containing UI, business logic, and database access
- **No test coverage** despite business-critical functionality
- **Inconsistent error handling** across DAOs

---

## Tools & Skills Used

This audit was performed using:

- **Manual code review** - Read through all DAO, controller, model, and utility files
- **File traversal** - Mapped complete project structure
- **Pattern search (grep)** - Found duplicate methods like `setActiveNav()`, `clearAllHighlights()`
- **Line counting** - Identified oversized controller files
- **Cross-reference analysis** - Traced dependencies between components

No automated static analysis tools were used as none are configured for this project. A tool like SpotBugs or SonarQube could potentially surface additional issues.

---

*Report generated: May 2026*