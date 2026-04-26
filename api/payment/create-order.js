// POST /api/payment/create-order
// Body: { name, phone, deviceId }
// Returns: { orderId, gopay, dana, nominal }
// Tidak butuh payment gateway – user transfer manual ke nomor kamu

import { rset, rsadd } from '../../lib/redis.js';

const GOPAY_NUMBER = process.env.GOPAY_NUMBER;   // ex: "081234567890"
const DANA_NUMBER  = process.env.DANA_NUMBER;    // ex: "081234567890"
const PRICE        = 25000;

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).end();

  const { name, phone, deviceId } = req.body;
  if (!name || !phone || !deviceId)
    return res.status(400).json({ error: 'Missing name, phone, or deviceId' });

  // Cek apakah device sudah punya pending order (hindari spam)
  // orderId unik per transaksi
  const orderId = `AETH-${Date.now()}-${Math.random().toString(36).slice(2,6).toUpperCase()}`;

  const record = {
    orderId,
    name: name.trim(),
    phone: phone.trim(),
    deviceId,
    status: 'pending',   // pending → confirmed → completed
    createdAt: Date.now(),
  };

  // Simpan pending order ke Redis, expire 48 jam
  await rset(`order:${orderId}`, record, 48 * 60 * 60);
  // Index untuk admin bisa list semua pending
  await rsadd('orders:pending', orderId);

  return res.status(200).json({
    orderId,
    gopay:   GOPAY_NUMBER,
    dana:    DANA_NUMBER,
    nominal: PRICE,
  });
}
