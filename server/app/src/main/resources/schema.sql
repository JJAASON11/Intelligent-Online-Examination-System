CREATE TABLE IF NOT EXISTS question (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bank_id BIGINT,
  type VARCHAR(32),
  stem TEXT,
  options_json TEXT,
  answer_json TEXT,
  score INT,
  difficulty INT,
  tags VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

