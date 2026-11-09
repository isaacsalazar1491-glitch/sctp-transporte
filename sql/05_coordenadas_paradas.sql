-- Migración para bases ya creadas: agrega ubicación (Google Maps) a las paradas.
-- Uso: psql -U postgres -d sctp -f sql/05_coordenadas_paradas.sql   (idempotente)
ALTER TABLE parada ADD COLUMN IF NOT EXISTS latitud  NUMERIC(9,6) NULL CHECK (latitud  BETWEEN -90  AND 90);
ALTER TABLE parada ADD COLUMN IF NOT EXISTS longitud NUMERIC(9,6) NULL CHECK (longitud BETWEEN -180 AND 180);

-- Coordenadas aproximadas de las paradas de prueba (ajústalas desde la pantalla Rutas)
UPDATE parada SET latitud = 19.1789, longitud = -99.4663 WHERE nombre = 'Terminal Tianguistenco' AND latitud IS NULL;
UPDATE parada SET latitud = 19.2257, longitud = -99.4846 WHERE nombre = 'Santiago Tilapa'        AND latitud IS NULL;
UPDATE parada SET latitud = 19.2847, longitud = -99.5122 WHERE nombre = 'Lerma'                  AND latitud IS NULL;
UPDATE parada SET latitud = 19.2900, longitud = -99.6560 WHERE nombre = 'Terminal Toluca'        AND latitud IS NULL;
UPDATE parada SET latitud = 19.1830, longitud = -99.4700 WHERE nombre = 'Mercado'                AND latitud IS NULL;
