PRAGMA foreign_keys = ON;

CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    icon TEXT NOT NULL,
    isActive INTEGER NOT NULL DEFAULT 1,
    createdAtEpochMillis INTEGER NOT NULL,
    UNIQUE(name, type)
);

CREATE TABLE transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    amountInCents INTEGER NOT NULL CHECK (amountInCents > 0),
    type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    categoryId INTEGER NOT NULL,
    description TEXT,
    dateEpochDay INTEGER NOT NULL,
    createdAtEpochMillis INTEGER NOT NULL,
    updatedAtEpochMillis INTEGER NOT NULL,
    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE INDEX index_transactions_categoryId ON transactions(categoryId);
CREATE INDEX index_transactions_dateEpochDay ON transactions(dateEpochDay);
