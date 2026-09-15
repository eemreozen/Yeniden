CREATE SCHEMA IF NOT EXISTS catalog;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS catalog.categories (
    id uuid PRIMARY KEY,
    parent_id uuid,
    code varchar(50) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    reusable boolean NOT NULL DEFAULT true,
    coin_multiplier double precision NOT NULL DEFAULT 1.0,
    sort_order integer NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS catalog.listings (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL,
    category_id uuid NOT NULL,
    title varchar(150) NOT NULL,
    description varchar(1000),
    quantity_band varchar(255) NOT NULL,
    condition varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    approx_latitude double precision NOT NULL,
    approx_longitude double precision NOT NULL,
    neighborhood_id uuid,
    reserved_request_id uuid,
    published_at timestamp,
    expires_at timestamp,
    created_at timestamp NOT NULL,
    updated_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_catalog_listings_status_published
    ON catalog.listings (status, published_at DESC);
CREATE INDEX IF NOT EXISTS idx_catalog_listings_owner_status
    ON catalog.listings (owner_id, status);
CREATE INDEX IF NOT EXISTS idx_catalog_listings_category
    ON catalog.listings (category_id);

INSERT INTO catalog.categories (id, parent_id, code, name, reusable, coin_multiplier, sort_order) VALUES
    ('11111111-1111-1111-1111-111111111101', NULL, 'AMBALAJ', 'Ambalaj', true, 1.0, 1),
    ('11111111-1111-1111-1111-111111111102', '11111111-1111-1111-1111-111111111101', 'AMBALAJ_KOLI', 'Koli', true, 1.0, 1),
    ('11111111-1111-1111-1111-111111111103', '11111111-1111-1111-1111-111111111101', 'AMBALAJ_KARTON', 'Karton Kutu', true, 1.0, 2),
    ('11111111-1111-1111-1111-111111111201', NULL, 'CAM', 'Cam', true, 1.2, 2),
    ('11111111-1111-1111-1111-111111111202', '11111111-1111-1111-1111-111111111201', 'CAM_KAVANOZ', 'Kavanoz', true, 1.2, 1),
    ('11111111-1111-1111-1111-111111111203', '11111111-1111-1111-1111-111111111201', 'CAM_SISE', 'Şişe', true, 1.2, 2),
    ('11111111-1111-1111-1111-111111111301', NULL, 'AHSAP', 'Ahşap', true, 1.8, 3),
    ('11111111-1111-1111-1111-111111111302', '11111111-1111-1111-1111-111111111301', 'AHSAP_PALET', 'Palet', true, 1.8, 1),
    ('11111111-1111-1111-1111-111111111303', '11111111-1111-1111-1111-111111111301', 'AHSAP_TAHTA', 'Tahta Parçası', true, 1.8, 2),
    ('11111111-1111-1111-1111-111111111401', NULL, 'HOBI', 'Hobi Malzemesi', true, 1.5, 4),
    ('11111111-1111-1111-1111-111111111402', '11111111-1111-1111-1111-111111111401', 'HOBI_KUMAS', 'Kumaş', true, 1.5, 1),
    ('11111111-1111-1111-1111-111111111403', '11111111-1111-1111-1111-111111111401', 'HOBI_IPLIK', 'İplik', true, 1.5, 2),
    ('11111111-1111-1111-1111-111111111404', '11111111-1111-1111-1111-111111111401', 'HOBI_BOYA', 'Boya', true, 1.5, 3),
    ('11111111-1111-1111-1111-111111111501', NULL, 'KUCUK_EV_ESYASI', 'Küçük Ev Eşyası', true, 2.0, 5)
ON CONFLICT (code) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    name = EXCLUDED.name,
    reusable = EXCLUDED.reusable,
    coin_multiplier = EXCLUDED.coin_multiplier,
    sort_order = EXCLUDED.sort_order;
