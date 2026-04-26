import { rset, rsadd } from '../../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).end();
  if (req.headers['x-admin-token'] !== process.env.ADMIN_TOKEN)
    return res.status(401).json({ error: 'Unauthorized' });

  const { key, expiresAt, note } = req.body;
  if (!key) return res.status(400).json({ error: 'Missing key' });

  const record = {
    key: key.trim().toUpperCase(),
    isActive: true,
    deviceId: null,
    activatedAt: null,
    expiresAt: expiresAt ?? null,
    note: note || '',
    createdAt: Date.now(),
  };

  await rset(`license:${record.key}`, record);
  await rsadd('license:index', record.key);
  return res.status(200).json({ success: true, key: record.key });
}
