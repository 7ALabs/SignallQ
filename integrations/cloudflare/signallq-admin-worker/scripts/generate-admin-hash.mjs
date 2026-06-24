#!/usr/bin/env node
// Gera hash PBKDF2 para bootstrap do primeiro usuário admin no D1.
// Usage: node generate-admin-hash.mjs <email> <senha> <PEPPER>
// Saída: SQL pronto para executar via wrangler d1 execute --remote

import { pbkdf2Sync, randomBytes } from 'crypto'

const [,, email, password, pepper] = process.argv
if (!email || !password || !pepper) {
  console.error('Usage: node generate-admin-hash.mjs <email> <senha> <PEPPER>')
  process.exit(1)
}

const salt = randomBytes(16)
const hash = pbkdf2Sync(pepper + password, salt, 150000, 32, 'sha256')
const b64 = buf => buf.toString('base64')
const hashStr = `pbkdf2$150000$${b64(salt)}$${b64(hash)}`
const id = randomBytes(16).toString('hex')
const now = Math.floor(Date.now() / 1000)

const sql = `INSERT INTO admin_users (id, email, password_hash, role, active, created_at) VALUES ('${id}', '${email}', '${hashStr}', 'admin', 1, ${now});`

console.log('\nExecute no D1 (produção):')
console.log(`npx wrangler d1 execute signallq-admin-db --remote --command="${sql}"`)
console.log('\nExecute no D1 (local):')
console.log(`npx wrangler d1 execute signallq-admin-db --local --command="${sql}"`)
