// GET /api/payment/poll-order?orderId=AETH-xxx&deviceId=yyy
// App polling setiap 3 detik untuk cek apakah kamu sudah konfirmasi
// Returns:
//   { status: 'pending' }                          → belum dikonfirmasi
//   { status: 'completed', licenseKey, expiresAt } → sudah dikonfirmasi, langsung aktif
//   { status: 'rejected' }                         → ditolak admin (opsional)

import { rget } from '../../lib/redis.js';

export default async function handler(req, res) {
  if (req.method !== 'GET') return res.status(405).end();

  const { orderId, deviceId } = req.query;
  if (!orderId || !deviceId)
    return res.status(400).json({ error: 'Missing orderId or deviceId' });

  const order = await rget(`order:${orderId}`);
  if (!order) return res.status(404).json({ error: 'Order not found' });

  // Security: deviceId harus cocok
  if (order.deviceId !== deviceId)
    return res.status(403).json({ error: 'Device mismatch' });

  if (order.status === 'completed') {
    return res.status(200).json({
      status:     'completed',
      licenseKey: order.licenseKey,
      expiresAt:  order.expiresAt,
    });
  }

  return res.status(200).json({ status: order.status ?? 'pending' });
}
