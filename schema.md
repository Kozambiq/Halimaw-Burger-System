# Halimaw Burger - Database Schema

## Database Setup

```sql
CREATE DATABASE IF NOT EXISTS burgerhq;
USE burgerhq;
```

### Migration (for existing databases)
```sql
ALTER TABLE ingredients ADD COLUMN max_stock DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER min_threshold;
ALTER TABLE ingredients ADD COLUMN status ENUM('Available','Unavailable') NOT NULL DEFAULT 'Available' AFTER max_stock;
ALTER TABLE staff ADD COLUMN email VARCHAR(100) AFTER name;
ALTER TABLE staff MODIFY COLUMN status ENUM('Active','Break','Off Shift','Disabled') NOT NULL DEFAULT 'Off Shift';
```

## Tables

```sql
CREATE TABLE IF NOT EXISTS staff (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(100),
    role        ENUM('Manager','Cashier','Cook') NOT NULL,
    shift_start TIME,
    shift_end   TIME,
    status      ENUM('Active','Break','Off Shift','Disabled') NOT NULL DEFAULT 'Off Shift',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE IF NOT EXISTS menu_items (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)   NOT NULL,
    category     ENUM('Burgers','Chicken','Sides','Drinks','Others') NOT NULL,
    price        DECIMAL(10,2)  NOT NULL,
    availability ENUM('Available','Low Stock','Out of Stock','Unavailable') NOT NULL DEFAULT 'Available',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ingredients (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    unit          VARCHAR(20)   NOT NULL,
    quantity      DECIMAL(10,2) NOT NULL DEFAULT 0,
    min_threshold DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_stock     DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS menu_item_ingredients (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id   INT           NOT NULL,
    ingredient_id  INT           NOT NULL,
    quantity_used  DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (menu_item_id)  REFERENCES menu_items(id)  ON DELETE CASCADE,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS restock_logs (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    ingredient_id  INT           NOT NULL,
    quantity_added DECIMAL(10,2) NOT NULL,
    restocked_by   INT           NOT NULL,
    restocked_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
    FOREIGN KEY (restocked_by)  REFERENCES staff(id)
);

CREATE TABLE IF NOT EXISTS combos (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    includes      VARCHAR(500)  NOT NULL,
    promo_price   DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2) NOT NULL,
    valid_until   DATE,
    status        ENUM('Active','Scheduled','Expired') NOT NULL DEFAULT 'Active',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## Seed Data

```sql
INSERT INTO staff (name, email, role, shift_start, shift_end, status) VALUES
('Maria G.',  'maria@halimaw.ph',  'Manager', '08:00:00', '18:00:00', 'Active'),
('Jose R.',   'jose@halimaw.ph',   'Cashier', '09:00:00', '17:00:00', 'Active'),
('Ana L.',    'ana@halimaw.ph',    'Cook',    '10:00:00', '20:00:00', 'Break'),
('Ben P.',    'ben@halimaw.ph',    'Cashier', '08:00:00', '16:00:00', 'Active'),
('Carlo S.',  'carlo@halimaw.ph',  'Cook',    '10:00:00', '20:00:00', 'Active');

INSERT INTO menu_items (name, category, price, availability) VALUES
('Halimaw Burger',   'Burgers', 185.00, 'Available'),
('Double Smash',     'Burgers', 195.00, 'Available'),
('Classic Burger',   'Burgers', 145.00, 'Low Stock'),
('BBQ Bacon Burger', 'Burgers', 210.00, 'Hidden'),
('Crispy Chicken',   'Chicken', 160.00, 'Available'),
('Classic Fries',    'Sides',    65.00, 'Available'),
('Onion Rings',      'Sides',    75.00, 'Available'),
('Iced Tea',         'Drinks',   55.00, 'Available'),
('Coleslaw',         'Sides',    45.00, 'Available');

INSERT INTO ingredients (name, unit, quantity, min_threshold, max_stock) VALUES
('Beef Patty',     'pcs',  50,    20,   100),
('Bun',            'pcs',  60,    20,   100),
('Cheddar',        'pcs',  40,    15,    80),
('Lettuce',        'g',   500,   200,  1000),
('Tomato',         'pcs',  30,    10,    50),
('Chicken Fillet', 'pcs',  25,    10,    50),
('Potato',         'g',  2000,  500,  5000),
('Cooking Oil',    'L',     5,    2,    10),
('Onion',          'pcs',  20,     8,    40),
('Ketchup',        'g',   300,  100,   500),
('Mayo',           'g',   200,    80,   400),
('BBQ Sauce',      'g',   150,    50,   300);

INSERT INTO menu_item_ingredients (menu_item_id, ingredient_id, quantity_used) VALUES
(1, 1,  1),    -- Halimaw Burger  → 1 Beef Patty
(1, 2,  1),    -- Halimaw Burger  → 1 Bun
(1, 3,  2),    -- Halimaw Burger  → 2 Cheddar
(1, 4, 30),    -- Halimaw Burger  → 30g Lettuce
(1, 5,  1),    -- Halimaw Burger  → 1 Tomato
(2, 1,  2),    -- Double Smash    → 2 Beef Patties
(2, 2,  1),    -- Double Smash    → 1 Bun
(2, 3,  2),    -- Double Smash    → 2 Cheddar
(3, 1,  1),    -- Classic Burger  → 1 Beef Patty
(3, 2,  1),    -- Classic Burger  → 1 Bun
(3, 4, 20),    -- Classic Burger  → 20g Lettuce
(5, 6,  1),    -- Crispy Chicken  → 1 Chicken Fillet
(6, 7, 150),   -- Classic Fries   → 150g Potato
(6, 8, 0.05);  -- Classic Fries   → 0.05L Cooking Oil

`INSERT INTO users (staff_id, email, password_hash, role, is_active) VALUES
(1, 'maria@halimaw.ph', 'admin123', 'Manager', 1);`
```

## Default Credentials

- Email: `maria@halimaw.ph`
- Password: `admin123`

## Role Permissions

| Role | Permissions |
|------|-------------|
| Manager | Full access - all CRUD, reports, staff management |
| Cashier | Process orders, view menu, daily sales |
| Cook | View orders, update inventory, restock |