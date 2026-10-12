CREATE DATABASE IF NOT EXISTS catalog_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS inventory_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS '${appUser}'@'%' IDENTIFIED BY '${appPassword}';
ALTER USER '${appUser}'@'%' IDENTIFIED BY '${appPassword}';
GRANT ALL PRIVILEGES ON catalog_db.* TO '${appUser}'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO '${appUser}'@'%';
FLUSH PRIVILEGES;
