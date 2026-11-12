CREATE TABLE author (
  id char(36) NOT NULL DEFAULT (uuid()),
  name varchar(100) NOT NULL,
  nationality varchar(50) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE member (
  id char(36) NOT NULL DEFAULT (uuid()),
  full_name varchar(100) NOT NULL,
  email varchar(150) NOT NULL,
  join_date date NOT NULL DEFAULT (curdate()),
  PRIMARY KEY (id),
  UNIQUE KEY email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE book (
  id char(36) NOT NULL DEFAULT (uuid()),
  author_id char(36) NOT NULL,
  title varchar(200) NOT NULL,
  isbn varchar(20) DEFAULT NULL,
  total_copies int NOT NULL DEFAULT 1,
  available_copies int NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY isbn (isbn),
  KEY fk_book_author (author_id),
  CONSTRAINT fk_book_author FOREIGN KEY (author_id) REFERENCES author (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE loan (
  id char(36) NOT NULL DEFAULT (uuid()),
  book_id char(36) NOT NULL,
  member_id char(36) NOT NULL,
  borrow_date date NOT NULL DEFAULT (curdate()),
  due_date date NOT NULL,
  return_date date DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_loan_book (book_id),
  KEY fk_loan_member (member_id),
  CONSTRAINT fk_loan_book FOREIGN KEY (book_id) REFERENCES book (id),
  CONSTRAINT fk_loan_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
