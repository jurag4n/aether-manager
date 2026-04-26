import { rget, rset } from '../../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'POST' && req.method !== 'PATCH')
    return res.status(405).end();
  if (req.headers['x-admin-token'] !== process.env.ADMIN_TOKEN)
    return res.status(401).json({ error: 'Unauthorized' });

  const { key, note, expiresAt } = req.body;
  if (!key) return res.status(400).json({ error: 'Missing key' });

  const k = key.trim().toUpperCase();
  const record = await rget(`license:${k}`);
  if (!record) return res.status(404).json({ error: 'Key not found' });

  if (note !== undefined) record.note = note;
  if ('expiresAt' in req.body) record.expiresAt = expiresAt ?? null;

  await rset(`license:${k}`, record);
  return res.status(200).json({ success: true, record });
}
