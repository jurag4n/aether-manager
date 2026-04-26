import { rget, rset } from '../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).end();
  const { key, deviceId } = req.body;
  if (!key || !deviceId) return res.status(400).json({ error: 'Missing key or deviceId' });

  const normalized = key.trim().toUpperCase();
  const record = await rget(`license:${normalized}`);

  if (!record) return res.status(404).json({ error: 'Invalid license key' });
  if (!record.isActive) return res.status(403).json({ error: 'License has been revoked' });

  const now = Date.now();
  if (record.expiresAt !== null && record.expiresAt < now)
    return res.status(403).json({ error: 'License has expired' });

  if (record.deviceId && record.deviceId !== deviceId)
    return res.status(403).json({ error: 'Key already activated on another device' });

  if (!record.deviceId) {
    record.deviceId = deviceId;
    record.activatedAt = now;
    await rset(`license:${normalized}`, record);
  }

  return res.status(200).json({ valid: true, expiresAt: record.expiresAt });
}
