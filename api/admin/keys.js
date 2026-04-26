import { rsmembers, rget } from '../../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'GET') return res.status(405).end();
  if (req.headers['x-admin-token'] !== process.env.ADMIN_TOKEN)
    return res.status(401).json({ error: 'Unauthorized' });

  const keyIds = await rsmembers('license:index') ?? [];
  const keys = (await Promise.all(keyIds.map(k => rget(`license:${k}`)))).filter(Boolean);
  return res.status(200).json({ keys });
}
