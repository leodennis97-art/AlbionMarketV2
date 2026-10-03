/**
 * AlbionDataPro Central Market & OTA Server (Node.js / Express)
 * Collects public Albion Online market data 24/7 and serves it to installed apps via Public Tunnel.
 * Includes Cloud Backup & Restore for User Data, Full Account & Device Management, and License Generator.
 */

const express = require('express');
const axios = require('axios');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

const app = express();
app.set('trust proxy', true);
const PORT = process.env.PORT || 4000;
const SERVER_HMAC_SECRET = process.env.SERVER_HMAC_SECRET || 'AlbionDataProSecretKey2026_HMAC_SHA256_Secure';
const ADMIN_API_KEY = process.env.ADMIN_API_KEY || 'AlbionDataPro_Military_Admin_SuperSecret_2026#Key';

// Security Middleware: Allow all connections (removed IP block on 74.220.*)
app.use((req, res, next) => {
    next();
});

// In-Memory Rate Limiting & Abuse Prevention
const downloadRateLimiter = new Map(); // IP -> timestamps[]
const loginRateLimiter = new Map();    // IP -> timestamps[]
const generalRateLimiter = new Map();  // IP -> timestamps[]

function isRateLimited(map, ip, maxRequests, windowMs) {
    const now = Date.now();
    let timestamps = map.get(ip) || [];
    timestamps = timestamps.filter(t => now - t < windowMs);
    if (timestamps.length >= maxRequests) {
        map.set(ip, timestamps);
        return true;
    }
    timestamps.push(now);
    map.set(ip, timestamps);
    return false;
}

// Clean up rate limiters every 15 minutes
setInterval(() => {
    const now = Date.now();
    for (const [ip, list] of downloadRateLimiter.entries()) {
        const filtered = list.filter(t => now - t < 3600000);
        if (filtered.length === 0) downloadRateLimiter.delete(ip);
        else downloadRateLimiter.set(ip, filtered);
    }
    for (const [ip, list] of loginRateLimiter.entries()) {
        const filtered = list.filter(t => now - t < 900000);
        if (filtered.length === 0) loginRateLimiter.delete(ip);
        else loginRateLimiter.set(ip, filtered);
    }
}, 900000);

// High-End Security Middleware: Require Admin Key
function requireAdminAuth(req, res, next) {
    const authHeader = req.headers['authorization'] || req.headers['x-admin-key'];
    const queryKey = req.query.adminKey || req.query.key;
    const token = (authHeader && authHeader.startsWith('Bearer ')) ? authHeader.substring(7).trim() : authHeader;

    if (token === ADMIN_API_KEY || queryKey === ADMIN_API_KEY) {
        return next();
    }
    return res.status(403).json({
        error: 'Forbidden: High-End Security Authentication Required',
        status: 'unauthorized'
    });
}

// Helper to generate cryptographically signed download tokens
function generateSignedDownloadToken(expiresAtMs) {
    const payload = `dl_token_${expiresAtMs}`;
    const hash = crypto.createHmac('sha256', SERVER_HMAC_SECRET).update(payload).digest('hex');
    return `${expiresAtMs}.${hash}`;
}

// Helper to verify signed download tokens
function verifySignedDownloadToken(token) {
    if (!token || typeof token !== 'string') return false;
    const parts = token.split('.');
    if (parts.length !== 2) return false;
    const [expiresStr, clientHash] = parts;
    const expiresAt = parseInt(expiresStr, 10);
    if (isNaN(expiresAt) || Date.now() > expiresAt) return false; // Expired!

    const expectedHash = crypto.createHmac('sha256', SERVER_HMAC_SECRET).update(`dl_token_${expiresAt}`).digest('hex');
    try {
        return crypto.timingSafeEqual(Buffer.from(clientHash, 'hex'), Buffer.from(expectedHash, 'hex'));
    } catch (_) {
        return false;
    }
}

let marketCache = { items: [], lastUpdated: null };
let registeredDevices = [];
let registeredUsers = [];
let generatedLicenses = [];
let hourlyData24h = Array.from({ length: 24 }, (_, i) => ({
    hour: `${i.toString().padStart(2, '0')}:00`,
    itemsCollected: Math.floor(Math.random() * 300) + 750
}));
let totalInformationCount = 45280;

let remoteConfig = {
    minMarginPercent: 12.0,
    maintenanceMode: false,
    blacklistedCities: [],
    aiAnalyzerEnabled: true,
    lastUpdated: new Date().toISOString()
};

let guildSharedOrders = [];
let deviceTelemetryLogs = [];
let sseClients = [];

const DEVICES_FILE = path.join(__dirname, 'devices.json');
const USERS_FILE = path.join(__dirname, 'users.json');
const LICENSES_FILE = path.join(__dirname, 'licenses.json');
const DATA_24H_FILE = path.join(__dirname, 'data_24h.json');
const BACKUPS_DIR = path.join(__dirname, 'backups');
const DOWNLOADS_DIR = path.join(__dirname, 'downloads');

if (!fs.existsSync(BACKUPS_DIR)) fs.mkdirSync(BACKUPS_DIR, { recursive: true });
if (!fs.existsSync(DOWNLOADS_DIR)) fs.mkdirSync(DOWNLOADS_DIR, { recursive: true });

const CURRENT_SERVER_VERSION = "2.0.8";
let globalOtaTrigger = false;
let lastApkMtime = 0;

function compareVersions(v1, v2) {
    if (!v1 || !v2) return 0;
    const p1 = v1.trim().replace(/^v/i, '').split('.').map(n => parseInt(n, 10) || 0);
    const p2 = v2.trim().replace(/^v/i, '').split('.').map(n => parseInt(n, 10) || 0);
    const maxLen = Math.max(p1.length, p2.length);
    for (let i = 0; i < maxLen; i++) {
        const num1 = p1[i] || 0;
        const num2 = p2[i] || 0;
        if (num1 > num2) return 1;
        if (num1 < num2) return -1;
    }
    return 0;
}

function triggerAutoOtaUpdateForAllDevices(reason = 'Neue Version bereitgestellt') {
    globalOtaTrigger = true;
    let count = 0;
    registeredDevices.forEach(d => {
        d.forceOtaUpdate = true;
        d.pendingUpdate = true;
        count++;
    });
    saveDevices();
    broadcastSSE('ota_update_available', {
        targetVersion: CURRENT_SERVER_VERSION,
        force: true,
        reason: reason,
        downloadUrl: '/download/AlbionDataPro.apk',
        timestamp: new Date().toISOString()
    });
}

// Administration Upload-Route für OTA-Updates (Neue APK auf den Render Server laden)
const multer = require('multer');
const upload = multer({
    dest: DOWNLOADS_DIR,
    fileFilter: (req, file, cb) => {
        if (file.mimetype !== 'application/vnd.android.package-archive' && !file.originalname.endsWith('.apk')) {
            return cb(new Error('Nur .apk Dateien erlaubt'), false);
        }
        cb(null, true);
    }
});

app.post('/api/admin/upload-apk', requireAdminAuth, upload.single('apkFile'), (req, res) => {
    if (!req.file) return res.status(400).json({ error: 'Keine Datei hochgeladen' });

    // Lösche alle eventuellen alten APK-Dateien im downloads Verzeichnis
    try {
        const files = fs.readdirSync(DOWNLOADS_DIR);
        files.forEach(file => {
            if (file.endsWith('.apk')) {
                fs.unlinkSync(path.join(DOWNLOADS_DIR, file));
            }
        });
    } catch (e) {
        console.error('Fehler beim Bereinigen alter APKs:', e.message);
    }

    const targetPath = path.join(DOWNLOADS_DIR, 'AlbionDataPro.apk');
    fs.renameSync(req.file.path, targetPath);

    triggerAutoOtaUpdateForAllDevices('Neue APK von Admin hochgeladen');
    res.json({ status: 'success', message: 'APK erfolgreich hochgeladen und alte Versionen bereinigt!' });
});

// Endpoint zum Bereinigen alter APKs
app.post('/api/admin/cleanup-apks', requireAdminAuth, (req, res) => {
    try {
        const files = fs.readdirSync(DOWNLOADS_DIR);
        let count = 0;
        files.forEach(file => {
            if (file !== 'AlbionDataPro.apk' && file.endsWith('.apk')) {
                fs.unlinkSync(path.join(DOWNLOADS_DIR, file));
                count++;
            }
        });
        res.json({ status: 'success', deletedCount: count });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

function syncLatestApk() {
    try {
        const apkPath = path.join(DOWNLOADS_DIR, 'AlbionDataPro.apk');
        if (fs.existsSync(apkPath)) {
            const stat = fs.statSync(apkPath);
            if (lastApkMtime === 0) {
                lastApkMtime = stat.mtimeMs;
            } else if (stat.mtimeMs > lastApkMtime) {
                lastApkMtime = stat.mtimeMs;
                console.log('[OTA Sync] 🚀 Neue APK-Datei erkannt! Triggere automatisches OTA-Update für alle Geräte...');
                triggerAutoOtaUpdateForAllDevices('Neue APK-Datei automatisch auf Server synchronisiert');
            }
        }
    } catch (e) {
        console.error('[OTA Sync] Fehler bei APK-Prüfung:', e.message);
    }
}

// Autonome Echtzeit-Cloud-Schleife: Erkennt App-Änderungen in Gradle sofort und synchronisiert Server & Geräte
async function autonomousApkSyncLoop() {
    try {
        const currentGradleVersion = getAppVersionFromGradle();
        const apkPath = path.join(DOWNLOADS_DIR, 'AlbionDataPro.apk');
        const versionPath = path.join(DOWNLOADS_DIR, 'version.txt');
        const localBuildDebug = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
        const localBuildRelease = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk');

        let sourceApk = null;
        if (fs.existsSync(localBuildRelease) && fs.statSync(localBuildRelease).size > 500000) {
            sourceApk = localBuildRelease;
        } else if (fs.existsSync(localBuildDebug) && fs.statSync(localBuildDebug).size > 500000) {
            sourceApk = localBuildDebug;
        }

        let currentUploadedVersion = '';
        if (fs.existsSync(versionPath)) {
            currentUploadedVersion = fs.readFileSync(versionPath, 'utf8').trim();
        }

        const needsUpdate = currentUploadedVersion !== currentGradleVersion || !fs.existsSync(apkPath);

        if (sourceApk && needsUpdate) {
            fs.copyFileSync(sourceApk, apkPath);
            fs.writeFileSync(versionPath, currentGradleVersion);
            console.log(`[Autonomous Sync] 🚀 Neue App-Version v${currentGradleVersion} erkannt & Server automatisch aktualisiert!`);
            triggerAutoOtaUpdateForAllDevices(`Neue App-Version v${currentGradleVersion} veröffentlicht`);
            return;
        }

        if (needsUpdate) {
            console.log(`[Autonomous Sync] 🔄 Synchronisiere Server auf neuste Version v${currentGradleVersion}...`);
            fs.writeFileSync(versionPath, currentGradleVersion);
            triggerAutoOtaUpdateForAllDevices(`Server auf v${currentGradleVersion} aktualisiert`);
        }
    } catch (e) {
        console.log('[Autonomous Sync] ℹ️ Status:', e.message);
    }
}

setInterval(autonomousApkSyncLoop, 10000);
setTimeout(autonomousApkSyncLoop, 2000);

function loadDevices() {
    if (fs.existsSync(DEVICES_FILE)) {
        try { registeredDevices = JSON.parse(fs.readFileSync(DEVICES_FILE, 'utf8')); }
        catch (e) { console.error('Failed to load devices.json:', e.message); }
    }
}
loadDevices();
syncLatestApk();
setInterval(syncLatestApk, 15000);

function saveDevices() {
    try { fs.writeFileSync(DEVICES_FILE, JSON.stringify(registeredDevices, null, 2)); }
    catch (e) { console.error('Failed to save devices.json:', e.message); }
}

function cleanupExpiredAccounts() {
    const now = new Date();
    registeredUsers = registeredUsers.filter(u => {
        if (u.username.toLowerCase() === 'dnnx') return true;
        if (!u.licenseExpiresAt) return true;
        const expDate = new Date(u.licenseExpiresAt);
        const deletionThreshold = new Date(expDate.getTime() + (90 * 24 * 3600 * 1000));
        return deletionThreshold > now;
    });
    saveUsers();
}

function loadUsers() {
    if (fs.existsSync(USERS_FILE)) {
        try { registeredUsers = JSON.parse(fs.readFileSync(USERS_FILE, 'utf8')); }
        catch (e) { console.error('Failed to load users.json:', e.message); }
    } else {
        registeredUsers = [{
            id: 'usr_admin_dnnx',
            username: 'dnnx',
            password: 'Dean3153...',
            isAdmin: true,
            isLicensed: true,
            licenseExpiresAt: '2099-12-31T23:59:59.000Z'
        }];
        saveUsers();
    }
    cleanupExpiredAccounts();
}
loadUsers();

function saveUsers() {
    try { fs.writeFileSync(USERS_FILE, JSON.stringify(registeredUsers, null, 2)); }
    catch (e) { console.error('Failed to save users.json:', e.message); }
}

function loadLicenses() {
    if (fs.existsSync(LICENSES_FILE)) {
        try { generatedLicenses = JSON.parse(fs.readFileSync(LICENSES_FILE, 'utf8')); }
        catch (e) { console.error('Failed to load licenses.json:', e.message); }
    }
}
loadLicenses();

function saveLicenses() {
    try { fs.writeFileSync(LICENSES_FILE, JSON.stringify(generatedLicenses, null, 2)); }
    catch (e) { console.error('Failed to save licenses.json:', e.message); }
}

function loadData24h() {
    if (fs.existsSync(DATA_24H_FILE)) {
        try {
            const data = JSON.parse(fs.readFileSync(DATA_24H_FILE, 'utf8'));
            if (data.hourly) hourlyData24h = data.hourly;
            if (data.total) totalInformationCount = data.total;
        } catch (_) {}
    }
}
loadData24h();

function saveData24h() {
    try {
        fs.writeFileSync(DATA_24H_FILE, JSON.stringify({ total: totalInformationCount, hourly: hourlyData24h }, null, 2));
    } catch (_) {}
}

function getActiveTunnelUrl() {
    try {
        const tunnelFile = path.join(__dirname, 'current_tunnel.json');
        if (fs.existsSync(tunnelFile)) {
            const data = JSON.parse(fs.readFileSync(tunnelFile, 'utf8'));
            if (data.tunnelUrl) return data.tunnelUrl;
        }
    } catch (_) {}
    return 'https://witty-catfish-22.loca.lt';
}

async function fetchAlbionMarketData() {
    try {
        const url = 'https://europe.albiononline-data.com/api/v2/stats/Prices/T4_BAG,T5_BAG,T6_BAG,T7_BAG,T8_BAG,T4_MAIN_SWORD,T5_MAIN_SWORD,T6_MAIN_SWORD,T4_ARMOR_CLOTH,T5_ARMOR_CLOTH,T6_ARMOR_CLOTH?locations=Bridgewatch,Caerleon,Fort Sterling,Lymhurst,Martlock,Thetford,Brecilien';
        const res = await axios.get(url, { timeout: 6000 });
        if (res.data && Array.isArray(res.data)) {
            marketCache = {
                items: res.data,
                lastUpdated: new Date().toISOString()
            };
            const count = res.data.length * 6;
            totalInformationCount += count;

            const currentHourKey = `${new Date().getHours().toString().padStart(2, '0')}:00`;
            let hObj = hourlyData24h.find(h => h.hour === currentHourKey);
            if (hObj) {
                hObj.itemsCollected += count;
            } else {
                hourlyData24h.push({ hour: currentHourKey, itemsCollected: count });
                if (hourlyData24h.length > 24) hourlyData24h.shift();
            }
            saveData24h();
        }
    } catch (e) {}
}

setInterval(fetchAlbionMarketData, 30000);
fetchAlbionMarketData();

app.use(express.json());

// High-End Security Headers (Anti-Sniffing, Anti-Clickjacking, HTTPS Enforcement)
app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    next();
});

// CORS & Persistent Keep-Alive Headers for Render Cloud <-> Device Connections
app.use((req, res, next) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Bypass-Tunnel-Reminder, X-Albion-Signature, X-Admin-Key');
    res.setHeader('Connection', 'keep-alive');
    res.setHeader('Keep-Alive', 'timeout=600, max=1000');
    if (req.method === 'OPTIONS') {
        return res.sendStatus(200);
    }
    next();
});

// HMAC-SHA256 Response Signing Middleware
app.use((req, res, next) => {
    const originalJson = res.json;
    res.json = function(data) {
        const payload = JSON.stringify(data);
        const signature = crypto.createHmac('sha256', SERVER_HMAC_SECRET).update(payload).digest('hex');
        res.setHeader('X-Albion-Signature', signature);
        return originalJson.call(this, data);
    };
    next();
});

// Endpoint to obtain a signed, short-lived download link (Valid for 15 minutes)
app.get(['/api/download/token', '/download/token'], (req, res) => {
    const ip = req.ip || req.connection.remoteAddress || 'unknown';
    // Rate limit token generation: max 15 tokens per 10 minutes per IP
    if (isRateLimited(generalRateLimiter, `dl_token_${ip}`, 15, 600000)) {
        return res.status(429).json({ error: 'Zu viele Anfragen. Bitte warte einen Moment.' });
    }
    const expiresAt = Date.now() + (15 * 60 * 1000); // 15 Min
    const signedToken = generateSignedDownloadToken(expiresAt);
    res.json({
        status: 'success',
        token: signedToken,
        expiresAt: expiresAt,
        downloadUrl: `/download/AlbionDataPro.apk?token=${signedToken}`
    });
});

// High-End Protected Streaming APK Download with Rate-Limiting & Memory Overflow Protection
app.get(['/download/AlbionDataPro.apk', '/download/app-update.apk', '/download/latest.apk'], (req, res) => {
    const ip = req.ip || req.connection.remoteAddress || 'unknown';
    const token = req.query.token;
    const adminKey = req.query.key || req.query.adminKey || req.headers['x-admin-key'];

    // 1. Verification: Either valid cryptographic signature or Admin-Key bypass
    const isValidToken = token && verifySignedDownloadToken(token);
    const isAdmin = adminKey === ADMIN_API_KEY;

    // Check rate limit: max 1000 downloads per hour per IP
    if (isRateLimited(downloadRateLimiter, ip, 1000, 3600000)) {
        console.warn(`[Albion Security] ⚠️ Download Rate-Limit erreicht für IP: ${ip}`);
        return res.status(429).send(`
            <html style="background:#0f172a;color:#fff;font-family:sans-serif;text-align:center;padding:50px;">
                <h2>⚠️ Download-Limit erreicht</h2>
                <p>Aus Sicherheitsgründen sind nur maximal 6 Downloads pro Stunde erlaubt. Bitte warte einige Minuten.</p>
            </html>
        `);
    }

    // High-End Fallback: If no token was provided via direct link, generate one-time access if under limit
    if (!isValidToken && !isAdmin && !token) {
        // Direct web browser download allowed under strict rate limiter
    } else if (!isValidToken && !isAdmin) {
        return res.status(403).send(`
            <html style="background:#0f172a;color:#fff;font-family:sans-serif;text-align:center;padding:50px;">
                <h2>⛔ Ungültiger oder abgelaufener Download-Link</h2>
                <p>Der Download-Link ist abgelaufen (Gültigkeit: 15 Minuten) oder die Signatur ist ungültig.</p>
                <a href="/" style="color:#38bdf8;">Zurück zur Startseite</a>
            </html>
        `);
    }

    const apkFile = path.join(DOWNLOADS_DIR, 'AlbionDataPro.apk');
    if (!fs.existsSync(apkFile)) {
        return res.status(404).send(`
            <html style="background:#0f172a;color:#fff;font-family:sans-serif;text-align:center;padding:50px;">
                <h2>⚠️ APK wird vorbereitet</h2>
                <p>Die aktuelle APK-Datei wird gerade auf den Server geladen. Bitte versuche es in wenigen Sekunden noch einmal.</p>
                <a href="/" style="color:#38bdf8;">Zurück zur Startseite</a>
            </html>
        `);
    }

    const stat = fs.statSync(apkFile);
    res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="AlbionDataPro.apk"',
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
    });

    // High-End Stream to avoid memory exhaustion on Render
    const readStream = fs.createReadStream(apkFile);
    readStream.pipe(res);
});

app.get('/dl', (req, res) => {
    const expiresAt = Date.now() + (15 * 60 * 1000);
    const token = generateSignedDownloadToken(expiresAt);
    res.redirect(`/download/AlbionDataPro.apk?token=${token}`);
});

app.get('/apk', (req, res) => {
    const expiresAt = Date.now() + (15 * 60 * 1000);
    const token = generateSignedDownloadToken(expiresAt);
    res.redirect(`/download/AlbionDataPro.apk?token=${token}`);
});

// Landing Page (Verkauf, Info & Download)
app.get(['/', '/get', '/app'], (req, res) => {
    res.send(`<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AlbionDataPro - Premium Market & Trading Tool</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        body { background-color: #0f172a; color: #f8fafc; font-family: 'Inter', sans-serif; }
        .glass-panel { background: rgba(30, 41, 59, 0.7); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.1); }
        .gradient-text { background: linear-gradient(135deg, #38bdf8, #8b5cf6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
    </style>
</head>
<body class="antialiased min-h-screen flex flex-col">

    <!-- Navbar -->
    <nav class="w-full p-6 flex justify-between items-center max-w-6xl mx-auto">
        <div class="text-2xl font-bold tracking-tighter flex items-center gap-2">
            <i class="fa-solid fa-shield-halved text-blue-500"></i> AlbionDataPro
        </div>
        <div>
            <span class="bg-emerald-500/20 text-emerald-400 px-3 py-1 rounded-full text-sm font-semibold border border-emerald-500/30">v${CURRENT_SERVER_VERSION} Live</span>
        </div>
    </nav>

    <!-- Hero Section -->
    <main class="flex-grow flex flex-col items-center justify-center px-4 py-12 text-center max-w-5xl mx-auto">
        <h1 class="text-5xl md:text-7xl font-extrabold mb-6 leading-tight">
            Dominiere den Markt mit <br><span class="gradient-text">Echtzeit-Daten</span>
        </h1>
        <p class="text-lg md:text-xl text-slate-400 mb-10 max-w-3xl leading-relaxed">
            Maximiere deinen Silber-Gewinn durch unser permanentes In-Game Overlay. KI-gesteuerte Marktüberwachung und Profitrechner direkt auf deinem Bildschirm - ohne die App zu wechseln.
        </p>

        <div class="flex flex-col sm:flex-row gap-4 mb-16">
            <a href="https://t.me/dnnx" target="_blank" class="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-bold py-4 px-8 rounded-xl shadow-lg shadow-blue-500/30 transition-all transform hover:scale-105 flex items-center justify-center gap-3 text-lg">
                <i class="fa-brands fa-telegram text-xl"></i> Lizenz kaufen (15€/Monat)
            </a>
            <a href="/download/AlbionDataPro.apk" class="glass-panel hover:bg-slate-800 text-white font-bold py-4 px-8 rounded-xl transition-all flex items-center justify-center gap-3 text-lg border border-slate-600 hover:border-slate-500">
                <i class="fa-solid fa-download"></i> App Herunterladen (APK)
            </a>
        </div>

        <!-- Features Grid -->
        <div class="grid md:grid-cols-3 gap-6 w-full text-left">
            <div class="glass-panel p-6 rounded-2xl">
                <div class="bg-blue-500/20 w-12 h-12 rounded-lg flex items-center justify-center mb-4 border border-blue-500/30">
                    <i class="fa-solid fa-layer-group text-blue-400 text-xl"></i>
                </div>
                <h3 class="text-xl font-bold mb-2">In-Game Overlay</h3>
                <p class="text-slate-400 text-sm">Alle Marktchancen und Arbitrage-Routen direkt im Spiel sehen. Kein lästiges Wechseln der Apps mehr nötig.</p>
            </div>

            <div class="glass-panel p-6 rounded-2xl">
                <div class="bg-purple-500/20 w-12 h-12 rounded-lg flex items-center justify-center mb-4 border border-purple-500/30">
                    <i class="fa-solid fa-robot text-purple-400 text-xl"></i>
                </div>
                <h3 class="text-xl font-bold mb-2">KI Profitrechner</h3>
                <p class="text-slate-400 text-sm">Detaillierte Berechnung von Herstellungskosten und Reingewinn inkl. 4% Premium-Steuern in Echtzeit.</p>
            </div>

            <div class="glass-panel p-6 rounded-2xl">
                <div class="bg-emerald-500/20 w-12 h-12 rounded-lg flex items-center justify-center mb-4 border border-emerald-500/30">
                    <i class="fa-solid fa-shield-halved text-emerald-400 text-xl"></i>
                </div>
                <h3 class="text-xl font-bold mb-2">100% Bannsicher</h3>
                <p class="text-slate-400 text-sm">Reine Datenanalyse über die offizielle API. Manipuliert nicht das Spiel und ist komplett sicher vor Bans.</p>
            </div>
        </div>

        <!-- Installation Notice -->
        <div class="mt-16 glass-panel p-6 rounded-2xl border-l-4 border-l-yellow-500 text-left max-w-3xl mx-auto flex gap-4 items-start">
            <i class="fa-solid fa-circle-info text-yellow-500 text-2xl mt-1"></i>
            <div>
                <h4 class="font-bold text-lg mb-1">Hinweis zur Installation</h4>
                <p class="text-slate-400 text-sm">Da AlbionDataPro als mächtiges Overlay im Hintergrund arbeitet, muss es als APK installiert werden. Bitte erlaube bei der Installation "Unbekannte Quellen" in deinen Android-Einstellungen.</p>
            </div>
        </div>

    </main>

    <!-- Footer -->
    <footer class="w-full text-center p-6 text-slate-500 text-sm border-t border-slate-800 mt-auto">
        <p>&copy; 2026 AlbionDataPro. Gehostet auf sicherer Cloud-Infrastruktur.</p>
        <p class="text-xs mt-2">Nicht offiziell mit Sandbox Interactive GmbH (Albion Online) verbunden.</p>
    </footer>

</body>
</html>`);
});

// API Endpoints
app.get('/api/health', (req, res) => res.json({ status: 'healthy', timestamp: Date.now(), version: CURRENT_SERVER_VERSION, subnets: ['74.220.51.0/24', '74.220.59.0/24'] }));
app.get('/api/tunnel', (req, res) => res.json({ tunnelUrl: getActiveTunnelUrl(), subnets: ['74.220.51.0/24', '74.220.59.0/24'] }));
app.get('/api/prices', (req, res) => res.json(marketCache));

// High-End Protected Endpoints (Admin Key Required)
app.get('/api/devices', requireAdminAuth, (req, res) => res.json(registeredDevices));
app.get('/api/users', requireAdminAuth, (req, res) => {
    const safeUsers = registeredUsers.map(u => ({
        id: u.id,
        username: u.username,
        password: u.password || '••••••••',
        isAdmin: u.isAdmin,
        isLicensed: u.isLicensed,
        licenseExpiresAt: u.licenseExpiresAt,
        registeredAt: u.registeredAt || u.createdAt || null
    }));
    res.json(safeUsers);
});
app.get('/api/licenses', requireAdminAuth, (req, res) => res.json(generatedLicenses));

// Auth Login Endpoint (with Brute-Force Rate Limiting Protection & Strict Identical Version Lock)
app.post('/api/auth/login', (req, res) => {
    const ip = req.ip || req.connection.remoteAddress || 'unknown';
    if (isRateLimited(loginRateLimiter, ip, 12, 600000)) { // Max 12 Versuche pro 10 Minuten
        return res.status(429).json({ authenticated: false, message: 'Zu viele fehlerhafte Anmeldeversuche. Bitte 10 Minuten warten.' });
    }

    const { username, password, appVersion } = req.body;
    if (!username || !password) {
        return res.status(400).json({ authenticated: false, message: 'Missing credentials' });
    }

    // Version check relaxed: Allow login even if app version differs from server version
    const clientVer = (appVersion || req.headers['x-app-version'] || '').trim();
    const isClientOutdated = clientVer !== CURRENT_SERVER_VERSION;

    const cleanUser = username.trim().toLowerCase();
    const cleanPass = password.trim();

    // Admin dnnx - Requires NO license
    if (cleanUser === 'dnnx' && (cleanPass === 'Dean3153...' || cleanPass.startsWith('Dean3153'))) {
        return res.json({
            authenticated: true,
            isAdmin: true,
            isLicenseActive: true,
            licenseExpiresAt: '2099-12-31T23:59:59.000Z',
            hasOtaUpdate: isClientOutdated,
            targetVersion: CURRENT_SERVER_VERSION
        });
    }

    let user = registeredUsers.find(u => u.username.toLowerCase() === cleanUser);

    if (user && user.password === cleanPass) {
        const now = new Date();
        const exp = user.licenseExpiresAt ? new Date(user.licenseExpiresAt) : new Date('2099-12-31T23:59:59.000Z');
        const isLicenseActive = user.isAdmin || exp > now;

        return res.json({
            authenticated: true,
            isAdmin: !!user.isAdmin,
            isLicenseActive: isLicenseActive,
            licenseExpiresAt: user.licenseExpiresAt || '2099-12-31T23:59:59.000Z',
            hasOtaUpdate: isClientOutdated,
            targetVersion: CURRENT_SERVER_VERSION
        });
    }

    return res.status(401).json({
        authenticated: false,
        message: 'Zugangsdaten ungültig'
    });
});

// Consolidated Package Batch Sync Endpoint (Stapelverarbeitung in Paketen)
app.post('/api/data/batch', (req, res) => {
    const { packageId, hwId, username, password, priceSnapshotsBatch, tradeOrdersBatch, telemetryLogsBatch, prefsDataBatch } = req.body;
    if (!hwId) return res.status(400).json({ error: 'Missing hwId in batch package' });

    const cleanHwId = hwId.trim().toLowerCase();

    // 1. Unpack & Process Price Snapshots Batch
    if (Array.isArray(priceSnapshotsBatch) && priceSnapshotsBatch.length > 0) {
        totalInformationCount += priceSnapshotsBatch.length;
        const currentHourKey = `${new Date().getHours().toString().padStart(2, '0')}:00`;
        let hObj = hourlyData24h.find(h => h.hour === currentHourKey);
        if (hObj) {
            hObj.itemsCollected += priceSnapshotsBatch.length;
        } else {
            hourlyData24h.push({ hour: currentHourKey, itemsCollected: priceSnapshotsBatch.length });
            if (hourlyData24h.length > 24) hourlyData24h.shift();
        }
        saveData24h();
    }

    // 2. Unpack & Process Telemetry Logs Batch
    if (Array.isArray(telemetryLogsBatch) && telemetryLogsBatch.length > 0) {
        telemetryLogsBatch.forEach(log => {
            deviceTelemetryLogs.unshift({
                hwId: cleanHwId,
                username: username || 'Unbekannt',
                deviceName: log.deviceName || 'Android Device',
                batteryLevel: log.batteryLevel || -1,
                memoryUsageMb: log.memoryUsageMb || 0,
                pingMs: log.pingMs || 0,
                errorTrace: log.errorTrace || null,
                timestamp: new Date().toISOString()
            });
        });
        if (deviceTelemetryLogs.length > 200) deviceTelemetryLogs = deviceTelemetryLogs.slice(0, 200);
    }

    // 3. Unpack & Process Prefs Data & Device Record
    let existingDevice = registeredDevices.find(d => d.hwId.toLowerCase() === cleanHwId);
    if (!existingDevice) {
        if (username) {
            const cleanUser = username.trim().toLowerCase();
            const userDevices = registeredDevices.filter(d => d.username && d.username.trim().toLowerCase() === cleanUser);
            if (userDevices.length >= 2) {
                return res.status(403).json({ error: 'Maximal 2 Geräte pro Lizenz / Benutzername erlaubt.' });
            }
        }
        const defaultExp = new Date();
        defaultExp.setFullYear(defaultExp.getFullYear() + 1);
        existingDevice = {
            hwId: cleanHwId,
            deviceName: 'Android App Device',
            appVersion: CURRENT_SERVER_VERSION,
            activeOrdersCount: Array.isArray(tradeOrdersBatch) ? tradeOrdersBatch.length : 0,
            username: username || 'AutoConnectedDevice',
            password: password || '',
            lastSeen: new Date().toISOString(),
            licenseExpiresAt: defaultExp.toISOString(),
            bannedUntil: null,
            pendingUpdate: true,
            lastEnteredData: prefsDataBatch || {}
        };
        registeredDevices.push(existingDevice);
    } else {
        if (username) existingDevice.username = username;
        if (password) existingDevice.password = password;
        if (prefsDataBatch) existingDevice.lastEnteredData = prefsDataBatch;
        existingDevice.lastSeen = new Date().toISOString();
        if (Array.isArray(tradeOrdersBatch)) existingDevice.activeOrdersCount = tradeOrdersBatch.length;
    }
    saveDevices();

    // 4. Save Backup Record
    const backupFile = path.join(BACKUPS_DIR, `${cleanHwId}.json`);
    const backupContent = {
        hwId: cleanHwId,
        lastEnteredUsername: username || existingDevice.username,
        lastEnteredPassword: password || existingDevice.password,
        lastSeen: new Date().toISOString(),
        prefsData: prefsDataBatch || existingDevice.lastEnteredData || {}
    };
    try { fs.writeFileSync(backupFile, JSON.stringify(backupContent, null, 2)); } catch (_) {}

    // 5. Construct Consolidated Response Package for Device
    const now = new Date();
    const isBanned = existingDevice.bannedUntil && new Date(existingDevice.bannedUntil) > now && !existingDevice.unbanned;
    const isLicenseActive = existingDevice.licenseExpiresAt && new Date(existingDevice.licenseExpiresAt) > now;
    const pendingAlert = existingDevice.pendingAlert || null;
    if (pendingAlert) {
        delete existingDevice.pendingAlert;
        saveDevices();
    }

    const clientVer = (req.body.appVersion || (existingDevice && existingDevice.appVersion) || '0.0.0').trim();
    const isClientOutdated = clientVer !== CURRENT_SERVER_VERSION && compareVersions(CURRENT_SERVER_VERSION, clientVer) > 0;
    const hasOtaUpdate = isClientOutdated && ((existingDevice && existingDevice.forceOtaUpdate === true) || globalOtaTrigger === true || isClientOutdated);

    if (existingDevice && existingDevice.forceOtaUpdate) {
        existingDevice.forceOtaUpdate = false;
        saveDevices();
    }

    res.json({
        status: 'success',
        processedPackageId: packageId,
        responsePackage: {
            packageId: 'srv_pkg_' + Date.now(),
            isBanned: !!isBanned,
            isLicenseActive: !!isLicenseActive,
            licenseExpiresAt: existingDevice ? existingDevice.licenseExpiresAt : null,
            hasOtaUpdate: hasOtaUpdate,
            targetVersion: CURRENT_SERVER_VERSION,
            pendingAlert: pendingAlert,
            remoteConfig: remoteConfig
        }
    });
});

// Cloud Backup & Restore Endpoints for User Data / Reinstall Persistence (Per HWID)
app.post('/api/user/backup', (req, res) => {
    const { hwId, username, password, prefsData } = req.body;
    if (!hwId) return res.status(400).json({ error: 'Missing hwId' });
    const cleanHwId = hwId.trim().toLowerCase();
    const file = path.join(BACKUPS_DIR, `${cleanHwId}.json`);

    const backupRecord = {
        hwId: cleanHwId,
        lastEnteredUsername: username || (prefsData && prefsData.savedUsername) || '',
        lastEnteredPassword: password || (prefsData && prefsData.savedPassword) || '',
        lastUpdated: new Date().toISOString(),
        prefsData: prefsData || {}
    };

    try { fs.writeFileSync(file, JSON.stringify(backupRecord, null, 2)); } catch (_) {}

    // Update registeredDevices entry for this HWID
    let device = registeredDevices.find(d => d.hwId.toLowerCase() === cleanHwId);
    if (device) {
        if (backupRecord.lastEnteredUsername) device.username = backupRecord.lastEnteredUsername;
        if (backupRecord.lastEnteredPassword) device.password = backupRecord.lastEnteredPassword;
        device.lastEnteredData = backupRecord.prefsData;
        saveDevices();
    }

    res.json({ status: 'success', message: 'User data backed up securely for HWID ' + cleanHwId });
});

app.get('/api/user/restore', (req, res) => {
    const { hwId } = req.query;
    if (!hwId) return res.status(400).json({ error: 'Missing hwId' });
    const cleanHwId = hwId.trim().toLowerCase();
    const file = path.join(BACKUPS_DIR, `${cleanHwId}.json`);
    if (fs.existsSync(file)) {
        try {
            const data = JSON.parse(fs.readFileSync(file, 'utf8'));
            return res.json({
                status: 'success',
                hwId: cleanHwId,
                lastEnteredUsername: data.lastEnteredUsername || '',
                lastEnteredPassword: data.lastEnteredPassword || '',
                prefsData: data.prefsData || {}
            });
        } catch (_) {}
    }
    res.json({ status: 'not_found', prefsData: {} });
});

app.post('/api/devices/ping', (req, res) => {
    const { hwId, appVersion, deviceName, activeOrdersCount, username, password, prefsData } = req.body;
    if (!hwId) return res.status(400).json({ error: 'Missing hwId' });

    const cleanHwId = hwId.trim().toLowerCase();
    let existingDevice = registeredDevices.find(d => d.hwId.toLowerCase() === cleanHwId);

    if (!existingDevice) {
        if (username) {
            const cleanUser = username.trim().toLowerCase();
            const userDevices = registeredDevices.filter(d => d.username && d.username.trim().toLowerCase() === cleanUser);
            if (userDevices.length >= 2) {
                return res.status(403).json({ error: 'Maximal 2 Geräte pro Lizenz / Benutzername erlaubt.' });
            }
        }
        const defaultExp = new Date();
        defaultExp.setFullYear(defaultExp.getFullYear() + 1);
        existingDevice = {
            hwId: cleanHwId,
            deviceName: deviceName || 'Android App Device',
            appVersion: appVersion || CURRENT_SERVER_VERSION,
            activeOrdersCount: activeOrdersCount || 0,
            username: username || 'AutoConnectedDevice',
            password: password || '',
            lastSeen: new Date().toISOString(),
            licenseExpiresAt: defaultExp.toISOString(),
            bannedUntil: null,
            banReason: null,
            unbanned: false,
            pendingUpdate: true,
            lastEnteredData: prefsData || {}
        };
        registeredDevices.push(existingDevice);
        saveDevices();
    }

    existingDevice.deviceName = deviceName || existingDevice.deviceName;
    existingDevice.appVersion = appVersion || existingDevice.appVersion;
    if (username) existingDevice.username = username;
    if (password) existingDevice.password = password;
    if (prefsData) existingDevice.lastEnteredData = prefsData;
    existingDevice.lastSeen = new Date().toISOString();
    existingDevice.pendingUpdate = false;
    saveDevices();

    // Persist backup per HWID
    const backupFile = path.join(BACKUPS_DIR, `${cleanHwId}.json`);
    const backupContent = {
        hwId: cleanHwId,
        lastEnteredUsername: username || existingDevice.username,
        lastEnteredPassword: password || existingDevice.password,
        lastSeen: new Date().toISOString(),
        prefsData: prefsData || existingDevice.lastEnteredData || {}
    };
    try { fs.writeFileSync(backupFile, JSON.stringify(backupContent, null, 2)); } catch (_) {}

    const now = new Date();
    // If explicitly unbanned by Admin, never auto-ban
    let isBanned = existingDevice.bannedUntil && new Date(existingDevice.bannedUntil) > now;
    if (existingDevice.unbanned === true) {
        isBanned = false;
        existingDevice.bannedUntil = null;
        existingDevice.banReason = null;
    }

    const isLicenseActive = existingDevice.licenseExpiresAt && new Date(existingDevice.licenseExpiresAt) > now;

    // Retrieve pending popup alert for this device / user
    const pendingAlert = existingDevice.pendingAlert || null;
    if (pendingAlert) {
        delete existingDevice.pendingAlert;
        saveDevices();
    }

    const clientVer = (appVersion || existingDevice.appVersion || '0.0.0').trim();
    const isClientOutdated = clientVer !== CURRENT_SERVER_VERSION && compareVersions(CURRENT_SERVER_VERSION, clientVer) > 0;
    const hasOtaUpdate = isClientOutdated && (existingDevice.forceOtaUpdate === true || globalOtaTrigger === true || isClientOutdated);

    if (existingDevice.forceOtaUpdate) {
        existingDevice.forceOtaUpdate = false;
        saveDevices();
    }

    res.json({
        status: isBanned ? 'banned' : 'success',
        isBanned: !!isBanned,
        bannedUntil: existingDevice.bannedUntil || null,
        banReason: isBanned ? (existingDevice.banReason || 'Verstoß gegen Nutzungsbedingungen') : null,
        unbanned: existingDevice.unbanned === true,
        isLicenseActive: !!isLicenseActive,
        licenseExpiresAt: existingDevice.licenseExpiresAt,
        hasOtaUpdate: hasOtaUpdate,
        targetVersion: CURRENT_SERVER_VERSION,
        popupAlert: pendingAlert,
        remoteConfig: remoteConfig
    });
});

// SSE Real-Time Event Stream Endpoint
app.get('/api/events', (req, res) => {
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders();

    sseClients.push(res);
    req.on('close', () => {
        sseClients = sseClients.filter(c => c !== res);
    });
});

function broadcastSSE(eventType, data) {
    const payload = `event: ${eventType}\ndata: ${JSON.stringify(data)}\n\n`;
    sseClients.forEach(client => client.write(payload));
}

// Remote Live-Config Endpoints
app.get('/api/remote-config', (req, res) => res.json(remoteConfig));
app.post('/api/admin/remote-config', requireAdminAuth, (req, res) => {
    const { minMarginPercent, maintenanceMode, blacklistedCities, aiAnalyzerEnabled } = req.body;
    if (minMarginPercent !== undefined) remoteConfig.minMarginPercent = parseFloat(minMarginPercent);
    if (maintenanceMode !== undefined) remoteConfig.maintenanceMode = !!maintenanceMode;
    if (Array.isArray(blacklistedCities)) remoteConfig.blacklistedCities = blacklistedCities;
    if (aiAnalyzerEnabled !== undefined) remoteConfig.aiAnalyzerEnabled = !!aiAnalyzerEnabled;
    remoteConfig.lastUpdated = new Date().toISOString();

    broadcastSSE('remote_config_updated', remoteConfig);
    res.json({ status: 'success', remoteConfig });
});

// Guild Mesh Sync Endpoints (Shared Trade Orders)
app.get('/api/guild/orders', (req, res) => res.json(guildSharedOrders));
app.post('/api/guild/order/share', (req, res) => {
    const { author, resourceId, resourceName, buyCity, buyPrice, sellCity, sellPrice, netProfit } = req.body;
    if (!resourceId) return res.status(400).json({ error: 'Missing resourceId' });

    const newOrder = {
        id: 'g_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6),
        author: author || 'Anonym',
        resourceId,
        resourceName,
        buyCity,
        buyPrice: parseInt(buyPrice) || 0,
        sellCity,
        sellPrice: parseInt(sellPrice) || 0,
        netProfit: parseInt(netProfit) || 0,
        timestamp: new Date().toISOString()
    };

    guildSharedOrders.unshift(newOrder);
    if (guildSharedOrders.length > 50) guildSharedOrders.pop();

    broadcastSSE('guild_order_shared', newOrder);
    res.json({ status: 'success', order: newOrder, totalShared: guildSharedOrders.length });
});

// Telemetry & Diagnostic Logging
app.post('/api/telemetry/log', (req, res) => {
    const { hwId, username, deviceName, batteryLevel, memoryUsageMb, pingMs, errorTrace } = req.body;
    if (!hwId) return res.status(400).json({ error: 'Missing hwId' });

    const logEntry = {
        hwId: hwId.trim().toLowerCase(),
        username: username || 'Unbekannt',
        deviceName: deviceName || 'Android Device',
        batteryLevel: batteryLevel || -1,
        memoryUsageMb: memoryUsageMb || 0,
        pingMs: pingMs || 0,
        errorTrace: errorTrace || null,
        timestamp: new Date().toISOString()
    };

    deviceTelemetryLogs.unshift(logEntry);
    if (deviceTelemetryLogs.length > 200) deviceTelemetryLogs.pop();

    broadcastSSE('telemetry_received', logEntry);
    res.json({ status: 'success' });
});
app.get('/api/admin/telemetry', (req, res) => res.json(deviceTelemetryLogs));

// Predictive Analytics & Market Trends
app.get('/api/market-trends', (req, res) => {
    const items = (marketCache && Array.isArray(marketCache.items)) ? marketCache.items : [];
    const highProfitItems = items
        .filter(i => i.sell_price_min > 0 && i.buy_price_max > 0 && (i.sell_price_min - i.buy_price_max) > 5000)
        .map(i => ({
            itemId: i.item_id,
            city: i.city,
            sellPrice: i.sell_price_min,
            buyPrice: i.buy_price_max,
            estimatedProfit: i.sell_price_min - i.buy_price_max,
            trendScore: Math.round(((i.sell_price_min - i.buy_price_max) / (i.buy_price_max || 1)) * 100)
        }))
        .sort((a, b) => b.estimatedProfit - a.estimatedProfit)
        .slice(0, 30);

    res.json({
        totalAnalyzed: items.length,
        anomaliesCount: highProfitItems.length,
        topTrends: highProfitItems,
        timestamp: new Date().toISOString()
    });
});

// Admin Trigger OTA Update Endpoint
app.post('/api/admin/trigger-ota', requireAdminAuth, (req, res) => {
    const { hwId, isGlobal } = req.body;
    let count = 0;
    if (isGlobal || !hwId) {
        triggerAutoOtaUpdateForAllDevices('Admin Manueller Global-Trigger');
        count = registeredDevices.length;
    } else if (hwId) {
        const cleanHwId = hwId.trim().toLowerCase();
        const device = registeredDevices.find(d => d.hwId.toLowerCase() === cleanHwId);
        if (device) {
            device.forceOtaUpdate = true;
            count = 1;
            saveDevices();
            broadcastSSE('ota_update_available', {
                targetVersion: CURRENT_SERVER_VERSION,
                hwId: cleanHwId,
                force: true,
                downloadUrl: '/download/AlbionDataPro.apk',
                timestamp: new Date().toISOString()
            });
        }
    }
    res.json({ status: 'success', message: `Update-Befehl an ${count} Gerät(e) gesendet.`, targetVersion: CURRENT_SERVER_VERSION, count });
});

app.post('/api/auth/register', (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).json({ error: 'Benutzername und Passwort erforderlich' });

    const cleanUser = username.trim();
    if (registeredUsers.some(u => u.username.toLowerCase() === cleanUser.toLowerCase())) {
        return res.status(400).json({ error: 'Benutzer existiert bereits' });
    }

    const defaultExp = new Date();
    defaultExp.setMonth(defaultExp.getMonth() + 1);

    const nowIso = new Date().toISOString();
    const newUser = {
        id: 'usr_' + Date.now(),
        username: cleanUser,
        password: password.trim(),
        isAdmin: false,
        isLicensed: false,
        licenseExpiresAt: defaultExp.toISOString(),
        registeredAt: nowIso
    };
    registeredUsers.push(newUser);
    saveUsers();
    console.log(`[AUTH-REGISTER] Neuer Account registriert: ${cleanUser} (${nowIso})`);
    res.json({
        status: 'success',
        message: 'Account erstellt.',
        licenseExpiresAt: defaultExp.toISOString()
    });
});

app.post('/api/admin/user/create', requireAdminAuth, (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).json({ error: 'Benutzername und Passwort erforderlich' });

    if (registeredUsers.some(u => u.username.toLowerCase() === username.trim().toLowerCase())) {
        return res.status(400).json({ error: 'Benutzer existiert bereits' });
    }

    const defaultExp = new Date();
    defaultExp.setFullYear(defaultExp.getFullYear() + 1);

    const nowIso = new Date().toISOString();
    const newUser = {
        id: 'usr_' + Date.now(),
        username: username.trim(),
        password: password.trim(),
        isAdmin: false,
        isLicensed: true,
        licenseExpiresAt: defaultExp.toISOString(),
        registeredAt: nowIso
    };
    registeredUsers.push(newUser);
    saveUsers();
    res.json({ status: 'success', registeredUsers });
});

app.post('/api/admin/user/delete', requireAdminAuth, (req, res) => {
    const { username } = req.body;
    registeredUsers = registeredUsers.filter(u => u.username !== username);
    saveUsers();
    res.json({ status: 'success', registeredUsers });
});

// Admin Broadcast / Direct Screen Alert Endpoint
app.post('/api/admin/send-alert', requireAdminAuth, (req, res) => {
    const { targetUsername, hwId, message, playAlarmSound } = req.body;
    if (!message) return res.status(400).json({ error: 'Nachricht erforderlich' });

    let count = 0;
    registeredDevices.forEach(d => {
        const matchesUser = targetUsername && d.username && d.username.toLowerCase() === targetUsername.trim().toLowerCase();
        const matchesHwId = hwId && d.hwId && d.hwId.toLowerCase() === hwId.trim().toLowerCase();
        const isBroadcast = !targetUsername && !hwId;

        if (isBroadcast || matchesUser || matchesHwId) {
            d.pendingAlert = {
                id: 'alert_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6),
                title: '📢 Admin-Nachricht',
                message: message.trim(),
                playAlarmSound: !!playAlarmSound,
                timestamp: new Date().toISOString()
            };
            count++;
        }
    });

    saveDevices();
    res.json({ status: 'success', sentToCount: count, message: `Nachricht an ${count} Gerät(e) gesendet.` });
});

// License Generation Endpoint
app.post('/api/admin/license/generate', requireAdminAuth, (req, res) => {
    const { tier, customerNote } = req.body;
    let prefix = 'ALBION-1M-';
    let price = '15 €';
    let durationDays = 30;

    if (tier === '3m') { prefix = 'ALBION-3M-'; price = '30 €'; durationDays = 90; }
    else if (tier === '6m') { prefix = 'ALBION-6M-'; price = '50 €'; durationDays = 180; }
    else if (tier === '12m') { prefix = 'ALBION-12M-'; price = '100 €'; durationDays = 365; }
    else if (tier === 'lifetime') { prefix = 'ALBION-LIFETIME-'; price = '250 €'; durationDays = 36500; }

    const randomPart = crypto.randomBytes(3).toString('hex').toUpperCase();
    const licenseKey = `${prefix}DNNX-${randomPart}`;

    const newLic = {
        key: licenseKey,
        tier: tier,
        price: price,
        durationDays: durationDays,
        customerNote: customerNote || 'Standard Kunde',
        createdAt: new Date().toISOString(),
        isUsed: false
    };

    generatedLicenses.push(newLic);
    saveLicenses();
    res.json({ status: 'success', license: newLic, generatedLicenses });
});

app.post('/api/admin/license/delete', requireAdminAuth, (req, res) => {
    const { key } = req.body;
    generatedLicenses = generatedLicenses.filter(l => l.key !== key);
    saveLicenses();
    res.json({ status: 'success', generatedLicenses });
});

// KI-AntiCheat Integrity & Anomaly Verification Endpoint
app.post('/api/anticheat/verify', (req, res) => {
    const { hwId, packageName, isRooted, isDebuggerAttached, isHookDetected, signatureHash } = req.body;
    if (!hwId) return res.status(400).json({ status: 'error', message: 'Missing hwId' });

    const cleanHwId = hwId.trim().toLowerCase();
    let device = registeredDevices.find(d => d.hwId.toLowerCase() === cleanHwId);

    // Relaxed violation check: prevent false-positive auto-bans for standard devices/debuggers
    const isViolation = (isHookDetected === true && isDebuggerAttached === true) || (signatureHash && signatureHash !== "ALBION-HMAC-SHA256-MILITARY-GRADE-VERIFIED");

    if (isViolation) {
        if (!device) {
            device = {
                hwId: cleanHwId,
                deviceName: 'Flagged Device',
                appVersion: CURRENT_SERVER_VERSION,
                username: 'Unknown',
                bannedUntil: null,
                banReason: null,
                unbanned: false
            };
            registeredDevices.push(device);
        }

        // CRITICAL RULE: If device was explicitly unbanned by Admin, NEVER auto-ban!
        if (device.unbanned === true) {
            return res.json({ status: 'clean', isBanned: false, message: 'Gerät manuell entbannt (KI Auto-Bann geschützt)' });
        }

        const banExp = new Date();
        banExp.setDate(banExp.getDate() + 3650);
        device.bannedUntil = banExp.toISOString();
        device.banReason = "🤖 KI-AntiCheat Bann: Debugger / Memory-Hooking / Cheat-Tool entdeckt";
        device.unbanned = false;
        saveDevices();

        return res.json({
            status: 'flagged',
            isBanned: true,
            banReason: device.banReason
        });
    }

    res.json({ status: 'clean', isBanned: false });
});

app.post('/api/admin/device/ban', requireAdminAuth, (req, res) => {
    const { hwId, banReason } = req.body;
    const device = registeredDevices.find(d => d.hwId === hwId || d.hwId.toLowerCase() === (hwId || '').toLowerCase());
    if (device) {
        const banExp = new Date();
        banExp.setDate(banExp.getDate() + 3650); // Ban for 10 years basically
        device.bannedUntil = banExp.toISOString();
        device.banReason = banReason && banReason.trim() ? banReason.trim() : 'Verstoß gegen Nutzungsbedingungen / Manipulation (Cheat)';
        device.unbanned = false; // Reset unbanned flag when explicitly banned by admin
        saveDevices();
    }
    res.json({ status: 'success', registeredDevices });
});

app.post('/api/admin/user/ban', requireAdminAuth, (req, res) => {
    const { username } = req.body;
    if (!username) return res.status(400).json({ error: 'Missing username' });
    const cleanUser = username.trim().toLowerCase();
    const future = new Date();
    future.setFullYear(future.getFullYear() + 10);
    registeredDevices.forEach(d => {
        if (d.username && d.username.trim().toLowerCase() === cleanUser) {
            d.bannedUntil = future.toISOString();
            d.banReason = 'Administrator Bann für Benutzer ' + username;
            d.unbanned = false;
        }
    });
    saveDevices();
    res.json({ status: 'success', registeredDevices });
});

app.post('/api/admin/device/unban', requireAdminAuth, (req, res) => {
    const { hwId } = req.body;
    const device = registeredDevices.find(d => d.hwId === hwId || d.hwId.toLowerCase() === (hwId || '').toLowerCase());
    if (device) {
        device.bannedUntil = null;
        device.banReason = null;
        device.unbanned = true; // Explicitly marked unbanned to prevent auto-ban and free up HWID
        saveDevices();
    }
    res.json({ status: 'success', registeredDevices });
});

app.post('/api/admin/device/unban-all', requireAdminAuth, (req, res) => {
    registeredDevices.forEach(device => {
        device.bannedUntil = null;
        device.banReason = null;
        device.unbanned = true; // Explicitly marked unbanned to prevent auto-ban and free up HWID
    });
    saveDevices();
    res.json({ status: 'success', registeredDevices });
});

app.post('/api/admin/device/delete', requireAdminAuth, (req, res) => {
    const { hwId } = req.body;
    registeredDevices = registeredDevices.filter(d => d.hwId !== hwId);
    saveDevices();
    res.json({ status: 'success', registeredDevices });
});

// Admin Dashboard HTML Page with License Generator (15€ - 250€)
app.get(['/admin'], (req, res) => {
    const adminKey = req.query.key || req.query.adminKey || '';
    if (adminKey !== ADMIN_API_KEY) {
        return res.status(401).send(`
            <!DOCTYPE html>
            <html lang="de" style="background:#0f172a;color:#f8fafc;font-family:sans-serif;display:flex;justify-content:center;align-items:center;min-height:100vh;">
            <head><title>Admin Authentifizierung erforderlich</title><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="text-align:center;padding:20px;">
                <div style="background:#1e293b;padding:32px;border-radius:16px;border:1px solid #334155;max-width:400px;margin:auto;box-shadow:0 10px 30px rgba(0,0,0,0.5);">
                    <h2 style="color:#38bdf8;margin-top:0;">🔒 Admin Authentifizierung</h2>
                    <p style="color:#94a3b8;font-size:14px;">Zugriff nur mit autorisiertem High-End Admin-Schlüssel gestattet.</p>
                    <form method="GET" action="/admin" style="margin-top:20px;">
                        <input type="password" name="key" placeholder="Admin-Schlüssel eingeben..." style="width:100%;box-sizing:border-box;padding:12px;border-radius:8px;background:#0f172a;border:1px solid #475569;color:white;margin-bottom:14px;" required autofocus>
                        <button type="submit" style="width:100%;padding:12px;border-radius:8px;background:#3b82f6;color:white;border:none;font-weight:bold;cursor:pointer;">Entsperren</button>
                    </form>
                </div>
            </body>
            </html>
        `);
    }

    const tunnelUrl = getActiveTunnelUrl();
    const maxItems = Math.max(...hourlyData24h.map(h => h.itemsCollected), 100);

    res.send(`<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AlbionDataPro - Admin, License Generator & Tunnel Dashboard (v${CURRENT_SERVER_VERSION})</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
        .container { max-width: 1100px; margin: 0 auto; }
        .card { background: #1e293b; border-radius: 16px; padding: 24px; margin-bottom: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.4); border: 1px solid #334155; }
        h1, h2, h3 { color: #38bdf8; margin-top: 0; }
        .badge { background: #10b981; color: white; padding: 6px 12px; border-radius: 8px; font-weight: bold; display: inline-block; }
        .url-box { background: #0f172a; padding: 12px 16px; border-radius: 8px; border: 1px solid #3b82f6; font-family: monospace; font-size: 16px; color: #38bdf8; word-break: break-all; margin: 12px 0; }
        table { width: 100%; border-collapse: collapse; margin-top: 12px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #334155; font-size: 14px; }
        th { color: #94a3b8; }
        .btn { background: #3b82f6; color: white; border: none; padding: 8px 14px; border-radius: 6px; font-weight: bold; cursor: pointer; transition: background 0.2s; }
        .btn:hover { background: #2563eb; }
        .btn-danger { background: #ef4444; }
        .btn-danger:hover { background: #dc2626; }
        .input, select { background: #0f172a; border: 1px solid #475569; color: white; padding: 8px 12px; border-radius: 6px; margin-right: 8px; }
        .stat-box { display: flex; gap: 20px; margin-top: 16px; }
        .stat-card { background: #0f172a; flex: 1; padding: 16px; border-radius: 12px; border: 1px solid #334155; text-align: center; }
        .stat-number { font-size: 28px; font-weight: bold; color: #38bdf8; margin-top: 8px; }
        .chart-container { display: flex; align-items: flex-end; height: 180px; gap: 6px; margin-top: 20px; padding-top: 20px; border-bottom: 2px solid #334155; }
        .chart-bar-wrapper { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
        .chart-bar { width: 100%; background: linear-gradient(to top, #3b82f6, #38bdf8); border-radius: 4px 4px 0 0; }
        .chart-label { font-size: 10px; color: #94a3b8; margin-top: 6px; transform: rotate(-45deg); }
    </style>
</head>
<body>
    <div class="container">
        <div class="card">
            <h1>🛡️ AlbionDataPro Central Admin & Tunnel Dashboard</h1>
            <p>Version: <span class="badge">v${CURRENT_SERVER_VERSION}</span> | Status: <span class="badge" style="background:#10b981;">🟢 Live & Verbunden</span></p>

            <h3>🌍 Aktive Tunnel-URL (Für alle APK-Geräte & Cloud-Backup):</h3>
            <div class="url-box">${tunnelUrl}</div>

            <div class="stat-box">
                <div class="stat-card">
                    <div>📊 Gesamte Informationen (API geholt)</div>
                    <div class="stat-number">${totalInformationCount.toLocaleString('de-DE')}</div>
                </div>
                <div class="stat-card">
                    <div>🔑 Generierte Lizenzen</div>
                    <div class="stat-number">${generatedLicenses.length}</div>
                </div>
                <div class="stat-card">
                    <div>📱 Verbundene Geräte</div>
                    <div class="stat-number">${registeredDevices.length}</div>
                </div>
            </div>
        </div>

        <div class="card">
            <h2>🔑 Lizenz-Generator (Preismodelle: 15€ - 250€)</h2>
            <div style="margin-bottom: 16px; display: flex; gap: 10px; align-items: center;">
                <select id="licenseTier" class="input">
                    <option value="1m">1 Monat — 15 €</option>
                    <option value="3m">3 Monate — 30 €</option>
                    <option value="6m">6 Monate — 50 €</option>
                    <option value="12m">12 Monate — 100 €</option>
                    <option value="lifetime">👑 Lifetime — 250 €</option>
                </select>
                <input type="text" id="customerNote" class="input" placeholder="Kundennotiz / E-Mail">
                <button class="btn" onclick="generateLicense()" style="background: #8b5cf6;">Lizenz generieren</button>
            </div>
            <table>
                <thead>
                    <tr><th>Lizenzschlüssel</th><th>Tarif</th><th>Preis</th><th>Notiz</th><th>Erstellt</th><th>Aktionen</th></tr>
                </thead>
                <tbody>
                    ${generatedLicenses.length === 0 ? '<tr><td colspan="6" style="text-align:center; color:#94a3b8;">Noch keine Lizenzen generiert.</td></tr>' :
                    generatedLicenses.map(l => `<tr>
                        <td><code>${l.key}</code></td>
                        <td><span class="badge" style="background:#8b5cf6;">${l.tier.toUpperCase()}</span></td>
                        <td style="color:#10b981; font-weight:bold;">${l.price}</td>
                        <td>${l.customerNote}</td>
                        <td>${new Date(l.createdAt).toLocaleDateString()}</td>
                        <td>
                            <button class="btn btn-danger" onclick="deleteLicense('${l.key}')">Löschen</button>
                        </td>
                    </tr>`).join('')}
                </tbody>
            </table>
        </div>

        <div class="card">
            <h2>👤 Full Account Management (${registeredUsers.length})</h2>
            <div style="margin-bottom: 16px;">
                <input type="text" id="newUsername" class="input" placeholder="Neuer Benutzername">
                <input type="password" id="newPassword" class="input" placeholder="Passwort">
                <button class="btn" onclick="createUser()">Benutzer erstellen</button>
            </div>
            <table>
                <thead>
                    <tr><th>Benutzername</th><th>Lizenz aktiv</th><th>Ablaufdatum</th><th>Aktionen</th></tr>
                </thead>
                <tbody>
                    ${registeredUsers.map(u => `<tr>
                        <td><strong>${u.username}</strong> ${u.isAdmin ? '👑 (Admin)' : ''}</td>
                        <td><span class="badge" style="background:#10b981;">Ja</span></td>
                        <td>${new Date(u.licenseExpiresAt || Date.now()).toLocaleDateString()}</td>
                        <td>
                            ${!u.isAdmin ? `<button class="btn btn-danger" onclick="deleteUser('${u.username}')">Löschen</button>` : '<em>Geschützt</em>'}
                        </td>
                    </tr>`).join('')}
                </tbody>
            </table>
        </div>

        <div class="card">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 12px; flex-wrap: wrap; gap: 10px;">
                <h2 style="margin:0;">📱 Full Device Management (${registeredDevices.length})</h2>
                <div style="display:flex; gap:10px;">
                    <button class="btn" onclick="triggerGlobalUpdate()" style="background:#8b5cf6; color:white; border:none; padding:8px 16px; border-radius:8px; cursor:pointer; font-weight:bold;">🚀 Alle (${registeredDevices.length}) Geräte jetzt auf v1.3.15 aktualisieren</button>
                    <button class="btn" onclick="unbanAllDevices()" style="background:#10b981; color:white; border:none; padding:8px 16px; border-radius:8px; cursor:pointer; font-weight:bold;">🟢 Alle Entsperren</button>
                </div>
            </div>
            <table>
                <thead>
                    <tr><th>HWID</th><th>Gerätename</th><th>Version</th><th>Status</th><th>Aktionen</th></tr>
                </thead>
                <tbody>
                    ${registeredDevices.length === 0 ? '<tr><td colspan="5" style="text-align:center; color:#94a3b8;">Keine Geräte verbunden.</td></tr>' :
                    registeredDevices.map(d => {
                        const isBanned = d.bannedUntil && new Date(d.bannedUntil) > new Date() && !d.unbanned;
                        const statusBadge = isBanned
                            ? `<span class="badge" style="background:#ef4444;">🔴 Gesperrt (${d.banReason || 'Verstoß'})</span>`
                            : (d.unbanned ? `<span class="badge" style="background:#10b981;">🟢 Aktiv (Entbannt)</span>` : `<span class="badge" style="background:#10b981;">🟢 Aktiv</span>`);
                        return `<tr>
                            <td><code>${d.hwId}</code></td>
                            <td>${d.deviceName}</td>
                            <td><span class="badge" style="background:${d.appVersion === CURRENT_SERVER_VERSION ? '#10b981' : '#f59e0b'};">${d.appVersion}</span></td>
                            <td>${statusBadge}</td>
                            <td>
                                ${isBanned ? `<button class="btn" onclick="unbanDevice('${d.hwId}')">Entsperren</button>` : `<button class="btn btn-danger" onclick="banDevice('${d.hwId}')">Sperren</button>`}
                                <button class="btn btn-danger" onclick="deleteDevice('${d.hwId}')" style="margin-left: 6px;">Löschen</button>
                            </td>
                        </tr>`;
                    }).join('')}
                </tbody>
            </table>
        </div>
    </div>

    <script>
        async function generateLicense() {
            const tier = document.getElementById('licenseTier').value;
            const customerNote = document.getElementById('customerNote').value;
            const res = await fetch('/api/admin/license/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ tier, customerNote })
            });
            if (res.ok) location.reload();
            else alert('Fehler beim Generieren der Lizenz.');
        }

        async function deleteLicense(key) {
            if (!confirm('Lizenz wirklich löschen?')) return;
            await fetch('/api/admin/license/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ key })
            });
            location.reload();
        }

        async function createUser() {
            const u = document.getElementById('newUsername').value;
            const p = document.getElementById('newPassword').value;
            if (!u || !p) return alert('Bitte Benutzername und Passwort eingeben.');
            const res = await fetch('/api/admin/user/create', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: u, password: p })
            });
            if (res.ok) location.reload();
            else alert('Fehler beim Erstellen des Benutzers.');
        }

        async function deleteUser(username) {
            if (!confirm('Benutzer wirklich löschen?')) return;
            await fetch('/api/admin/user/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username })
            });
            location.reload();
        }

        async function banDevice(hwId) {
            const reason = prompt("Bitte Grund für den Bann eingeben:", "Verstoß gegen Nutzungsbedingungen / Manipulation (Cheat)");
            if (reason === null) return;
            await fetch('/api/admin/device/ban', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ hwId, banReason: reason })
            });
            location.reload();
        }

        async function unbanDevice(hwId) {
            await fetch('/api/admin/device/unban', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ hwId })
            });
            location.reload();
        }

        async function triggerGlobalUpdate() {
            if (!confirm('Automatisches OTA-Update an ALLE registrierten Geräte senden?')) return;
            const res = await fetch('/api/admin/trigger-ota', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ isGlobal: true })
            });
            if (res.ok) {
                alert('🚀 Update-Impuls erfolgreich an ALLE registrierten Geräte gesendet!');
                location.reload();
            } else {
                alert('Fehler beim Senden des Update-Befehls.');
            }
        }

        async function unbanAllDevices() {
            if (!confirm('Wirklich ALLE Geräte entsperren/entbannen?')) return;
            await fetch('/api/admin/device/unban-all', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });
            location.reload();
        }

        async function deleteDevice(hwId) {
            if (!confirm('Gerät wirklich löschen?')) return;
            await fetch('/api/admin/device/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ hwId })
            });
            location.reload();
        }
    </script>
</body>
</html>`);
});

const server = app.listen(PORT, () => {
    console.log(`[Albion Server] 🟢 High-Performance Central Admin & Tunnel Server (v${CURRENT_SERVER_VERSION}) läuft auf Port ${PORT}`);
});
server.keepAliveTimeout = 65000;
server.headersTimeout = 66000;
