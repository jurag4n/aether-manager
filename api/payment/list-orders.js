// GET /api/payment/list-orders?status=pending
// Header: x-admin-token: <ADMIN_TOKEN>
// Untuk admin panel: list semua order pending yang butuh konfirmasi

import { redis, rget } from '../../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'GET') return res.status(405).end();
  if (req.headers['x-admin-token'] !== process.env.ADMIN_TOKEN)
    return res.status(401).json({ error: 'Unauthorized' });

  // Ambil semua orderId dari index
  const orderIds = await redis('SMEMBERS', 'orders:pending') ?? [];

  // Fetch tiap order
  const orders = (
    await Promise.all(orderIds.map(id => rget(`order:${id}`)))
  )
    .filter(Boolean)
    .filter(o => {
      const statusFilter = req.query.status;
      return statusFilter ? o.status === statusFilter : true;
    })
    .sort((a, b) => b.createdAt - a.createdAt); // terbaru dulu

  return res.status(200).json({ orders });
}
