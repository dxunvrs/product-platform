CREATE TYPE unit_of_measures AS ENUM (
    'METERS', 'SQUARE_METERS', 'PCS', 'MILLILITERS'
);

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    x NUMERIC NOT NULL,
    y INTEGER NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    price INTEGER NOT NULL,
    unit_of_measure unit_of_measures NOT NULL,
    owner_name TEXT NOT NULL,
    owner_birthday DATE NOT NULL,
    owner_height NUMERIC NOT NULL,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE
);