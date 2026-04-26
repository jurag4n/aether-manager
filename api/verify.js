import { rget, rset } from '../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).end();
  const { key, deviceId } = req.body;
  if (!key || !deviceId) return res.status(200).json({ valid: false });

  const record = await rget(`license:${key.trim().toUpperCase()}`);
  if (!record || !record.isActive) return res.status(200).json({ valid: false });

  const now = Date.now();
  if (record.expiresAt !== null && record.expiresAt < now)
    return res.status(200).json({ valid: false, expiresAt: record.expiresAt });

  if (record.deviceId !== deviceId) return res.status(200).json({ valid: false });

  return res.status(200).json({ valid: true, expiresAt: record.expiresAt });
}
