INSERT INTO gamification.badges (id, code, name, description, icon_key, active, rule) VALUES
('2f7b2c28-4b5b-4f8f-8c2c-7e6c7567d001', 'FIRST_GIVE', 'İlk Paylaşım', 'İlk başarılı teslimini tamamla.', 'sprout', true, '{"metric":"CONFIRMED_GIVE_COUNT","operator":"GTE","value":1}'::jsonb),
('2f7b2c28-4b5b-4f8f-8c2c-7e6c7567d002', 'GIVER_10', 'Paylaşım Ustası', '10 başarılı teslim tamamla.', 'package', true, '{"metric":"CONFIRMED_GIVE_COUNT","operator":"GTE","value":10}'::jsonb),
('2f7b2c28-4b5b-4f8f-8c2c-7e6c7567d003', 'FIRST_TAKE', 'Döngüye Katıldın', 'İlk malzeme teslimini al.', 'hand-heart', true, '{"metric":"CONFIRMED_TAKE_COUNT","operator":"GTE","value":1}'::jsonb),
('2f7b2c28-4b5b-4f8f-8c2c-7e6c7567d004', 'CATEGORY_EXPLORER', 'Kategori Kaşifi', '3 farklı kategoride teslim tamamla.', 'compass', true, '{"metric":"DISTINCT_CATEGORY_COUNT","operator":"GTE","value":3}'::jsonb),
('2f7b2c28-4b5b-4f8f-8c2c-7e6c7567d005', 'ECO_100', 'Eco Başlangıç', 'Toplam 100 Eco-Coin kazan.', 'coins', true, '{"metric":"TOTAL_COINS_EARNED","operator":"GTE","value":100}'::jsonb),
('2f7b2c28-4b5b-4f8f-8c2c-7e6c7567d006', 'ECO_500', 'Eco Kahraman', 'Toplam 500 Eco-Coin kazan.', 'trophy', true, '{"metric":"TOTAL_COINS_EARNED","operator":"GTE","value":500}'::jsonb)
ON CONFLICT (code) DO UPDATE SET
name = EXCLUDED.name,
description = EXCLUDED.description,
icon_key = EXCLUDED.icon_key,
active = EXCLUDED.active,
rule = EXCLUDED.rule;
