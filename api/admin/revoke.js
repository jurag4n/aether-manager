import { rget, rset } from '../../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).end();
  if (req.headers['x-admin-token'] !== process.env.ADMIN_TOKEN)
    return res.status(401).json({ error: 'Unauthorized' });

  const { key } = req.body;
  if (!key) return res.status(400).json({ error: 'Missing key' });

  const record = await rget(`license:${key.trim().toUpperCase()}`);
  if (!record) return res.status(404).json({ error: 'Key not found' });

  record.isActive = false;
  await rset(`license:${key.trim().toUpperCase()}`, record);
  return res.status(200).json({ success: true });
}
