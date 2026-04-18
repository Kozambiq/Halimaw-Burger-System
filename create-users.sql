USE burgerhq;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('Manager','Cashier','Cook') NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    last_login DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert test user (no foreign key for now)
INSERT INTO users (staff_id, email, password_hash, role) VALUES
(1, 'maria@halimaw.ph', 'admin123', 'Manager');
