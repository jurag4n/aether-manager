// GET /api/public/key/:key
// Endpoint publik — tidak butuh admin token
// Return: status aktif/tidak + expiry (tanpa deviceId/note)

import { rget } from '../../../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'GET') return res.status(405).end();

  const { key } = req.query;
  if (!key) return res.status(400).json({ error: 'Missing key' });

  const record = await rget(`license:${key.trim().toUpperCase()}`);
  if (!record) return res.status(404).json({ error: 'Not found' });

  // Hanya expose field yang aman untuk publik
  return res.status(200).json({
    isActive:  record.isActive ?? false,
    expiresAt: record.expiresAt ?? null,
    activated: !!record.deviceId,   // sudah diaktifkan di device atau belum
  });
}
