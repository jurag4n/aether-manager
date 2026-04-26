import { rget } from '../../../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'GET') return res.status(405).end();
  if (req.headers['x-admin-token'] !== process.env.ADMIN_TOKEN)
    return res.status(401).json({ error: 'Unauthorized' });

  const { key } = req.query;
  const record = await rget(`license:${key.trim().toUpperCase()}`);
  if (!record) return res.status(404).json({ error: 'Not found' });
  return res.status(200).json(record);
}
