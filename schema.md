# Halimaw Burger - Database Schema

## Database Setup

Run this SQL to create the database and tables:

```sql
CREATE DATABASE IF NOT EXISTS burgerhq;
USE burgerhq;
```

## Tables

### staff
Employees table with roles (Manager, Cashier, Cook)
```sql
CREATE TABLE IF NOT EXISTS staff (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    role        ENUM('Manager','Cashier','Cook') NOT NULL,
    shift_start TIME,
    shift_end   TIME,
    status      ENUM('Active','Break','Off Shift') NOT NULL DEFAULT 'Off Shift',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### users
Login credentials linked to staff
```sql
CREATE TABLE IF NOT EXISTS users (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    staff_id        INT           NOT NULL UNIQUE,
    email           VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('Manager','Cashier','Cook') NOT NULL,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    last_login      DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
);
```

### menu_items
```sql
CREATE TABLE IF NOT EXISTS menu_items (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)   NOT NULL,
    category     ENUM('Burgers','Chicken','Sides','Drinks','Others') NOT NULL,
    price        DECIMAL(10,2)  NOT NULL,
    availability ENUM('Available','Low Stock','Hidden') NOT NULL DEFAULT 'Available',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### ingredients
```sql
CREATE TABLE IF NOT EXISTS ingredients (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    unit          VARCHAR(20)   NOT NULL,
    quantity      DECIMAL(10,2) NOT NULL DEFAULT 0,
    min_threshold DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### menu_item_ingredients
```sql
CREATE TABLE IF NOT EXISTS menu_item_ingredients (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id   INT           NOT NULL,
    ingredient_id  INT           NOT NULL,
    quantity_used  DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (menu_item_id)  REFERENCES menu_items(id)  ON DELETE CASCADE,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE CASCADE
);
```

### restock_logs
```sql
CREATE TABLE IF NOT EXISTS restock_logs (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    ingredient_id  INT           NOT NULL,
    quantity_added DECIMAL(10,2) NOT NULL,
    restocked_by   INT           NOT NULL,
    restocked_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
    FOREIGN KEY (restocked_by)  REFERENCES staff(id)
);
```

## Seed Data

### Staff
```sql
INSERT INTO staff (name, role, shift_start, shift_end, status) VALUES
('Maria G.',  'Manager', '08:00:00', '18:00:00', 'Active'),
('Jose R.',   'Cashier', '09:00:00', '17:00:00', 'Active'),
('Ana L.',    'Cook',    '10:00:00', '20:00:00', 'Break'),
('Ben P.',    'Cashier', '08:00:00', '16:00:00', 'Active'),
('Carlo S.',  'Cook',    '10:00:00', '20:00:00', 'Active');
```

### Users (Default Login)
```sql
-- Default manager account (password: admin123)
INSERT INTO users (staff_id, email, password_hash, role) VALUES
(1, 'maria@halimaw.ph', '$2a$10$xGJ9Q7K5P3X8Y2Z0N5V9B.O9Y1U5O3J0W1X9Y5Z0N2V8U1O3J', 'Manager');
```

**Default Credentials:**
- Email: `maria@halimaw.ph`
- Password: `admin123`

### Menu Items
```sql
INSERT INTO menu_items (name, category, price, availability) VALUES
('Halimaw Burger',  'Burgers', 185.00, 'Available'),
('Double Smash',    'Burgers', 195.00, 'Available'),
('Classic Burger',  'Burgers', 145.00, 'Low Stock'),
('BBQ Bacon Burger','Burgers', 210.00, 'Hidden'),
('Crispy Chicken',  'Chicken', 160.00, 'Available'),
('Classic Fries',   'Sides',   65.00,  'Available'),
('Onion Rings',     'Sides',   75.00,  'Available'),
('Iced Tea',        'Drinks',  55.00,  'Available'),
('Coleslaw',        'Sides',   45.00,  'Available');
```

### Ingredients
```sql
INSERT INTO ingredients (name, unit, quantity, min_threshold) VALUES
('Burger Patty (Beef)', 'pcs',   0,    20),
('Burger Buns',         'pcs',   8,    30),
('Cheddar Cheese',      'slices',12,   40),
('Lettuce',             'g',     200,  500),
('Tomatoes',            'pcs',   0,    15),
('Chicken Fillet',      'pcs',   42,   20),
('Potato (Fries)',      'kg',    5.1,  2),
('Cooking Oil',         'L',     4.5,  2),
('Ketchup',             'btl',   3,    5),
('Onion',               'pcs',   24,  10),
('Burger Sauce',        'g',     800,  300),
('Salt',                'g',     1200, 200);
```

## Role Permissions

| Role | Permissions |
|------|-------------|
| Manager | Full access - all CRUD, reports, staff management |
| Cashier | Process orders, view menu, daily sales |
| Cook | View orders, update inventory, restock |