-- A field report stored before V3, when every report hung from a zone.
INSERT INTO emergency_management.emergencies (id, name, type, department, province, district, start_date)
VALUES ('6f1c2a3b-4d5e-4f60-8a7b-9c0d1e2f3a01', 'Huaico Chosica 2026', 'LANDSLIDE', 'Lima', 'Lima',
        'Lurigancho-Chosica', DATE '2026-03-14');

INSERT INTO emergency_management.zones (id, emergency_id, name, damage_level)
VALUES ('7a2d3b4c-5e6f-4071-9b8c-0d1e2f3a4b02', '6f1c2a3b-4d5e-4f60-8a7b-9c0d1e2f3a01',
        'Sector Alto Peru', 'SEVERE');

INSERT INTO emergency_management.field_reports (id, zone_id, raw_text, source)
VALUES ('8b3e4c5d-6f70-4182-ac9d-1e2f3a4b5c03', '7a2d3b4c-5e6f-4071-9b8c-0d1e2f3a4b02',
        'Derrumbe de viviendas en el sector Alto Peru.', 'AUTHORITY');
