CREATE TABLE app_user (
  id char(36) NOT NULL DEFAULT (uuid()),
  username varchar(50) NOT NULL,
  password_hash varchar(100) NOT NULL,
  role varchar(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
