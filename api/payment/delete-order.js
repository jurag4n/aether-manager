// POST /api/payment/delete-order
// Body: { orderId }
// Header: x-admin-token: <ADMIN_TOKEN>

import { redis, rget, rdel } from '../../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).end();
  if (req.headers['x-admin-token'] !== process.env.ADMIN_TOKEN)
    return res.status(401).json({ error: 'Unauthorized' });

  const { orderId } = req.body;
  if (!orderId) return res.status(400).json({ error: 'Missing orderId' });

  const order = await rget(`order:${orderId}`);
  if (!order) return res.status(404).json({ error: 'Order not found' });

  // Hapus dari Redis
  await rdel(`order:${orderId}`);
  // Hapus dari index pending jika masih ada
  await redis('SREM', 'orders:pending', orderId);

  return res.status(200).json({ success: true });
}
