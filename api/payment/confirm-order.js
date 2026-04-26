// POST /api/payment/confirm-order
// Body: { orderId }
// Header: x-admin-token: <ADMIN_TOKEN>
// Dipanggil kamu (admin) setelah cek transfer masuk di GoPay/Dana
// Backend otomatis generate license key dan aktifkan

import { redis, rget, rset, rsadd, rdel } from '../../lib/redis.js';

function generateLicenseKey() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  const seg   = () => Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
  return `AETH-${seg()}-${seg()}-${seg()}`;
}

const MONTH_MS = 30 * 24 * 60 * 60 * 1000;

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).end();
  if (req.headers['x-admin-token'] !== process.env.ADMIN_TOKEN)
    return res.status(401).json({ error: 'Unauthorized' });

  const { orderId } = req.body;
  if (!orderId) return res.status(400).json({ error: 'Missing orderId' });

  const order = await rget(`order:${orderId}`);
  if (!order)                       return res.status(404).json({ error: 'Order not found' });
  if (order.status === 'completed') return res.status(200).json({ message: 'Already confirmed', licenseKey: order.licenseKey });

  const licenseKey = generateLicenseKey();
  const expiresAt  = Date.now() + MONTH_MS;

  // Simpan license ke Redis (terikat deviceId pembeli)
  await rset(`license:${licenseKey}`, {
    key:         licenseKey,
    isActive:    true,
    deviceId:    order.deviceId,
    activatedAt: Date.now(),
    expiresAt,
    note:        `Manual transfer | ${order.name} | ${order.phone}`,
    createdAt:   Date.now(),
  });
  await rsadd('license:index', licenseKey);

  // Update order → completed
  await rset(`order:${orderId}`, {
    ...order,
    status:      'completed',
    licenseKey,
    expiresAt,
    completedAt: Date.now(),
  }, 7 * 24 * 60 * 60); // simpan 7 hari

  // Hapus dari index pending
  await redis('SREM', 'orders:pending', orderId);

  return res.status(200).json({ success: true, licenseKey, expiresAt });
}
