import { readFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default function handler(req, res) {
  try {
    // Naik 1 level dari /api ke root, lalu masuk license-admin/
    const htmlPath = join(__dirname, '..', 'license-admin', 'index.html');
    const html = readFileSync(htmlPath, 'utf8');
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store');
    res.status(200).send(html);
  } catch (e) {
    res.status(500).send('Admin panel not found: ' + e.message);
  }
}
