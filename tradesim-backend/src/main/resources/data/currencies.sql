INSERT INTO fx_fee_schedules (id, source_category, target_category, fee_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'MAJOR', 'MAJOR', 0.0010, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO fx_fee_schedules (id, source_category, target_category, fee_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'MAJOR', 'MINOR', 0.0025, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO fx_fee_schedules (id, source_category, target_category, fee_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'MAJOR', 'EXOTIC', 0.0050, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO fx_fee_schedules (id, source_category, target_category, fee_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'MINOR', 'MAJOR', 0.0025, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO fx_fee_schedules (id, source_category, target_category, fee_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'MINOR', 'MINOR', 0.0050, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO fx_fee_schedules (id, source_category, target_category, fee_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'MINOR', 'EXOTIC', 0.0075, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO fx_fee_schedules (id, source_category, target_category, fee_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'EXOTIC', 'MAJOR', 0.0050, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO fx_fee_schedules (id, source_category, target_category, fee_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'EXOTIC', 'MINOR', 0.0075, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO fx_fee_schedules (id, source_category, target_category, fee_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'EXOTIC', 'EXOTIC', 0.0100, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('USD', 'MAJOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;
INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('EUR', 'MAJOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;
INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('GBP', 'MAJOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;
INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('JPY', 'MAJOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;
INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('CHF', 'MAJOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;
INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('AUD', 'MAJOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;
INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('CAD', 'MAJOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;

INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('SGD', 'MINOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;
INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('NZD', 'MINOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;
INSERT INTO supported_currencies (code, category, is_active, created_at, updated_at) VALUES ('HKD', 'MINOR', true, NOW(), NOW()) ON CONFLICT DO NOTHING;