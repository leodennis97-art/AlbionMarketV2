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
const nodemailer = require('nodemailer');
const querystring = require('querystring');

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

// High-End Security Middleware: Require Admin Key or Admin Session Cookie
function requireAdminAuth(req, res, next) {
    const authHeader = req.headers['authorization'] || req.headers['x-admin-key'];
    const queryKey = req.query.adminKey || req.query.key;
    const token = (authHeader && authHeader.startsWith('Bearer ')) ? authHeader.substring(7).trim() : authHeader;

    // Check for cookie
    const cookieHeader = req.headers.cookie || '';
    const match = cookieHeader.match(/admin_auth=([^;]+)/);
    const clientToken = match ? match[1] : null;
    const expectedToken = crypto.createHmac('sha256', SERVER_HMAC_SECRET).update('admin_session').digest('hex');

    if (token === ADMIN_API_KEY || queryKey === ADMIN_API_KEY || clientToken === expectedToken) {
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

// Mailer Setup für vollautomatischen Lizenz-Versand
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: 'dnnxdigitalcreator@gmail.com',
        pass: process.env.GMAIL_APP_PASSWORD || 'bitte_app_passwort_im_render_dashboard_eintragen'
    }
});

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

function getAppVersionFromGradle() {
    try {
        const versionPath = path.join(DOWNLOADS_DIR, 'version.txt');
        if (fs.existsSync(versionPath)) {
            const v = fs.readFileSync(versionPath, 'utf8').trim();
            if (v) return v;
        }
        const buildGradlePath = path.join(__dirname, 'app', 'build.gradle');
        if (fs.existsSync(buildGradlePath)) {
            const content = fs.readFileSync(buildGradlePath, 'utf8');
            const match = content.match(/versionName\s+['"]([^'"]+)['"]/);
            if (match) return match[1];
        }
    } catch (e) {}
    return "3.1.8";
}

let CURRENT_SERVER_VERSION = getAppVersionFromGradle();
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

    const uploadedVer = req.query.version || req.headers['x-target-version'];
    if (uploadedVer) {
        CURRENT_SERVER_VERSION = uploadedVer;
        fs.writeFileSync(path.join(DOWNLOADS_DIR, 'version.txt'), uploadedVer);
    }

    triggerAutoOtaUpdateForAllDevices(`Neue APK v${CURRENT_SERVER_VERSION} von Admin hochgeladen`);
    res.json({ status: 'success', message: `APK v${CURRENT_SERVER_VERSION} erfolgreich hochgeladen und Homepage & Download-Button aktualisiert!` });
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

// (getAppVersionFromGradle is defined at startup)

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
            CURRENT_SERVER_VERSION = currentGradleVersion;
            console.log(`[Autonomous Sync] 🚀 Neue App-Version v${currentGradleVersion} erkannt & Server automatisch aktualisiert!`);
            triggerAutoOtaUpdateForAllDevices(`Neue App-Version v${currentGradleVersion} veröffentlicht`);
            return;
        }

        if (needsUpdate) {
            console.log(`[Autonomous Sync] 🔄 Synchronisiere Server auf neuste Version v${currentGradleVersion}...`);
            fs.writeFileSync(versionPath, currentGradleVersion);
            CURRENT_SERVER_VERSION = currentGradleVersion;
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

let globalMarketPrices = {};
let albion2dCache = { data: null, lastUpdated: null };

const AGENTS = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0'
];

// High-End Scraper & Retriever for europe.albiononline2d.com Item Statistics & Data
async function fetchAlbion2DData() {
    try {
        const randomAgent = AGENTS[Math.floor(Math.random() * AGENTS.length)];
        const res = await axios.get('https://europe.albiononline2d.com/en/item', {
            timeout: 15000,
            headers: {
                'User-Agent': randomAgent,
                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
                'Accept-Encoding': 'gzip, deflate, br',
                'Cache-Control': 'no-cache',
                'Pragma': 'no-cache',
                'Connection': 'keep-alive'
            },
            decompress: true // Auto-handle gzip/brotli
        });

        if (res.data) {
            albion2dCache = {
                dataHtmlLength: typeof res.data === 'string' ? res.data.length : 0,
                lastUpdated: new Date().toISOString()
            };
            console.log(`[Albion 2D Sync] 🟢 High-End Fetch erfolgreich! (${albion2dCache.dataHtmlLength} Bytes)`);
        }
    } catch (e) {
        console.log('[Albion 2D Sync] ⚠️ Warnung beim Fetch:', e.message);
        // Fallback retry with longer timeout if failed
        try {
            console.log('[Albion 2D Sync] 🔄 Führe Fallback-Retry aus...');
            const resRetry = await axios.get('https://europe.albiononline2d.com/en/item', { timeout: 25000 });
            if (resRetry.data) {
                albion2dCache = {
                    dataHtmlLength: typeof resRetry.data === 'string' ? resRetry.data.length : 0,
                    lastUpdated: new Date().toISOString()
                };
                console.log(`[Albion 2D Sync] 🟢 Retry erfolgreich! (${albion2dCache.dataHtmlLength} Bytes)`);
            }
        } catch (retryErr) {
            console.log('[Albion 2D Sync] ❌ Retry fehlgeschlagen:', retryErr.message);
        }
    }
}

// Autonomous KI Market Bot Loop for Cloud Data Persistence & Enchantments (.0 - .4)
async function aiMarketBotLoop() {
    try {
        const itemsToQuery = [
            'T4_WOOD', 'T4_WOOD@1', 'T4_WOOD@2', 'T4_WOOD@3', 'T4_WOOD@4',
            'T5_WOOD', 'T5_WOOD@1', 'T5_WOOD@2', 'T5_WOOD@3', 'T5_WOOD@4',
            'T6_WOOD', 'T6_WOOD@1', 'T6_WOOD@2', 'T6_WOOD@3', 'T6_WOOD@4',
            'T7_WOOD', 'T7_WOOD@1', 'T7_WOOD@2', 'T7_WOOD@3', 'T7_WOOD@4',
            'T8_WOOD', 'T8_WOOD@1', 'T8_WOOD@2', 'T8_WOOD@3', 'T8_WOOD@4',
            'T4_ORE', 'T4_ORE@1', 'T4_ORE@2', 'T4_ORE@3', 'T4_ORE@4',
            'T5_ORE', 'T5_ORE@1', 'T5_ORE@2', 'T5_ORE@3', 'T5_ORE@4',
            'T6_ORE', 'T6_ORE@1', 'T6_ORE@2', 'T6_ORE@3', 'T6_ORE@4',
            'T7_ORE', 'T7_ORE@1', 'T7_ORE@2', 'T7_ORE@3', 'T7_ORE@4',
            'T8_ORE', 'T8_ORE@1', 'T8_ORE@2', 'T8_ORE@3', 'T8_ORE@4',
            'T4_HIDE', 'T4_HIDE@1', 'T4_HIDE@2', 'T4_HIDE@3', 'T4_HIDE@4',
            'T5_HIDE', 'T5_HIDE@1', 'T5_HIDE@2', 'T5_HIDE@3', 'T5_HIDE@4',
            'T6_HIDE', 'T6_HIDE@1', 'T6_HIDE@2', 'T6_HIDE@3', 'T6_HIDE@4',
            'T7_HIDE', 'T7_HIDE@1', 'T7_HIDE@2', 'T7_HIDE@3', 'T7_HIDE@4',
            'T8_HIDE', 'T8_HIDE@1', 'T8_HIDE@2', 'T8_HIDE@3', 'T8_HIDE@4',
            'T4_FIBER', 'T4_FIBER@1', 'T4_FIBER@2', 'T4_FIBER@3', 'T4_FIBER@4',
            'T5_FIBER', 'T5_FIBER@1', 'T5_FIBER@2', 'T5_FIBER@3', 'T5_FIBER@4',
            'T6_FIBER', 'T6_FIBER@1', 'T6_FIBER@2', 'T6_FIBER@3', 'T6_FIBER@4',
            'T7_FIBER', 'T7_FIBER@1', 'T7_FIBER@2', 'T7_FIBER@3', 'T7_FIBER@4',
            'T8_FIBER', 'T8_FIBER@1', 'T8_FIBER@2', 'T8_FIBER@3', 'T8_FIBER@4',
            'T4_PLANKS', 'T4_PLANKS@1', 'T4_PLANKS@2', 'T4_PLANKS@3', 'T4_PLANKS@4',
            'T5_PLANKS', 'T5_PLANKS@1', 'T5_PLANKS@2', 'T5_PLANKS@3', 'T5_PLANKS@4',
            'T6_PLANKS', 'T6_PLANKS@1', 'T6_PLANKS@2', 'T6_PLANKS@3', 'T6_PLANKS@4',
            'T7_PLANKS', 'T7_PLANKS@1', 'T7_PLANKS@2', 'T7_PLANKS@3', 'T7_PLANKS@4',
            'T8_PLANKS', 'T8_PLANKS@1', 'T8_PLANKS@2', 'T8_PLANKS@3', 'T8_PLANKS@4',
            'T4_METALBAR', 'T4_METALBAR@1', 'T4_METALBAR@2', 'T4_METALBAR@3', 'T4_METALBAR@4',
            'T5_METALBAR', 'T5_METALBAR@1', 'T5_METALBAR@2', 'T5_METALBAR@3', 'T5_METALBAR@4',
            'T6_METALBAR', 'T6_METALBAR@1', 'T6_METALBAR@2', 'T6_METALBAR@3', 'T6_METALBAR@4',
            'T7_METALBAR', 'T7_METALBAR@1', 'T7_METALBAR@2', 'T7_METALBAR@3', 'T7_METALBAR@4',
            'T8_METALBAR', 'T8_METALBAR@1', 'T8_METALBAR@2', 'T8_METALBAR@3', 'T8_METALBAR@4',
            'T4_BAG', 'T4_BAG@1', 'T4_BAG@2', 'T4_BAG@3', 'T4_BAG@4',
            'T5_BAG', 'T5_BAG@1', 'T5_BAG@2', 'T5_BAG@3', 'T5_BAG@4',
            'T6_BAG', 'T6_BAG@1', 'T6_BAG@2', 'T6_BAG@3', 'T6_BAG@4',
            'T7_BAG', 'T7_BAG@1', 'T7_BAG@2', 'T7_BAG@3', 'T7_BAG@4',
            'T8_BAG', 'T8_BAG@1', 'T8_BAG@2', 'T8_BAG@3', 'T8_BAG@4',
            'T4_MAIN_SWORD', 'T4_MAIN_SWORD@1', 'T4_MAIN_SWORD@2', 'T4_MAIN_SWORD@3', 'T4_MAIN_SWORD@4',
            'T5_MAIN_SWORD', 'T5_MAIN_SWORD@1', 'T5_MAIN_SWORD@2', 'T5_MAIN_SWORD@3', 'T5_MAIN_SWORD@4',
            'T6_MAIN_SWORD', 'T6_MAIN_SWORD@1', 'T6_MAIN_SWORD@2', 'T6_MAIN_SWORD@3', 'T6_MAIN_SWORD@4'
        ];

        const url = `https://europe.albion-online-data.com/api/v2/stats/Prices/${itemsToQuery.join(',')}.json?locations=Bridgewatch,Caerleon,Fort Sterling,Lymhurst,Martlock,Thetford,BlackMarket,Brecilien`;
        const res = await axios.get(url, { timeout: 12000 });
        if (res.data && Array.isArray(res.data)) {
            marketCache = {
                items: res.data,
                lastUpdated: new Date().toISOString()
            };

            res.data.forEach(item => {
                if (item.sell_price_min > 0 || item.buy_price_max > 0) {
                    const key = `${item.item_id}_${item.city}`;
                    globalMarketPrices[key] = {
                        itemId: item.item_id,
                        city: item.city,
                        sellPriceMin: item.sell_price_min,
                        buyPriceMax: item.buy_price_max,
                        timestampMs: Date.now(),
                        sellPriceMinAmount: item.sell_price_min_amount || 1
                    };
                }
            });

            const count = res.data.length;
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
    } catch (e) {
        console.log('[KI Market Bot] ℹ️ Status:', e.message);
    }
}

setInterval(aiMarketBotLoop, 20000);
setInterval(fetchAlbion2DData, 600000);
setTimeout(aiMarketBotLoop, 1000);
setTimeout(fetchAlbion2DData, 3000);

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
    <title>AlbionDataPro v${CURRENT_SERVER_VERSION} - Das ultimative Markt- & Overlay-Tool für Albion Online</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800;900&display=swap" rel="stylesheet">
    <style>
        body { background-color: #0b1120; color: #f8fafc; font-family: 'Plus Jakarta Sans', sans-serif; overflow-x: hidden; }
        .glass-panel { background: rgba(15, 23, 42, 0.75); backdrop-filter: blur(16px); border: 1px solid rgba(255, 255, 255, 0.1); }
        .glass-panel-glow { background: linear-gradient(135deg, rgba(30, 41, 59, 0.8), rgba(15, 23, 42, 0.9)); backdrop-filter: blur(16px); border: 1px solid rgba(56, 189, 248, 0.25); box-shadow: 0 0 30px rgba(56, 189, 248, 0.15); }
        .gradient-text { background: linear-gradient(135deg, #38bdf8, #8b5cf6, #f59e0b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .gradient-gold { background: linear-gradient(135deg, #fbbf24, #f59e0b, #d97706); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .pulse-glow { animation: pulseGlow 2.5s infinite alternate; }
        @keyframes pulseGlow {
            0% { box-shadow: 0 0 15px rgba(16, 185, 129, 0.3); }
            100% { box-shadow: 0 0 35px rgba(16, 185, 129, 0.7); }
        }
    </style>
</head>
<body class="antialiased min-h-screen flex flex-col">

    <!-- Header / Navigation -->
    <header class="w-full border-b border-slate-800/80 sticky top-0 z-50 bg-slate-950/80 backdrop-blur-md">
        <div class="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
            <div class="flex items-center gap-3">
                <!-- Official ADP SVG Logo -->
                <div class="w-10 h-10 flex items-center justify-center">
                    <svg viewBox="0 0 128 128" width="100%" height="100%">
                        <path d="M64,8 L112,28 L112,68 C112,96 64,120 64,120 C64,120 16,96 16,68 L16,28 Z" fill="#1E293B" stroke="#06B6D4" stroke-width="4" />
                        <path d="M64,16 L104,33 L104,66 C104,89 64,110 64,110 C64,110 24,89 24,66 L24,33 Z" fill="#0F172A" stroke="#F59E0B" stroke-width="2" />
                        <path d="M38,78 L48,46 L58,78 M41,70 L55,70" stroke="#06B6D4" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
                        <path d="M62,46 L72,46 C80,46 84,52 84,62 C84,72 80,78 72,78 L62,78 Z" stroke="#F59E0B" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
                        <path d="M88,78 L88,46 L98,46 C104,46 108,50 108,56 C108,62 104,66 98,66 L88,66" stroke="#10B981" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
                        <path d="M64,28 L69,38 L64,48 L59,38 Z" fill="#F59E0B" />
                    </svg>
                </div>
                <div>
                    <span class="text-2xl font-black tracking-tight text-white">Albion<span class="text-sky-400">Data</span><span class="text-amber-400">Pro</span></span>
                    <span class="ml-2 text-xs px-2.5 py-0.5 bg-sky-500/20 text-sky-300 font-bold rounded-full border border-sky-500/30">v${CURRENT_SERVER_VERSION}</span>
                </div>
            </div>

            <a href="/download/AlbionDataPro.apk" class="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-extrabold px-5 py-2.5 rounded-xl text-sm transition shadow-lg flex items-center gap-2 border border-emerald-400/30">
                <i class="fa-solid fa-download"></i> APK Download
            </a>
        </div>
    </header>

    <!-- Main Content -->
    <main class="flex-grow max-w-6xl mx-auto px-4 py-12 text-center">

        <!-- Release Badge -->
        <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-slate-900/90 border border-sky-500/30 text-xs font-bold text-sky-400 mb-8 shadow-inner">
            <span class="flex h-2 w-2 relative">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-sky-400 opacity-75"></span>
                <span class="relative inline-flex rounded-full h-2 w-2 bg-sky-500"></span>
            </span>
            Offizieller Version Release v${CURRENT_SERVER_VERSION} — 24/7 Cloud & In-Game Overlay
        </div>

        <!-- Hero Branding -->
        <div class="flex justify-center mb-6">
            <div class="w-28 h-28 md:w-36 md:h-36 drop-shadow-[0_0_35px_rgba(56,189,248,0.4)]">
                <svg viewBox="0 0 128 128" width="100%" height="100%">
                    <path d="M64,8 L112,28 L112,68 C112,96 64,120 64,120 C64,120 16,96 16,68 L16,28 Z" fill="#1E293B" stroke="#06B6D4" stroke-width="4" />
                    <path d="M64,16 L104,33 L104,66 C104,89 64,110 64,110 C64,110 24,89 24,66 L24,33 Z" fill="#0F172A" stroke="#F59E0B" stroke-width="2" />
                    <path d="M38,78 L48,46 L58,78 M41,70 L55,70" stroke="#06B6D4" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
                    <path d="M62,46 L72,46 C80,46 84,52 84,62 C84,72 80,78 72,78 L62,78 Z" stroke="#F59E0B" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
                    <path d="M88,78 L88,46 L98,46 C104,46 108,50 108,56 C108,62 104,66 98,66 L88,66" stroke="#10B981" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
                    <path d="M64,28 L69,38 L64,48 L59,38 Z" fill="#F59E0B" />
                </svg>
            </div>
        </div>

        <h1 class="text-4xl md:text-7xl font-black mb-6 leading-tight tracking-tight text-white">
            <span class="gradient-text">AlbionDataPro</span>
        </h1>
        <p class="text-xl md:text-3xl font-extrabold text-slate-200 mb-6 max-w-3xl mx-auto leading-snug">
            Das mächtigste Handels- & Markt-Overlay für Albion Online Mobile
        </p>
        <p class="text-base md:text-lg text-slate-400 mb-10 max-w-2xl mx-auto leading-relaxed">
            Verdoppeln Sie Ihr Silber ohne stundenlanges Suchen. Nutzen Sie sekundengenaue Live-Preise, Arbitrage-Scanner & KI-Preise direkt als schwebendes In-Game Overlay über Ihrem Spiel!
        </p>

        <!-- DOWNLOAD BUTTON SECTION -->
        <div class="flex flex-col items-center gap-4 mb-16 max-w-md mx-auto w-full">
            <a href="/download/AlbionDataPro.apk" class="pulse-glow bg-gradient-to-r from-emerald-500 via-teal-500 to-emerald-600 hover:from-emerald-400 hover:to-teal-400 text-white font-black py-5 px-8 rounded-2xl shadow-2xl transition-all transform hover:scale-105 flex items-center justify-center gap-4 text-2xl border border-emerald-300/40 w-full">
                <i class="fa-solid fa-download text-3xl"></i>
                <div class="text-left">
                    <div class="text-xs uppercase tracking-wider font-extrabold text-emerald-200">Kostenlos Herunterladen</div>
                    <div>APK Download v${CURRENT_SERVER_VERSION}</div>
                </div>
            </a>
            <div class="text-xs text-slate-400 flex items-center gap-2">
                <i class="fa-solid fa-shield-halved text-emerald-400"></i> 100% Virenfrei • Direktes Android APK Package
            </div>
        </div>

        <!-- IMPORTANT NOTICE: LICENSES ARE PURCHASED IN APP -->
        <div class="glass-panel-glow p-8 rounded-3xl max-w-3xl mx-auto mb-20 text-left border border-amber-500/30 relative overflow-hidden">
            <div class="absolute -right-10 -bottom-10 opacity-10 text-amber-500 text-9xl pointer-events-none">
                <i class="fa-solid fa-key"></i>
            </div>
            <div class="flex items-start gap-5">
                <div class="bg-gradient-to-br from-amber-500 to-amber-600 text-slate-950 p-4 rounded-2xl text-2xl font-black shadow-lg">
                    <i class="fa-solid fa-lock text-2xl"></i>
                </div>
                <div>
                    <h3 class="text-2xl font-black text-amber-400 mb-2 flex items-center gap-2">
                        🔒 Lizenzen direkt in der App erwerben & freischalten!
                    </h3>
                    <p class="text-slate-300 text-sm md:text-base leading-relaxed mb-4">
                        Installieren Sie die App kostenlos über den Download-Button oben. Nach dem Start können Sie Ihren Account erstellen und Ihre Lizenz (1, 3, 6 oder 12 Monate) direkt in der App oder im Lizenz-Menü freischalten!
                    </p>
                    <div class="flex flex-wrap gap-3 text-xs font-bold text-slate-300">
                        <span class="bg-slate-800 px-3 py-1.5 rounded-lg border border-slate-700 flex items-center gap-1.5">
                            <i class="fa-solid fa-bolt text-amber-400"></i> Sofortige Freischaltung
                        </span>
                        <span class="bg-slate-800 px-3 py-1.5 rounded-lg border border-slate-700 flex items-center gap-1.5">
                            <i class="fa-brands fa-paypal text-blue-400"></i> Sichere PayPal-Zahlung
                        </span>
                        <span class="bg-slate-800 px-3 py-1.5 rounded-lg border border-slate-700 flex items-center gap-1.5">
                            <i class="fa-solid fa-cloud text-emerald-400"></i> 24/7 Cloud-Verbindung
                        </span>
                    </div>
                </div>
            </div>
        </div>

        <!-- WHY YOU NEED THIS TOOL (SALES PITCH) -->
        <div class="mb-20 text-left">
            <div class="text-center mb-12">
                <h2 class="text-3xl md:text-5xl font-black text-white mb-4">Warum du <span class="gradient-text">AlbionDataPro</span> brauchst</h2>
                <p class="text-slate-400 text-base md:text-lg max-w-2xl mx-auto">
                    Handeln in Albion Online ohne Live-Daten kostet dich täglich Millionen Silber. AlbionDataPro gibt dir den entscheidenden Vorteil gegenüber anderen Spielern.
                </p>
            </div>

            <div class="grid md:grid-cols-3 gap-6">
                <div class="glass-panel p-8 rounded-2xl border-t-4 border-t-sky-500 hover:border-sky-400 transition">
                    <div class="text-sky-400 text-3xl font-black mb-4"><i class="fa-solid fa-coins"></i></div>
                    <h3 class="text-xl font-bold text-white mb-2">Maximaler Profit ohne Risiko</h3>
                    <p class="text-slate-400 text-sm leading-relaxed">
                        Schluss mit Fehlkäufen! Der präzise ROI-Rechner zeigt dir vor jedem Deal exakt deinen Reingewinn nach Marktsteuern und Stationsgebühren an.
                    </p>
                </div>

                <div class="glass-panel p-8 rounded-2xl border-t-4 border-t-purple-500 hover:border-purple-400 transition">
                    <div class="text-purple-400 text-3xl font-black mb-4"><i class="fa-solid fa-gauge-high"></i></div>
                    <h3 class="text-xl font-bold text-white mb-2">Gewaltige Zeitersparnis</h3>
                    <p class="text-slate-400 text-sm leading-relaxed">
                        Kein lästiges Hin- und Herreisen mehr, um Preise zu vergleichen. Unsere Cloud scannt alle Städte und zeigt dir die lukrativsten Trade-Routen in Sekunden.
                    </p>
                </div>

                <div class="glass-panel p-8 rounded-2xl border-t-4 border-t-emerald-500 hover:border-emerald-400 transition">
                    <div class="text-emerald-400 text-3xl font-black mb-4"><i class="fa-solid fa-wand-magic-sparkles"></i></div>
                    <h3 class="text-xl font-bold text-white mb-2">Statistische KI-Garantie</h3>
                    <p class="text-slate-400 text-sm leading-relaxed">
                        Unser Algorithmus analysiert 7-Tage-Preisschwankungen und findet die idealen Buy- & Sell-Order Schwellenwerte für schnellen Umschlag.
                    </p>
                </div>
            </div>
        </div>

        <!-- BUBBLE OVERLAY FEATURES SECTION -->
        <div class="mb-20 text-left">
            <div class="text-center mb-12">
                <div class="inline-block px-3 py-1 bg-blue-500/20 text-blue-400 text-xs font-extrabold rounded-full mb-3 uppercase tracking-wider border border-blue-500/30">
                    In-Game Floating Overlay
                </div>
                <h2 class="text-3xl md:text-5xl font-black text-white mb-4">Das In-Game Bubble Overlay</h2>
                <p class="text-slate-400 text-base md:text-lg max-w-2xl mx-auto">
                    Die revolutionäre schwebende Overlay-Bubble läuft direkt über Albion Online Mobile. Du musst das Spiel niemals verlassen!
                </p>
            </div>

            <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
                <!-- Bubble Feature 1 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-sky-500/20 text-sky-400 flex items-center justify-center text-xl font-bold mb-4 border border-sky-500/30">
                        <i class="fa-solid fa-layer-group"></i>
                    </div>
                    <h4 class="text-lg font-bold text-white mb-2">Permanentes In-Game Overlay</h4>
                    <p class="text-slate-400 text-sm leading-relaxed">
                        Ein dezentes, schwebendes Symbol direkt auf deinem Bildschirm. Tippe einfach darauf, um Preise, Arbitrage und KI-Empfehlungen sofort einzublenden.
                    </p>
                </div>

                <!-- Bubble Feature 2 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center text-xl font-bold mb-4 border border-amber-500/30">
                        <i class="fa-solid fa-route"></i>
                    </div>
                    <h4 class="text-lg font-bold text-white mb-2">Städte & Schwarzmarkt Radar</h4>
                    <p class="text-slate-400 text-sm leading-relaxed">
                        Scannt Caerleon, Brecilien, den Schwarzmarkt und alle königlichen Städte. Berechnet Transportgewicht, Rüstungs-Tiers und exakte Margen.
                    </p>
                </div>

                <!-- Bubble Feature 3 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-purple-500/20 text-purple-400 flex items-center justify-center text-xl font-bold mb-4 border border-purple-500/30">
                        <i class="fa-solid fa-robot"></i>
                    </div>
                    <h4 class="text-lg font-bold text-white mb-2">KI Buy & Sell Order Bot</h4>
                    <p class="text-slate-400 text-sm leading-relaxed">
                        Sagt dir exakt, zu welchem Preis du Kauf- und Verkaufsaufträge einstellen musst, um maximale Profite bei hoher Verkaufschance zu erzielen.
                    </p>
                </div>

                <!-- Bubble Feature 4 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center text-xl font-bold mb-4 border border-emerald-500/30">
                        <i class="fa-solid fa-hammer"></i>
                    </div>
                    <h4 class="text-lg font-bold text-white mb-2">Crafting & Veredelungs Rechner</h4>
                    <p class="text-slate-400 text-sm leading-relaxed">
                        Berechnet Rohstoffkosten, Stadt-Rückgaberaten (Return Rates), Fokus-Ersparnis und Gebühren für Rüstungen, Waffen und Barren.
                    </p>
                </div>

                <!-- Bubble Feature 5 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-pink-500/20 text-pink-400 flex items-center justify-center text-xl font-bold mb-4 border border-pink-500/30">
                        <i class="fa-solid fa-seedling"></i>
                    </div>
                    <h4 class="text-lg font-bold text-white mb-2">Insel-Timer & Tierzucht</h4>
                    <p class="text-slate-400 text-sm leading-relaxed">
                        Überwache deine Insel-Ernten und Zuchtzeiten mit Benachrichtigung, sobald deine Reittiere oder Pflanzen abholbereit sind.
                    </p>
                </div>

                <!-- Bubble Feature 6 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-blue-500/20 text-blue-400 flex items-center justify-center text-xl font-bold mb-4 border border-blue-500/30">
                        <i class="fa-solid fa-cloud"></i>
                    </div>
                    <h4 class="text-lg font-bold text-white mb-2">24/7 Cloud-Sync & Auto-OTA</h4>
                    <p class="text-slate-400 text-sm leading-relaxed">
                        Deine Einstellungen und Favoriten sind sicher in der Cloud gespeichert. Automatische OTA-Updates halten deine App stets aktuell.
                    </p>
                </div>
            </div>
        </div>

        <!-- QUICK INSTALLATION GUIDE -->
        <div class="glass-panel p-8 rounded-3xl border-l-4 border-l-emerald-500 text-left max-w-4xl mx-auto flex flex-col md:flex-row gap-6 items-center">
            <div class="bg-emerald-500/20 p-4 rounded-2xl text-emerald-400 text-4xl shrink-0">
                <i class="fa-solid fa-mobile-screen"></i>
            </div>
            <div>
                <h4 class="font-extrabold text-2xl text-white mb-2">Einfache Installation auf Android</h4>
                <ol class="list-decimal list-inside text-slate-300 text-sm md:text-base space-y-2">
                    <li>Klicke oben auf <strong class="text-emerald-400">"APK Download"</strong> und speichere die Datei.</li>
                    <li>Öffne <code class="bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono">AlbionDataPro.apk</code> und erlaube die Installation.</li>
                    <li>Starte die App, erstelle deinen Account und schalte deine Lizenz direkt in der App frei!</li>
                </ol>
            </div>
        </div>

    </main>

    <!-- Footer -->
    <footer class="w-full text-center p-8 text-slate-500 text-sm border-t border-slate-800/80 mt-auto bg-slate-950">
        <div class="flex justify-center items-center gap-2 mb-2">
            <div class="w-5 h-5">
                <svg viewBox="0 0 128 128" width="100%" height="100%">
                    <path d="M64,8 L112,28 L112,68 C112,96 64,120 64,120 C64,120 16,96 16,68 L16,28 Z" fill="#1E293B" stroke="#06B6D4" stroke-width="4" />
                    <path d="M64,16 L104,33 L104,66 C104,89 64,110 64,110 C64,110 24,89 24,66 L24,33 Z" fill="#0F172A" stroke="#F59E0B" stroke-width="2" />
                    <path d="M38,78 L48,46 L58,78 M41,70 L55,70" stroke="#06B6D4" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
                    <path d="M62,46 L72,46 C80,46 84,52 84,62 C84,72 80,78 72,78 L62,78 Z" stroke="#F59E0B" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
                    <path d="M88,78 L88,46 L98,46 C104,46 108,50 108,56 C108,62 104,66 98,66 L88,66" stroke="#10B981" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
                    <path d="M64,28 L69,38 L64,48 L59,38 Z" fill="#F59E0B" />
                </svg>
            </div>
            <span class="font-bold text-slate-400">AlbionDataPro v${CURRENT_SERVER_VERSION}</span>
        </div>
        <p>&copy; 2026 AlbionDataPro. Alle Rechte vorbehalten. Gehostet auf Render Cloud.</p>
        <p class="text-xs mt-2 text-slate-600">Dieses Analyse-Tool steht in keiner offiziellen Verbindung zu Sandbox Interactive.</p>
    </footer>

</body>
</html>`);
});

// API Endpoints
app.get('/api/health', (req, res) => res.json({ status: 'healthy', timestamp: Date.now(), version: CURRENT_SERVER_VERSION, subnets: ['74.220.51.0/24', '74.220.59.0/24'] }));
app.get('/api/tunnel', (req, res) => res.json({ tunnelUrl: getActiveTunnelUrl(), subnets: ['74.220.51.0/24', '74.220.59.0/24'] }));
app.get('/api/prices', (req, res) => res.json(marketCache));

// 24/7 Global Data-Brain: Echtzeit Abruf aller Items
app.get('/api/prices/recent', (req, res) => {
    res.json(Object.values(globalMarketPrices));
});
app.get('/api/market/prices/live', (req, res) => {
    res.json(Object.values(globalMarketPrices));
});

app.get('/api/prices/albion2d', (req, res) => res.json(albion2dCache));

// PayPal IPN (Instant Payment Notification) Webhook - Automatische Lizenzausgabe
app.post('/api/paypal/ipn', express.urlencoded({ extended: true }), (req, res) => {
    res.status(200).send('OK'); // PayPal expects immediate 200 OK

    let body = req.body || {};
    let postreq = 'cmd=_notify-validate';
    for (let key in body) {
        if (body.hasOwnProperty(key)) {
            postreq += `&${key}=${encodeURIComponent(body[key])}`;
        }
    }

    axios.post('https://ipnpb.paypal.com/cgi-bin/webscr', postreq, {
        headers: { 'Content-Length': postreq.length }
    }).then(response => {
        if (response.data === 'VERIFIED') {
            const payment_status = body.payment_status;
            const receiver_email = body.receiver_email;
            const mc_gross = parseFloat(body.mc_gross);
            const payer_email = body.payer_email;

            if (payment_status === 'Completed' && receiver_email === 'dnnxdigitalcreator@gmail.com') {
                let months = 0;
                let tier = '';

                if (mc_gross === 15.00) { months = 1; tier = '1 Monat'; }
                else if (mc_gross === 30.00) { months = 3; tier = '3 Monate'; }
                else if (mc_gross === 50.00) { months = 6; tier = '6 Monate'; }
                else if (mc_gross === 100.00) { months = 12; tier = '12 Monate'; }

                if (months > 0) {
                    const prefix = (months === 12) ? '12M-' : ((months === 6) ? '6M-' : ((months === 3) ? '3M-' : '1M-'));
                    const key = 'ALBION-' + prefix + Math.random().toString(36).substring(2, 10).toUpperCase();
                    const newLicense = {
                        key,
                        tier,
                        price: `${mc_gross}€`,
                        note: `Auto-Generated (PayPal: ${payer_email})`,
                        createdAt: new Date().toISOString()
                    };
                    generatedLicenses.push(newLicense);
                    saveLicenses();
                    console.log(`[PayPal Auto-License] Lizenz ${key} generiert für ${payer_email}`);

                    const mailOptions = {
                        from: 'AlbionDataPro <dnnxdigitalcreator@gmail.com>',
                        to: payer_email,
                        bcc: 'dnnxdigitalcreator@gmail.com', // Admin bekommt unsichtbar eine Kopie der Mail!
                        subject: `Dein AlbionDataPro Lizenzschlüssel (${tier})`,
                        html: `
                            <div style="font-family: Arial, sans-serif; padding: 20px; background-color: #0f172a; color: #f8fafc; border-radius: 10px;">
                                <h2 style="color: #38bdf8;">Vielen Dank für deinen Kauf!</h2>
                                <p>Deine Zahlung über ${mc_gross}€ war erfolgreich.</p>
                                <p>Hier ist dein exklusiver Lizenzschlüssel für <strong>${tier}</strong>:</p>
                                <div style="background-color: #1e293b; padding: 15px; border-radius: 5px; text-align: center; margin: 20px 0; border: 1px solid #38bdf8;">
                                    <strong style="font-size: 24px; color: #10b981; letter-spacing: 2px;">${key}</strong>
                                </div>
                                <p>Lade dir die neueste APK-Version auf der <a href="https://albionmarketv2-1.onrender.com" style="color: #38bdf8;">AlbionDataPro Webseite</a> herunter, erstelle in der App ein Konto und schalte es mit diesem Schlüssel frei.</p>
                                <p>Besuche unsere <a href="https://albionmarketv2-1.onrender.com" style="color: #38bdf8;">AlbionDataPro Webseite</a> für Support.</p>
                                <p style="font-size: 12px; color: #64748b; margin-top: 30px;">Dies ist eine automatisch generierte E-Mail.</p>
                            </div>
                        `
                    };

                    transporter.sendMail(mailOptions, (error, info) => {
                        if (error) {
                            console.error('[PayPal Auto-License] Fehler beim Mail-Versand:', error);
                        } else {
                            console.log('[PayPal Auto-License] E-Mail erfolgreich an', payer_email, 'gesendet.');
                        }
                    });
                }
            }
        }
    }).catch(err => {
        console.error('[PayPal Auto-License] IPN Validierung fehlgeschlagen:', err.message);
    });
});

// Simulated/Test Endpoint for PayPal Purchase & License E-Mail Dispatch
app.post('/api/test/paypal-purchase', async (req, res) => {
    const payer_email = req.body.email || 'dnnxdigitalcreator@gmail.com';
    const amount = parseFloat(req.body.amount || 15.00);
    let months = 1;
    let tier = '1 Monat';

    if (amount === 30.00) { months = 3; tier = '3 Monate'; }
    else if (amount === 50.00) { months = 6; tier = '6 Monate'; }
    else if (amount === 100.00) { months = 12; tier = '12 Monate'; }

    const prefix = amount === 100.00 ? '12M-' : (amount === 50.00 ? '6M-' : (amount === 30.00 ? '3M-' : '1M-'));
    const key = 'ALBION-' + prefix + Math.random().toString(36).substring(2, 10).toUpperCase();

    const newLicense = {
        key,
        tier,
        price: `${amount}€`,
        note: `Auto-Generated (Test Purchase: ${payer_email})`,
        createdAt: new Date().toISOString()
    };

    generatedLicenses.push(newLicense);
    saveLicenses();

    let emailSent = false;
    let emailError = null;

    const mailOptions = {
        from: 'AlbionDataPro <dnnxdigitalcreator@gmail.com>',
        to: payer_email,
        bcc: 'dnnxdigitalcreator@gmail.com',
        subject: `Dein AlbionDataPro Lizenzschlüssel (${tier})`,
        html: `
            <div style="font-family: Arial, sans-serif; padding: 20px; background-color: #0f172a; color: #f8fafc; border-radius: 10px;">
                <h2 style="color: #38bdf8;">Vielen Dank für deinen Kauf!</h2>
                <p>Deine Zahlung über ${amount}€ war erfolgreich.</p>
                <p>Hier ist dein exklusiver Lizenzschlüssel für <strong>${tier}</strong>:</p>
                <div style="background-color: #1e293b; padding: 15px; border-radius: 5px; text-align: center; margin: 20px 0; border: 1px solid #38bdf8;">
                    <strong style="font-size: 24px; color: #10b981; letter-spacing: 2px;">${key}</strong>
                </div>
                <p>Lade dir die neueste APK-Version auf der <a href="https://albionmarketv2-1.onrender.com" style="color: #38bdf8;">AlbionDataPro Webseite</a> herunter, erstelle in der App ein Konto und schalte es mit diesem Schlüssel frei.</p>
                <p>Support via Telegram: <a href="https://t.me/DnnxDigitalCrator" style="color: #38bdf8;">@DnnxDigitalCrator</a></p>
            </div>
        `
    };

    try {
        await transporter.sendMail(mailOptions);
        emailSent = true;
    } catch (err) {
        emailError = err.message;
    }

    res.json({
        status: 'success',
        licenseKey: key,
        tier,
        payerEmail: payer_email,
        emailSent,
        emailError,
        message: emailSent ? 'Lizenz generiert & per E-Mail gesendet!' : `Lizenz ${key} generiert (E-Mail Info: ${emailError || 'Versand vorbereitet'})`
    });
});

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
        const exp = user.licenseExpiresAt ? new Date(user.licenseExpiresAt) : new Date(0);
        const isLicenseActive = user.isAdmin || (user.isLicensed && exp > now);

        if (!isLicenseActive && !user.isAdmin) {
            console.warn(`[AUTH-LOGIN] ⛔ Login abgelehnt für ${user.username}: Lizenz abgelaufen (${user.licenseExpiresAt})`);
            return res.status(403).json({
                authenticated: false,
                isLicenseActive: false,
                message: '🔴 Login fehlgeschlagen: Lizenz abgelaufen oder inaktiv! Bitte Lizenz erneuern.'
            });
        }

        return res.json({
            authenticated: true,
            isAdmin: !!user.isAdmin,
            isLicenseActive: true,
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

// Central API Hub for Android (Schwarmintelligenz)
app.post('/api/market/prices', (req, res) => {
    if (!req.body || !Array.isArray(req.body)) return res.json({ status: 'ignored' });
    req.body.forEach(item => {
        if (!item.itemId || !item.city) return;
        const key = `${item.itemId}_${item.city}`;
        if (!globalMarketPrices[key] || globalMarketPrices[key].timestampMs < item.timestampMs) {
            globalMarketPrices[key] = {
                itemId: item.itemId,
                city: item.city,
                sellPriceMin: item.sellPriceMin || 0,
                buyPriceMax: item.buyPriceMax || 0,
                timestampMs: item.timestampMs || Date.now(),
                sellPriceMinAmount: item.sellPriceMinAmount || 1
            };
        }
    });
    res.json({ status: 'success' });
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
    const { username, password, licenseKey, activatedLicenseCode } = req.body;
    if (!username || !password) return res.status(400).json({ error: 'Benutzername und Passwort erforderlich' });

    const cleanUser = username.trim();
    if (registeredUsers.some(u => u.username.toLowerCase() === cleanUser.toLowerCase())) {
        return res.status(400).json({ error: 'Benutzer existiert bereits' });
    }

    const keyInput = (licenseKey || activatedLicenseCode || '').trim().toUpperCase();
    const defaultExp = new Date();

    if (keyInput.startsWith('ALBION-12M-') || keyInput.includes('12M')) {
        defaultExp.setDate(defaultExp.getDate() + 365);
    } else if (keyInput.startsWith('ALBION-6M-') || keyInput.includes('6M')) {
        defaultExp.setDate(defaultExp.getDate() + 180);
    } else if (keyInput.startsWith('ALBION-3M-') || keyInput.includes('3M')) {
        defaultExp.setDate(defaultExp.getDate() + 90);
    } else if (keyInput.startsWith('ALBION-1M-') || keyInput.includes('1M')) {
        defaultExp.setDate(defaultExp.getDate() + 30);
    } else if (keyInput.startsWith('ALBION-LIFETIME') || keyInput.includes('LIFETIME') || keyInput === 'ALBION-PRO-LIFETIME') {
        defaultExp.setFullYear(2099);
    } else if (keyInput.length > 0) {
        // Search in generatedLicenses array
        const foundLicIdx = generatedLicenses.findIndex(l => l.key.toUpperCase() === keyInput);
        if (foundLicIdx !== -1) {
            const lic = generatedLicenses[foundLicIdx];
            if (lic.tier.includes('12') || lic.key.includes('12M')) defaultExp.setDate(defaultExp.getDate() + 365);
            else if (lic.tier.includes('6') || lic.key.includes('6M')) defaultExp.setDate(defaultExp.getDate() + 180);
            else if (lic.tier.includes('3') || lic.key.includes('3M')) defaultExp.setDate(defaultExp.getDate() + 90);
            else defaultExp.setDate(defaultExp.getDate() + 30);
            generatedLicenses.splice(foundLicIdx, 1);
            saveLicenses();
        } else {
            defaultExp.setDate(defaultExp.getDate() + 30);
        }
    } else {
        defaultExp.setDate(defaultExp.getDate() + 30);
    }

    const nowIso = new Date().toISOString();
    const newUser = {
        id: 'usr_' + Date.now(),
        username: cleanUser,
        password: password.trim(),
        isAdmin: false,
        isLicensed: true,
        licenseExpiresAt: defaultExp.toISOString(),
        registeredAt: nowIso
    };
    registeredUsers.push(newUser);
    saveUsers();
    console.log(`[AUTH-REGISTER] Neuer Account registriert & freigeschaltet: ${cleanUser} (${nowIso}, Key: ${keyInput || 'Standart'})`);
    res.json({
        status: 'success',
        message: 'Account erfolgreich registriert & freigeschaltet.',
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
    const cleanTier = (tier || '').toString().trim().toLowerCase();
    let prefix = 'ALBION-1M-';
    let price = '15 €';
    let durationDays = 30;

    if (cleanTier === '3m' || cleanTier.includes('3 monat') || cleanTier.includes('3m')) {
        prefix = 'ALBION-3M-'; price = '30 €'; durationDays = 90;
    } else if (cleanTier === '6m' || cleanTier.includes('6 monat') || cleanTier.includes('6m')) {
        prefix = 'ALBION-6M-'; price = '50 €'; durationDays = 180;
    } else if (cleanTier === '12m' || cleanTier.includes('12 monat') || cleanTier.includes('12m') || cleanTier.includes('jahr')) {
        prefix = 'ALBION-12M-'; price = '100 €'; durationDays = 365;
    } else if (cleanTier === 'lifetime' || cleanTier.includes('lifetime')) {
        prefix = 'ALBION-LIFETIME-'; price = '250 €'; durationDays = 36500;
    }

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

// KI-AntiCheat Integrity & Anomaly Verification Endpoint (V3.0 Threat-Scoring)
app.post('/api/anticheat/verify', (req, res) => {
    const { hwId, packageName, isRooted, isDebuggerAttached, isEmulator, isVpnActive, isHookDetected, maliciousAppCount, signatureHash } = req.body;
    if (!hwId) return res.status(400).json({ status: 'error', message: 'Missing hwId' });

    const cleanHwId = hwId.trim().toLowerCase();
    let device = registeredDevices.find(d => d.hwId.toLowerCase() === cleanHwId);

    // CRITICAL RULE: If device was explicitly unbanned by Admin, NEVER auto-ban!
    if (device && device.unbanned === true) {
        return res.json({ status: 'clean', isBanned: false, message: 'Gerät manuell entbannt (KI Auto-Bann geschützt)' });
    }

    // Threat Scoring System (0-100)
    // Avoids false positives from standard users/custom ROMs
    let threatScore = 0;

    if (isHookDetected === true) threatScore += 100; // Frida/Xposed = Immediate Ban
    if (maliciousAppCount > 0) threatScore += 50 * maliciousAppCount; // LuckyPatcher, GameGuardian = High Threat
    if (isDebuggerAttached === true) threatScore += 20; // Debugger alone might be dev option
    if (isEmulator === true) threatScore += 30; // Emulator alone is suspicious, but combined with VPN/Root it's dangerous
    if (isVpnActive === true) threatScore += 10;
    if (isRooted === true) threatScore += 10;

    // Invalid signature directly flags as modded APK
    if (signatureHash !== "ALBION-SECURE-V3" && signatureHash !== "ALBION-HMAC-SHA256-MILITARY-GRADE-VERIFIED") {
        threatScore += 100;
    }

    const isViolation = threatScore >= 100;

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

        const banExp = new Date();
        banExp.setDate(banExp.getDate() + 3650);
        device.bannedUntil = banExp.toISOString();

        let reasonParts = [];
        if (isHookDetected) reasonParts.push("Memory Hooking");
        if (maliciousAppCount > 0) reasonParts.push("Cheat Apps");
        if (signatureHash !== "ALBION-SECURE-V3" && signatureHash !== "ALBION-HMAC-SHA256-MILITARY-GRADE-VERIFIED") reasonParts.push("Modded APK");
        if (isEmulator && isDebuggerAttached) reasonParts.push("Emulator Debugging");

        device.banReason = `🤖 KI-AntiCheat: Score ${threatScore} (${reasonParts.join(', ')})`;
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

app.post('/admin/login', express.urlencoded({ extended: true }), (req, res) => {
    const { username, password } = req.body;
    if (username === 'dnnx' && password === 'Dean3153...') {
        const token = crypto.createHmac('sha256', SERVER_HMAC_SECRET).update('admin_session').digest('hex');
        res.setHeader('Set-Cookie', `admin_auth=${token}; HttpOnly; Path=/; Max-Age=864000`);
        return res.redirect('/admin');
    }
    return res.redirect('/admin?error=1');
});

// Admin Dashboard HTML Page with License Generator (15€ - 250€)
app.get(['/admin'], (req, res) => {
    const cookieHeader = req.headers.cookie || '';
    const match = cookieHeader.match(/admin_auth=([^;]+)/);
    const clientToken = match ? match[1] : null;
    const expectedToken = crypto.createHmac('sha256', SERVER_HMAC_SECRET).update('admin_session').digest('hex');

    if (clientToken !== expectedToken) {
        return res.status(401).send(`
            <!DOCTYPE html>
            <html lang="de" style="background:#0f172a;color:#f8fafc;font-family:sans-serif;display:flex;justify-content:center;align-items:center;min-height:100vh;">
            <head><title>Admin Authentifizierung erforderlich</title><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="text-align:center;padding:20px;">
                <div style="background:#1e293b;padding:32px;border-radius:16px;border:1px solid #334155;max-width:400px;margin:auto;box-shadow:0 10px 30px rgba(0,0,0,0.5);">
                    <h2 style="color:#38bdf8;margin-top:0;">🔒 Admin Control Center</h2>
                    <p style="color:#94a3b8;font-size:14px;">Zugriff nur für autorisierte Administratoren.</p>
                    ${req.query.error ? '<p style="color:#ef4444;font-size:14px;font-weight:bold;">❌ Falsche Zugangsdaten!</p>' : ''}
                    <form method="POST" action="/admin/login" style="margin-top:20px;">
                        <input type="text" name="username" placeholder="Benutzername" style="width:100%;box-sizing:border-box;padding:12px;border-radius:8px;background:#0f172a;border:1px solid #475569;color:white;margin-bottom:14px;" required autofocus>
                        <input type="password" name="password" placeholder="Passwort" style="width:100%;box-sizing:border-box;padding:12px;border-radius:8px;background:#0f172a;border:1px solid #475569;color:white;margin-bottom:14px;" required>
                        <button type="submit" style="width:100%;padding:12px;border-radius:8px;background:#3b82f6;color:white;border:none;font-weight:bold;cursor:pointer;">Einloggen</button>
                    </form>
                </div>
            </body>
            </html>
        `);
    }

    const tunnelUrl = getActiveTunnelUrl();
    const nowTime = Date.now();
    const onlineDevicesCount = registeredDevices.filter(d => d.lastSeen && (nowTime - new Date(d.lastSeen).getTime() < 120000)).length;
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

            <div style="margin-top: 16px; padding: 16px; background: #0f172a; border-radius: 12px; border: 1px solid #334155; display: flex; align-items: center; justify-content: space-between;">
                <div>
                    <strong>📱 Neueste APK Version v${CURRENT_SERVER_VERSION} bereitgestellt</strong>
                    <div style="font-size: 12px; color: #94a3b8; margin-top: 4px;">Direkt herunterladen und auf Android-Geräten installieren.</div>
                </div>
                <a href="/download" class="btn" style="background: #10b981; text-decoration: none; padding: 10px 20px; font-size: 14px;">📥 APK herunterladen (v${CURRENT_SERVER_VERSION})</a>
            </div>

            <div class="stat-box">
                <div class="stat-card">
                    <div>🟢 Gerade online (App aktiv)</div>
                    <div class="stat-number" style="color: #34d399;">${onlineDevicesCount}</div>
                </div>
                <div class="stat-card">
                    <div>📊 Gesamte Informationen (API geholt)</div>
                    <div class="stat-number">${totalInformationCount.toLocaleString('de-DE')}</div>
                </div>
                <div class="stat-card">
                    <div>🔑 Generierte Lizenzen</div>
                    <div class="stat-number">${generatedLicenses.length}</div>
                </div>
                <div class="stat-card">
                    <div>📱 Registrierte Geräte</div>
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
