-- Миграция V3: Удаление полей art_balance и avatar_url из таблицы users
--
-- Цель: упростить модель пользователя, оставить минимум данных,
-- чтобы не попадать под требования регистрации как оператор персональных данных.

ALTER TABLE users DROP COLUMN IF EXISTS art_balance;
ALTER TABLE users DROP COLUMN IF EXISTS avatar_url;
