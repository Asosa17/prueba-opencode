-- Migration 002: Sistema de Roles
-- Ejecutar en Supabase SQL Editor

-- 1. Agregar columna role si no existe
ALTER TABLE users ADD COLUMN IF NOT EXISTS role TEXT NOT NULL DEFAULT 'expert';

-- 2. Actualizar usuarios existentes a expert
UPDATE users SET role = 'expert' WHERE role IN ('user', 'expert');

-- 3. Crear usuario professor con rol legend
-- NOTA: Reemplazar 'AQUI_EL_HASH' por el bcrypt hash de 'pokemon123'
-- Generar con: node -e "const bcrypt=require('bcryptjs');bcrypt.hash('pokemon123',12).then(console.log)"
INSERT INTO users (username, password_hash, pokemon_type_id, role)
SELECT 'professor', 'AQUI_EL_HASH', 1, 'legend'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'professor');

-- 4. Forzar recarga de schema en PostgREST
NOTIFY pgrst, 'reload schema';
