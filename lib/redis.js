// lib/redis.js — Upstash REST client
export async function redis(cmd, ...args) {
  const url   = process.env.UPSTASH_REDIS_REST_URL;
  const token = process.env.UPSTASH_REDIS_REST_TOKEN;
  const res = await fetch(`${url}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify([cmd, ...args]),
  });
  const data = await res.json();
  return data.result;
}

export async function rget(key) {
  const val = await redis('GET', key);
  return val ? JSON.parse(val) : null;
}

/** Set key. Jika ttlSeconds diberikan, key akan expire otomatis */
export async function rset(key, val, ttlSeconds) {
  if (ttlSeconds) {
    return redis('SET', key, JSON.stringify(val), 'EX', ttlSeconds);
  }
  return redis('SET', key, JSON.stringify(val));
}

export async function rsadd(key, ...members) {
  return redis('SADD', key, ...members);
}

export async function rsmembers(key) {
  return redis('SMEMBERS', key);
}

export async function rdel(key) {
  return redis('DEL', key);
}

export async function rsrem(key, ...members) {
  return redis('SREM', key, ...members);
}
