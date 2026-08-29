-- YENİDEN Veritabanı İlklendirme Betiği
-- PostgreSQL + PostGIS şemalarını otomatik oluşturur

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Mikroservis Şemaları (Database-per-service mantığıyla şema izolasyonu)
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS exchange;
CREATE SCHEMA IF NOT EXISTS ecocoin;
CREATE SCHEMA IF NOT EXISTS gamification;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS moderation;
CREATE SCHEMA IF NOT EXISTS wasteai;

GRANT ALL PRIVILEGES ON DATABASE yeniden_db TO postgres;
