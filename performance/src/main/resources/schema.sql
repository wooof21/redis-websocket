DROP TABLE IF EXISTS products;
CREATE TABLE products(
   -- serial: auto-generates IDs
   id serial PRIMARY KEY,
   description VARCHAR (500),
   price numeric (10,2) NOT NULL
);