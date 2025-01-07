CREATE DATABASE IF NOT EXISTS treasure_hunt_db;
USE treasure_hunt_db;

CREATE TABLE player_score (
  id INT NOT NULL AUTO_INCREMENT,
  player_name VARCHAR(100),
  score INT,
  registered_at DATETIME,
  PRIMARY KEY (id)
);