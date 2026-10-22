CREATE TABLE IF NOT EXISTS barcode_flow_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  barcode VARCHAR(128) NOT NULL,
  business_name VARCHAR(128) NOT NULL,
  operation_type VARCHAR(32) NOT NULL,
  bill_no VARCHAR(128) NOT NULL,
  warehouse VARCHAR(128) NOT NULL,
  business_time DATETIME NOT NULL,
  record_status VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_barcode_time (barcode, business_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bill_trace_node (
  id VARCHAR(64) NOT NULL,
  bill_type VARCHAR(64) NOT NULL,
  bill_no VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bill_trace_edge (
  from_id VARCHAR(64) NOT NULL,
  to_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (from_id, to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sso_user_mapping (
  id BIGINT NOT NULL AUTO_INCREMENT,
  outer_user_id VARCHAR(128) NOT NULL,
  local_user_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_outer_user (outer_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sf_push_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  bill_id VARCHAR(64) NOT NULL,
  service_code VARCHAR(32) NOT NULL,
  success TINYINT NOT NULL,
  result_message VARCHAR(500) NOT NULL,
  msg_digest VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_bill_time (bill_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  detail VARCHAR(500) NOT NULL,
  success TINYINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_action_time (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
