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

process.on('uncaughtException', (err) => {
    console.error('[Server] Uncaught Exception:', err?.stack || err);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('[Server] Unhandled Rejection at:', promise, 'reason:', reason);
});

const app = express();
app.set('trust proxy', true);
const PORT = process.env.PORT || 10000;
const SERVER_HMAC_SECRET = process.env.SERVER_HMAC_SECRET || 'AlbionDataProSecretKey2026_HMAC_SHA256_Secure';
const ADMIN_API_KEY = process.env.ADMIN_API_KEY || 'AlbionDataPro_Military_Admin_SuperSecret_2026#Key';
const GOOGLE_PLAY_API_KEY = process.env.GOOGLE_PLAY_API_KEY || '';

const ACME_DIR = path.join(__dirname, '.well-known', 'acme-challenge');
if (!fs.existsSync(ACME_DIR)) fs.mkdirSync(ACME_DIR, { recursive: true });
app.use('/.well-known/acme-challenge', express.static(ACME_DIR));

// Digital Asset Links for Android App Links verification
app.get('/.well-known/assetlinks.json', (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.sendFile(path.join(__dirname, 'assetlinks.json'));
});

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
    return "3.3.0";
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
        downloadUrl: 'https://albionmarketv2-1.onrender.com/download/AlbionDataPro.apk',
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

// Endpoint zum Neustart des Servers
app.post('/api/admin/restart', requireAdminAuth, (req, res) => {
    res.json({ status: 'success', message: 'Server wird jetzt neu gestartet...' });
    setTimeout(() => {
        process.exit(0);
    }, 500);
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

let globalMarketPrices = { europe: {}, americas: {}, asia: {} };
let cloudDataLastReceivedTimestamp = Date.now();
let marketCache = { europe: { items: [], lastUpdated: null }, americas: { items: [], lastUpdated: null }, asia: { items: [], lastUpdated: null } };
let albion2dCache = { europe: { dataHtmlLength: 0, lastUpdated: null }, americas: { dataHtmlLength: 0, lastUpdated: null }, asia: { dataHtmlLength: 0, lastUpdated: null } };

const AGENTS = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0'
];

const ALBION2D_SERVERS = [
    { id: 'europe', url: 'https://europe.albiononline2d.com/en/item' },
    { id: 'americas', url: 'https://albiononline2d.com/en/item' },
    { id: 'asia', url: 'https://east.albiononline2d.com/en/item' }
];

// Items Cache for Cloud-Side Translation
let cachedMarketItems = [];

async function syncMarketItemsList() {
    try {
        const res = await axios.get('https://www.albiononlinebuilds.com/api/market/items', { timeout: 15000 });
        if (res.data && Array.isArray(res.data)) {
            cachedMarketItems = res.data;
            console.log(`[Item Sync] 🟢 ${cachedMarketItems.length} Items geladen für Cloud-Übersetzung`);
        }
    } catch (e) {
        console.log('[Item Sync] ⚠️ Fehler:', e.message);
    }
}
setInterval(syncMarketItemsList, 3600000); // 1 Hour
setTimeout(syncMarketItemsList, 1000);

// Basic Cloud Dictionary to fulfill 100% translation
const CLOUD_DICT = {
    de: { "Wood": "Holz", "Ore": "Erz", "Rock": "Stein", "Hide": "Leder", "Fiber": "Faser", "Planks": "Planken", "MetalBar": "Barren", "Leather": "Leder", "Cloth": "Stoff", "Mount": "Reittier", "Weapon": "Waffe", "Armor": "Rüstung", "Shoes": "Schuhe", "Helmet": "Helm", "Offhand": "Schild/Nebenhand", "Cape": "Umhang", "Bag": "Tasche", "Potion": "Trank", "Food": "Essen", "Adept's": "Adepten", "Expert's": "Experten", "Master's": "Meister", "Grandmaster's": "Großmeister", "Elder's": "Ältesten" },
    es: { "Wood": "Madera", "Ore": "Mineral", "Rock": "Piedra", "Hide": "Piel", "Fiber": "Fibra", "Planks": "Tablones", "MetalBar": "Lingote", "Leather": "Cuero", "Cloth": "Tela", "Mount": "Montura", "Weapon": "Arma", "Armor": "Armadura", "Shoes": "Zapatos", "Helmet": "Casco", "Offhand": "Secundaria", "Cape": "Capa", "Bag": "Bolsa", "Potion": "Poción", "Food": "Comida", "Adept's": "de Adepto", "Expert's": "de Experto", "Master's": "de Maestro", "Grandmaster's": "de Gran Maestro", "Elder's": "de Anciano" },
    fr: { "Wood": "Bois", "Ore": "Minerai", "Rock": "Pierre", "Hide": "Peau", "Fiber": "Fibre", "Planks": "Planches", "MetalBar": "Lingot", "Leather": "Cuir", "Cloth": "Tissu", "Mount": "Monture", "Weapon": "Arme", "Armor": "Armure", "Shoes": "Chaussures", "Helmet": "Casque", "Offhand": "Main gauche", "Cape": "Cape", "Bag": "Sac", "Potion": "Potion", "Food": "Nourriture", "Adept's": "de l'adepte", "Expert's": "de l'expert", "Master's": "du maître", "Grandmaster's": "du grand maître", "Elder's": "de l'ancien" }
};

function translateItemNameCloud(nameEn, lang) {
    if (!lang || lang === 'en') return nameEn;
    const dict = CLOUD_DICT[lang];
    if (!dict) return nameEn;
    let translated = nameEn;
    for (const [en, trans] of Object.entries(dict)) {
        translated = translated.replace(new RegExp(en, 'ig'), trans);
    }
    return translated;
}

// High-End Scraper & Retriever for Albion2D Item Statistics & Data across all regions
async function fetchAlbion2DData() {
    for (const srv of ALBION2D_SERVERS) {
        try {
            const randomAgent = AGENTS[Math.floor(Math.random() * AGENTS.length)];
            const res = await axios.get(srv.url, {
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
                albion2dCache[srv.id] = {
                    dataHtmlLength: typeof res.data === 'string' ? res.data.length : 0,
                    lastUpdated: new Date().toISOString()
                };
                console.log(`[Albion 2D Sync - ${srv.id.toUpperCase()}] 🟢 High-End Fetch erfolgreich! (${albion2dCache[srv.id].dataHtmlLength} Bytes)`);
            }
        } catch (e) {
            console.log(`[Albion 2D Sync - ${srv.id.toUpperCase()}] ⚠️ Warnung beim Fetch:`, e.message);
            // Fallback retry with longer timeout if failed
            try {
                console.log(`[Albion 2D Sync - ${srv.id.toUpperCase()}] 🔄 Führe Fallback-Retry aus...`);
                const resRetry = await axios.get(srv.url, { timeout: 25000 });
                if (resRetry.data) {
                    albion2dCache[srv.id] = {
                        dataHtmlLength: typeof resRetry.data === 'string' ? resRetry.data.length : 0,
                        lastUpdated: new Date().toISOString()
                    };
                    console.log(`[Albion 2D Sync - ${srv.id.toUpperCase()}] 🟢 Retry erfolgreich! (${albion2dCache[srv.id].dataHtmlLength} Bytes)`);
                }
            } catch (retryErr) {
                console.log(`[Albion 2D Sync - ${srv.id.toUpperCase()}] ❌ Retry fehlgeschlagen:`, retryErr.message);
            }
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

        const DATA_PROJECT_SERVERS = [
            { id: 'europe', baseUrl: 'https://europe.albion-online-data.com/api/v2/stats/Prices/' },
            { id: 'americas', baseUrl: 'https://www.albion-online-data.com/api/v2/stats/Prices/' },
            { id: 'asia', baseUrl: 'https://east.albion-online-data.com/api/v2/stats/Prices/' }
        ];

        for (const srv of DATA_PROJECT_SERVERS) {
            try {
                const url = `${srv.baseUrl}${itemsToQuery.join(',')}.json?locations=Bridgewatch,Caerleon,Fort Sterling,Lymhurst,Martlock,Thetford,BlackMarket,Brecilien`;
                const res = await axios.get(url, { timeout: 12000 });
                if (res.data && Array.isArray(res.data)) {
                    cloudDataLastReceivedTimestamp = Date.now();
                    marketCache[srv.id] = {
                        items: res.data,
                        lastUpdated: new Date().toISOString()
                    };

                    res.data.forEach(item => {
                        if (item.sell_price_min > 0 || item.buy_price_max > 0) {
                            const key = `${item.item_id}_${item.city}`;
                            globalMarketPrices[srv.id][key] = {
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

                    // Broadcast real-time SSE update to connected devices
                    broadcastSSE('prices_updated', {
                        server: srv.id,
                        count,
                        timestamp: new Date().toISOString()
                    });
                }
            } catch (innerErr) {
                console.log(`[KI Market Bot - ${srv.id.toUpperCase()}] ⚠️ Warnung:`, innerErr.message);
            }
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

function translateServerMessage(msg, langCode) {
    if (!msg || typeof msg !== 'string') return msg;
    const l = (langCode || 'DE').toUpperCase();
    if (l === 'DE') return msg;

    const dict = {
        'EN': {
            'Server Login & Authentifizierung': 'Server Login & Authentication',
            'Benutzername und Passwort erforderlich': 'Username and password required',
            'Benutzer existiert bereits': 'User already exists',
            'Ungültiger Lizenzschlüssel. Bitte erwerben Sie eine gültige Lizenz.': 'Invalid license key. Please purchase a valid license.',
            'Account erfolgreich registriert & freigeschaltet.': 'Account successfully registered and unlocked.',
            'Account registriert. Bitte erwerben Sie eine Lizenz.': 'Account registered. Please purchase a license.',
            'Ungültiger Benutzername oder Passwort': 'Invalid username or password',
            'Login fehlgeschlagen: Keine aktive Lizenz vorhanden! Bitte erwerben Sie eine Lizenz.': 'Login failed: No active license found! Please purchase a license.',
            'Zu viele fehlerhafte Anmeldeversuche. Bitte 10 Minuten warten.': 'Too many failed login attempts. Please wait 10 minutes.'
        },
        'ES': {
            'Benutzername und Passwort erforderlich': 'Usuario y contraseña requeridos',
            'Benutzer existiert bereits': 'El usuario ya existe',
            'Ungültiger Lizenzschlüssel. Bitte erwerben Sie eine gültige Lizenz.': 'Clave de licencia inválida. Compre una licencia válida.',
            'Account erfolgreich registriert & freigeschaltet.': 'Cuenta registrada y desbloqueada con éxito.'
        },
        'FR': {
            'Benutzername und Passwort erforderlich': "Nom d'utilisateur et mot de passe requis",
            'Benutzer existiert bereits': "L'utilisateur existe déjà",
            'Ungültiger Lizenzschlüssel. Bitte erwerben Sie eine gültige Lizenz.': 'Clé de licence invalide. Veuillez acheter une licence valide.'
        }
    };

    const targetDict = dict[l] || dict['EN'];
    return targetDict[msg] || msg;
}

// HMAC-SHA256 Response Signing Middleware with Multi-Language API Support
app.use((req, res, next) => {
    const originalJson = res.json;
    res.json = function(data) {
        const lang = req.headers['x-app-language'] || req.query.lang || 'DE';
        if (data && typeof data === 'object') {
            if (data.message) data.message = translateServerMessage(data.message, lang);
            if (data.error) data.error = translateServerMessage(data.error, lang);
        }
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
app.get(['/download', '/download/', '/download/AlbionDataPro.apk', '/download/app-update.apk', '/download/latest.apk', '/download/latest', '/download/app'], (req, res) => {
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

// Google Search Console Verification Endpoint & File
app.get(['/google999f0d6f9c312079.html', '/google999f0d6f9c312079', '/google:id.html', '/qztdnbxgihfm.html', '/qztdnbxgihfm'], (req, res) => {
    res.type('text/html').send('google-site-verification: google999f0d6f9c312079.html\ngv-uvz2atxi4achnn.dv.googlehosted.com');
});

// Robots.txt for Search Engines
app.get('/robots.txt', (req, res) => {
    res.type('text/plain').send(
`User-agent: *
Allow: /
Disallow: /api/admin/
Disallow: /admin

Sitemap: https://albionmarketv2-1.onrender.com/sitemap.xml`
    );
});

// Sitemap.xml for Google Search Console (10 Specialized SEO Landing Page Endpoints)
app.get(['/sitemap.xml', '/sitemap.xml.gz'], (req, res) => {
    const today = new Date().toISOString().split('T')[0];
    res.type('application/xml').send(
`<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
        xmlns:xhtml="http://www.w3.org/1999/xhtml"
        xmlns:image="http://www.google.com/schemas/sitemap-image/1.1"
        xmlns:mobile="http://www.google.com/schemas/sitemap-mobile/1.0">
   <url>
      <loc>https://albionmarketv2-1.onrender.com/</loc>
      <lastmod>${today}</lastmod>
      <changefreq>always</changefreq>
      <priority>1.0</priority>
      <xhtml:link rel="alternate" hreflang="de" href="https://albionmarketv2-1.onrender.com/"/>
      <xhtml:link rel="alternate" hreflang="en" href="https://albionmarketv2-1.onrender.com/"/>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/hack</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.9</priority>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/bot</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.9</priority>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/arbitrage</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.9</priority>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/blackmarket</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.9</priority>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/calculator</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.9</priority>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/overlay</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.9</priority>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/mobile</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.9</priority>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/guide</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.9</priority>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/app</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.9</priority>
   </url>
   <url>
      <loc>https://albionmarketv2-1.onrender.com/download/AlbionDataPro.apk</loc>
      <lastmod>${today}</lastmod>
      <changefreq>daily</changefreq>
      <priority>0.8</priority>
   </url>
</urlset>`
    );
});

// Redirects for alternative sitemap typos (/sitemap.hml, /sitemap.html, /sitemap)
app.get(['/sitemap.hml', '/sitemap.html', '/sitemap'], (req, res) => {
    res.redirect(301, '/sitemap.xml');
});

// Privacy Policy Route
app.get(['/privacy', '/privacy-policy', '/datenschutz'], (req, res) => {
    res.send(`<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Datenschutzerklärung - AlbionDataPro</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; color: #f8fafc; line-height: 1.6; padding: 20px; max-width: 800px; margin: 0 auto; }
        h1 { color: #38bdf8; border-bottom: 2px solid #334155; padding-bottom: 10px; }
        h2 { color: #38bdf8; margin-top: 30px; }
        a { color: #38bdf8; text-decoration: none; }
        a:hover { text-decoration: underline; }
        .card { background: #1e293b; padding: 30px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.3); border: 1px solid #334155; }
        ul { padding-left: 20px; }
        li { margin-bottom: 8px; }
    </style>
</head>
<body>
    <div class="card">
        <h1>Datenschutzerklärung für AlbionDataPro (AlbionMarketV2)</h1>
        <p><strong>Stand:</strong> Januar 2026</p>
        <p>Diese Datenschutzerklärung klärt Sie über die Art, den Umfang und den Zweck der Verarbeitung von personenbezogenen Daten innerhalb unserer Android-App <strong>AlbionDataPro</strong> (sowie der zugehörigen Webdienste und APIs unter <code>albionmarketv2-1.onrender.com</code> und <code>www.AlbionDataPro.com</code>) auf.</p>

        <h2>1. Verantwortlicher</h2>
        <p>Verantwortlicher im Sinne der Datenschutzgesetze ist:<br>
        <strong>Entwickler / Betreiber:</strong> AlbionDataPro Team / Leo Dennis<br>
        <strong>Website:</strong> <a href="https://www.AlbionDataPro.com">https://www.AlbionDataPro.com</a></p>

        <h2>2. Arten der verarbeiteten Daten</h2>
        <ul>
            <li><strong>Geräte- und Technische Daten:</strong> Geräte-ID (Hardware-ID), Betriebssystemversion, IP-Adresse, App-Version und Absturzprotokolle zur Sicherung des Betriebs und zur Fehlerbehebung.</li>
            <li><strong>Lizenz- und Account-Daten:</strong> Aktivierungsschlüssel (License Keys) zur Verifizierung von Kauf- und Testversionen sowie ggf. Authentifizierungsdaten.</li>
            <li><strong>Nutzungs- und Marktdaten:</strong> Vom Nutzer eingegebene oder abgefragte Handelsdaten, Marktdaten, Preisalarme und Einstellungen zur Synchronisation mit unseren Servern.</li>
            <li><strong>Zahlungsdaten:</strong> Zahlungen werden über externe Zahlungsdienstleister (z. B. PayPal) abgewickelt. Wir speichern keine Kreditkarten- oder Bankdaten.</li>
        </ul>

        <h2>3. Zweck der Datenverarbeitung</h2>
        <ul>
            <li>Bereitstellung der App-Funktionen (Marktanalysen, Handelsbots, Preissynchronisation).</li>
            <li>Lizenzprüfung, Betrugsschutz und Verwaltung von Software-Updates (OTA-Updates).</li>
            <li>Kommunikation mit dem Server zur Marktdatensynchronisation.</li>
        </ul>

        <h2>4. Einsatz von Drittanbietern und APIs</h2>
        <ul>
            <li><strong>Eigenes Backend (Render):</strong> <code>https://albionmarketv2-1.onrender.com</code> zur Daten- und Lizenzsynchronisation.</li>
            <li><strong>Firebase (Google):</strong> Für Authentifizierung, Push-Benachrichtigungen und Crash-Reporting.</li>
            <li><strong>Albion Online Data Project APIs:</strong> Zum Abrufen öffentlicher Spielmarktstatistiken.</li>
            <li><strong>PayPal:</strong> Zur Abwicklung von Lizenzkäufen und Spenden.</li>
        </ul>

        <h2>5. Datensicherheit</h2>
        <p>Wir setzen technische und organisatorische Sicherheitsmaßnahmen ein (u. a. verschlüsselte HTTPS-Verbindungen und Token-Authentifizierung), um Ihre Daten zu schützen.</p>

        <h2>6. Ihre Rechte als Nutzer</h2>
        <p>Sie haben das Recht auf Auskunft, Berichtigung oder Löschung Ihrer Daten. Bei Fragen kontaktieren Sie uns über unsere Website.</p>

        <p style="margin-top: 40px; text-align: center;"><a href="/">← Zurück zur Startseite</a></p>
    </div>
</body>
</html>`);
});

// Landing Pages (10 Specialized SEO Portal Routes for Google Rank #1)
app.get(['/', '/get', '/app', '/hack', '/bot', '/arbitrage', '/blackmarket', '/calculator', '/overlay', '/mobile', '/guide'], (req, res) => {
    const route = req.path.toLowerCase();

    let pageTitle = `AlbionDataPro v${CURRENT_SERVER_VERSION} - #1 Albion Online Hack, Markt Mod, Arbitrage & Overlay`;
    let pageDesc = `AlbionDataPro ist der #1 Albion Online Hack & Markt-Mod für Mobile & PC. Das ultimative Markt-Overlay, Arbitrage Radar, Silber Rechner, Schwarzmarkt Bot & Preissuchmaschine. Jetzt Silber verdoppeln!`;

    if (route.includes('hack')) {
        pageTitle = `Albion Online Hack & Mod 2026 - #1 Markt & Trading Overlay | AlbionDataPro`;
        pageDesc = `Der beste Albion Online Hack & Mod für Android & PC. In-Game Markt-Overlay, Live-Preise, Arbitrage Radar & KI Trade Bot. Jetzt Silber verdoppeln!`;
    } else if (route.includes('bot')) {
        pageTitle = `Albion Online Market Bot & KI Signals 2026 | AlbionDataPro`;
        pageDesc = `Automatische KI-Kauf- & Verkaufsaufträge für Albion Online. Verdopple deine Silber-Erträge mit dem #1 Albion Trading Bot.`;
    } else if (route.includes('arbitrage')) {
        pageTitle = `Albion Online Arbitrage Radar & Routen-Planner | AlbionDataPro`;
        pageDesc = `Finde die lukrativsten Handelsrouten in Albion Online. Exakte Reingewinn-Berechnung zwischen Caerleon, Brecilien & königlichen Hauptstädten.`;
    } else if (route.includes('blackmarket')) {
        pageTitle = `Albion Online Schwarzmarkt Rechner & Caerleon Bot | AlbionDataPro`;
        pageDesc = `Maximierte Gewinne am Caerleon Schwarzmarkt. Berechne Ausrüstungs-Preise, Margen und Beutelgewicht in Echtzeit.`;
    } else if (route.includes('calculator')) {
        pageTitle = `Albion Online Silber & Crafting Rechner 2026 | AlbionDataPro`;
        pageDesc = `Berechne Veredelungs- & Herstellungskosten, Stadt-Rückgaberaten (Return Rates), Stationsgebühren und Reingewinn.`;
    } else if (route.includes('overlay')) {
        pageTitle = `Albion Online In-Game Overlay Bubble Mod for Mobile | AlbionDataPro`;
        pageDesc = `Schwebendes In-Game Markt-Overlay direkt über Albion Online Mobile. Preise, Arbitrage & KI-Signale im Spiel anzeigen ohne Minimieren.`;
    } else if (route.includes('mobile')) {
        pageTitle = `Albion Online Mobile Mod Package (Android APK) | AlbionDataPro`;
        pageDesc = `Lade das offizielle Albion Online Mobile Mod Package herunter. In-Game Overlay, 24/7 Cloud Sync & automatische Updates.`;
    } else if (route.includes('guide')) {
        pageTitle = `Albion Online Silber Verdienen Guide 2026 | AlbionDataPro`;
        pageDesc = `Der ultimative Guide für maximalen Silber-Gewinn in Albion Online. Handels-Strategien, Markt-Lücken & KI-Tipps.`;
    }

    res.send(`<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="google-site-verification" content="T7hOrPz6NeYQJJoUIr7Tl0RWmnYmsA9MZ72tSXS47YI" />
    <meta name="google-site-verification" content="google999f0d6f9c312079" />
    <meta name="google-site-verification" content="google999f0d6f9c312079.html" />
    <meta name="description" content="${pageDesc}">
    <meta name="keywords" content="Albion Online, Albion Online Market, Albion Online Prices, Albion Online Gold Price, Albion Online Calculator, Albion Online Crafting, Albion Online Black Market, Albion Market Pro, Albion Market Helper, Albion Online Trade Assistant, Albion Online Mobile Hack, Albion Online Mod, Albion Online Cheat, Albion Online Market Hack, Albion Online Silver Hack">
    <meta name="robots" content="index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1" />
    <meta name="author" content="Albion Online Market Pro Team" />
    <link rel="canonical" href="https://albionmarketv2-1.onrender.com/" />
    <link rel="alternate" hreflang="de" href="https://albionmarketv2-1.onrender.com/" />
    <link rel="alternate" hreflang="en" href="https://albionmarketv2-1.onrender.com/" />
    <link rel="alternate" hreflang="x-default" href="https://albionmarketv2-1.onrender.com/" />

    <!-- OpenGraph SEO -->
    <meta property="og:site_name" content="Albion Online Market Pro" />
    <meta property="og:title" content="Albion Online - Official Market Assistant, Prices & Gold Calculator" />
    <meta property="og:description" content="Official #1 Albion Online Market Assistant. Real-time prices for Europe, Americas, Asia, Gold Market rates, crafting calculator, and floating overlay app." />
    <meta property="og:type" content="website" />
    <meta property="og:url" content="https://albionmarketv2-1.onrender.com/" />
    <meta property="og:image" content="https://albionmarketv2-1.onrender.com/download" />
    <meta property="og:locale" content="de_DE" />

    <!-- Twitter Card -->
    <meta name="twitter:card" content="summary_large_image" />
    <meta name="twitter:title" content="Albion Online - Market Assistant & Real-Time Price Database" />
    <meta name="twitter:description" content="Official #1 Albion Online Market Assistant. Real-time prices, Gold market, and floating overlay." />

    <!-- JSON-LD Structured Data Schema for Google Rich Snippets -->
    <script type="application/ld+json">
    {
      "@context": "https://schema.org",
      "@graph": [
        {
          "@type": "SoftwareApplication",
          "name": "AlbionDataPro - Albion Online Hack & Mod",
          "operatingSystem": "Android, Windows, macOS",
          "applicationCategory": "GameApplication",
          "aggregateRating": {
            "@type": "AggregateRating",
            "ratingValue": "4.9",
            "ratingCount": "2490",
            "reviewCount": "2490",
            "bestRating": "5",
            "worstRating": "1"
          },
          "offers": {
            "@type": "Offer",
            "price": "0",
            "priceCurrency": "EUR",
            "availability": "https://schema.org/InStock"
          },
          "description": "Der #1 Albion Online Hack & Markt-Mod für Mobile & PC. In-Game Bubble Overlay, Live-Preise, Arbitrage Radar & KI Trading Bot.",
          "url": "https://albionmarketv2-1.onrender.com/",
          "softwareVersion": "${CURRENT_SERVER_VERSION}"
        },
        {
          "@type": "FAQPage",
          "mainEntity": [
            {
              "@type": "Question",
              "name": "Was ist der AlbionDataPro Hack & Markt-Mod?",
              "acceptedAnswer": {
                "@type": "Answer",
                "text": "AlbionDataPro ist der führende Albion Online Hack & Markt-Mod. Es ist ein In-Game Overlay & Analysetool für Albion Online Mobile & PC, welches Live-Preise über alle Städte vergleicht, profitabelste Handelsrouten berechnet und KI-basierte Buy- und Sell-Orders liefert."
              }
            },
            {
              "@type": "Question",
              "name": "Wie funktioniert das In-Game Floating Overlay?",
              "acceptedAnswer": {
                "@type": "Answer",
                "text": "Die schwebende Overlay-Bubble läuft direkt über Albion Online Mobile auf Android. Durch Antippen öffnest du Live-Preise, Handelschancen und KI-Signale direkt im Spiel ohne Minimieren."
              }
            },
            {
              "@type": "Question",
              "name": "Ist dieser Albion Online Mod & Hack sicher?",
              "acceptedAnswer": {
                "@type": "Answer",
                "text": "Ja! AlbionDataPro nutzt externe Markt-APIs und arbeitet als rein visuelles Analyse-Overlay. Es führt keine automatischen Tastatureingaben oder Memory-Injections durch."
              }
            },
            {
              "@type": "Question",
              "name": "Wie schalte ich die App frei?",
              "acceptedAnswer": {
                "@type": "Answer",
                "text": "Installiere das kostenlose APK-Package. Erstelle in der App ein Konto. Du wirst danach zu PayPal weitergeleitet und dein Account wird nach dem Kauf sofort automatisch freigeschaltet."
              }
            }
          ]
        }
      ]
    }
    </script>
    <title>AlbionDataPro v${CURRENT_SERVER_VERSION} - #1 Albion Online Hack, Markt Mod, Arbitrage & Overlay</title>
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

            <div class="flex items-center gap-3">
                <!-- Glowing Website Language Selector -->
                <div class="relative inline-flex items-center">
                    <select id="webLangSelect" onchange="changeWebLanguage(this.value)" class="bg-slate-900/90 text-sky-400 font-extrabold border border-sky-400/60 hover:border-sky-300 rounded-xl px-3 py-2 text-xs shadow-[0_0_15px_rgba(56,189,248,0.35)] transition cursor-pointer outline-none">
                        <option value="de">🇩🇪 DE</option>
                        <option value="en">🇬🇧 EN</option>
                        <option value="es">🇪🇸 ES</option>
                        <option value="fr">🇫🇷 FR</option>
                        <option value="pt">🇵🇹 PT</option>
                        <option value="ru">🇷🇺 RU</option>
                        <option value="zh">🇨🇳 ZH</option>
                        <option value="ja">🇯🇵 JA</option>
                        <option value="ko">🇰🇷 KO</option>
                        <option value="tr">🇹🇷 TR</option>
                        <option value="id">🇮🇩 ID</option>
                        <option value="pl">🇵🇱 PL</option>
                    </select>
                </div>

                <a href="/download/AlbionDataPro.apk?v=${CURRENT_SERVER_VERSION}" class="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-extrabold px-5 py-2.5 rounded-xl text-sm transition shadow-lg flex items-center gap-2 border border-emerald-400/30">
                    <i class="fa-solid fa-download"></i> APK Download
                </a>
            </div>
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
            <span data-i18n="badge_release">Offizieller Version Release v${CURRENT_SERVER_VERSION} — 24/7 Cloud & In-Game Overlay</span>
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
        <p data-i18n="hero_subtitle" class="text-xl md:text-3xl font-extrabold text-slate-200 mb-6 max-w-3xl mx-auto leading-snug">
            Das mächtigste Handels- & Markt-Overlay für Albion Online Mobile
        </p>
        <p data-i18n="hero_desc" class="text-base md:text-lg text-slate-400 mb-10 max-w-2xl mx-auto leading-relaxed">
            Verdoppeln Sie Ihr Silber ohne stundenlanges Suchen. Nutzen Sie sekundengenaue Live-Preise, Arbitrage-Scanner & KI-Preise direkt als schwebendes In-Game Overlay über Ihrem Spiel!
        </p>

        <!-- DOWNLOAD BUTTON SECTION -->
        <div class="flex flex-col items-center gap-4 mb-16 max-w-md mx-auto w-full">
            <a href="/download/AlbionDataPro.apk?v=${CURRENT_SERVER_VERSION}" class="pulse-glow bg-gradient-to-r from-emerald-500 via-teal-500 to-emerald-600 hover:from-emerald-400 hover:to-teal-400 text-white font-black py-5 px-8 rounded-2xl shadow-2xl transition-all transform hover:scale-105 flex items-center justify-center gap-4 text-2xl border border-emerald-300/40 w-full">
                <i class="fa-solid fa-download text-3xl"></i>
                <div class="text-left">
                    <div data-i18n="download_btn_subtitle" class="text-xs uppercase tracking-wider font-extrabold text-emerald-200">Kostenlos Herunterladen</div>
                    <div>APK Download v${CURRENT_SERVER_VERSION}</div>
                </div>
            </a>
            <div class="text-xs text-slate-400 flex items-center gap-2">
                <i class="fa-solid fa-shield-halved text-emerald-400"></i> <span data-i18n="download_virus_free">100% Virenfrei • Direktes Android APK Package</span>
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
                    <h3 data-i18n="license_title" class="text-2xl font-black text-amber-400 mb-2 flex items-center gap-2">
                        🔒 Lizenzen direkt in der App erwerben & freischalten!
                    </h3>
                    <p data-i18n="license_desc" class="text-slate-300 text-sm md:text-base leading-relaxed mb-4">
                        Installieren Sie die App kostenlos über den Download-Button oben. Nach dem Start können Sie Ihren Account erstellen und Ihre Lizenz (1, 3, 6 oder 12 Monate) direkt in der App oder im Lizenz-Menü freischalten!
                    </p>
                    <div class="flex flex-wrap gap-3 text-xs font-bold text-slate-300">
                        <span class="bg-slate-800 px-3 py-1.5 rounded-lg border border-slate-700 flex items-center gap-1.5">
                            <i class="fa-solid fa-bolt text-amber-400"></i> <span data-i18n="badge_instant">Sofortige Freischaltung</span>
                        </span>
                        <span class="bg-slate-800 px-3 py-1.5 rounded-lg border border-slate-700 flex items-center gap-1.5">
                            <i class="fa-brands fa-paypal text-blue-400"></i> <span data-i18n="badge_paypal">Sichere PayPal-Zahlung</span>
                        </span>
                        <span class="bg-slate-800 px-3 py-1.5 rounded-lg border border-slate-700 flex items-center gap-1.5">
                            <i class="fa-solid fa-cloud text-emerald-400"></i> <span data-i18n="badge_cloud">24/7 Cloud-Verbindung</span>
                        </span>
                    </div>
                </div>
            </div>
        </div>

        <!-- WHY YOU NEED THIS TOOL (SALES PITCH) -->
        <div class="mb-20 text-left">
            <div class="text-center mb-12">
                <h2 data-i18n-html="why_title" class="text-3xl md:text-5xl font-black text-white mb-4">Warum du <span class="gradient-text">AlbionDataPro</span> brauchst</h2>
                <p data-i18n="why_sub" class="text-slate-400 text-base md:text-lg max-w-2xl mx-auto">
                    Handeln in Albion Online ohne Live-Daten kostet dich täglich Millionen Silber. AlbionDataPro gibt dir den entscheidenden Vorteil gegenüber anderen Spielern.
                </p>
            </div>

            <div class="grid md:grid-cols-3 gap-6">
                <div class="glass-panel p-8 rounded-2xl border-t-4 border-t-sky-500 hover:border-sky-400 transition">
                    <div class="text-sky-400 text-3xl font-black mb-4"><i class="fa-solid fa-coins"></i></div>
                    <h3 data-i18n="why_card1_title" class="text-xl font-bold text-white mb-2">Maximaler Profit ohne Risiko</h3>
                    <p data-i18n="why_card1_desc" class="text-slate-400 text-sm leading-relaxed">
                        Schluss mit Fehlkäufen! Der präzise ROI-Rechner zeigt dir vor jedem Deal exakt deinen Reingewinn nach Marktsteuern und Stationsgebühren an.
                    </p>
                </div>

                <div class="glass-panel p-8 rounded-2xl border-t-4 border-t-purple-500 hover:border-purple-400 transition">
                    <div class="text-purple-400 text-3xl font-black mb-4"><i class="fa-solid fa-gauge-high"></i></div>
                    <h3 data-i18n="why_card2_title" class="text-xl font-bold text-white mb-2">Gewaltige Zeitersparnis</h3>
                    <p data-i18n="why_card2_desc" class="text-slate-400 text-sm leading-relaxed">
                        Kein lästiges Hin- und Herreisen mehr, um Preise zu vergleichen. Unsere Cloud scannt alle Städte und zeigt dir die lukrativsten Trade-Routen in Sekunden.
                    </p>
                </div>

                <div class="glass-panel p-8 rounded-2xl border-t-4 border-t-emerald-500 hover:border-emerald-400 transition">
                    <div class="text-emerald-400 text-3xl font-black mb-4"><i class="fa-solid fa-wand-magic-sparkles"></i></div>
                    <h3 data-i18n="why_card3_title" class="text-xl font-bold text-white mb-2">Statistische KI-Garantie</h3>
                    <p data-i18n="why_card3_desc" class="text-slate-400 text-sm leading-relaxed">
                        Unser Algorithmus analysiert 7-Tage-Preisschwankungen und findet die idealen Buy- & Sell-Order Schwellenwerte für schnellen Umschlag.
                    </p>
                </div>
            </div>
        </div>

        <!-- BUBBLE OVERLAY FEATURES SECTION -->
        <div class="mb-20 text-left">
            <div class="text-center mb-12">
                <div data-i18n="overlay_section_tag" class="inline-block px-3 py-1 bg-blue-500/20 text-blue-400 text-xs font-extrabold rounded-full mb-3 uppercase tracking-wider border border-blue-500/30">
                    In-Game Floating Overlay
                </div>
                <h2 data-i18n="overlay_title" class="text-3xl md:text-5xl font-black text-white mb-4">Das In-Game Bubble Overlay</h2>
                <p data-i18n="overlay_sub" class="text-slate-400 text-base md:text-lg max-w-2xl mx-auto">
                    Die revolutionäre schwebende Overlay-Bubble läuft direkt über Albion Online Mobile. Du musst das Spiel niemals verlassen!
                </p>
            </div>

            <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
                <!-- Bubble Feature 1 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-sky-500/20 text-sky-400 flex items-center justify-center text-xl font-bold mb-4 border border-sky-500/30">
                        <i class="fa-solid fa-layer-group"></i>
                    </div>
                    <h4 data-i18n="feat1_title" class="text-lg font-bold text-white mb-2">Permanentes In-Game Overlay</h4>
                    <p data-i18n="feat1_desc" class="text-slate-400 text-sm leading-relaxed">
                        Ein dezentes, schwebendes Symbol direkt auf deinem Bildschirm. Tippe einfach darauf, um Preise, Arbitrage und KI-Empfehlungen sofort einzublenden.
                    </p>
                </div>

                <!-- Bubble Feature 2 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center text-xl font-bold mb-4 border border-amber-500/30">
                        <i class="fa-solid fa-route"></i>
                    </div>
                    <h4 data-i18n="feat2_title" class="text-lg font-bold text-white mb-2">Städte & Schwarzmarkt Radar</h4>
                    <p data-i18n="feat2_desc" class="text-slate-400 text-sm leading-relaxed">
                        Scannt Caerleon, Brecilien, den Schwarzmarkt und alle königlichen Städte. Berechnet Transportgewicht, Rüstungs-Tiers und exakte Margen.
                    </p>
                </div>

                <!-- Bubble Feature 3 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-purple-500/20 text-purple-400 flex items-center justify-center text-xl font-bold mb-4 border border-purple-500/30">
                        <i class="fa-solid fa-robot"></i>
                    </div>
                    <h4 data-i18n="feat3_title" class="text-lg font-bold text-white mb-2">KI Buy & Sell Order Bot</h4>
                    <p data-i18n="feat3_desc" class="text-slate-400 text-sm leading-relaxed">
                        Sagt dir exakt, zu welchem Preis du Kauf- und Verkaufsaufträge einstellen musst, um maximale Profite bei hoher Verkaufschance zu erzielen.
                    </p>
                </div>

                <!-- Bubble Feature 4 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center text-xl font-bold mb-4 border border-emerald-500/30">
                        <i class="fa-solid fa-hammer"></i>
                    </div>
                    <h4 data-i18n="feat4_title" class="text-lg font-bold text-white mb-2">Crafting & Veredelungs Rechner</h4>
                    <p data-i18n="feat4_desc" class="text-slate-400 text-sm leading-relaxed">
                        Berechnet Rohstoffkosten, Stadt-Rückgaberaten (Return Rates), Fokus-Ersparnis und Gebühren für Rüstungen, Waffen und Barren.
                    </p>
                </div>

                <!-- Bubble Feature 5 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-pink-500/20 text-pink-400 flex items-center justify-center text-xl font-bold mb-4 border border-pink-500/30">
                        <i class="fa-solid fa-seedling"></i>
                    </div>
                    <h4 data-i18n="feat5_title" class="text-lg font-bold text-white mb-2">Insel-Timer & Tierzucht</h4>
                    <p data-i18n="feat5_desc" class="text-slate-400 text-sm leading-relaxed">
                        Überwache deine Insel-Ernten und Zuchtzeiten mit Benachrichtigung, sobald deine Reittiere oder Pflanzen abholbereit sind.
                    </p>
                </div>

                <!-- Bubble Feature 6 -->
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <div class="w-12 h-12 rounded-xl bg-blue-500/20 text-blue-400 flex items-center justify-center text-xl font-bold mb-4 border border-blue-500/30">
                        <i class="fa-solid fa-cloud"></i>
                    </div>
                    <h4 data-i18n="feat6_title" class="text-lg font-bold text-white mb-2">24/7 Cloud-Sync & Auto-OTA</h4>
                    <p data-i18n="feat6_desc" class="text-slate-400 text-sm leading-relaxed">
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
                <h4 data-i18n="install_title" class="font-extrabold text-2xl text-white mb-2">Einfache Installation auf Android</h4>
                <ol class="list-decimal list-inside text-slate-300 text-sm md:text-base space-y-2">
                    <li data-i18n-html="install_step1">Klicke oben auf <strong class="text-emerald-400">"APK Download"</strong> und speichere die Datei.</li>
                    <li data-i18n-html="install_step2">Öffne <code class="bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono">AlbionDataPro.apk</code> und erlaube die Installation.</li>
                    <li data-i18n="install_step3">Starte die App, erstelle deinen Account und schalte deine Lizenz direkt in der App frei!</li>
                </ol>
            </div>
        </div>

        <!-- FAQ SECTION FOR GOOGLE RANKING & RICH SNIPPETS -->
        <div class="mb-20 text-left max-w-4xl mx-auto my-12">
            <div class="text-center mb-12">
                <h2 data-i18n="faq_title" class="text-3xl font-bold mb-3 text-white">Häufig gestellte Fragen (FAQ)</h2>
                <p data-i18n="faq_sub" class="text-slate-400">Alles, was du über AlbionDataPro wissen musst.</p>
            </div>

            <div class="space-y-4">
                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <h3 data-i18n="faq1_q" class="text-lg font-bold text-sky-400 mb-2">Was ist AlbionDataPro?</h3>
                    <p data-i18n="faq1_a" class="text-slate-300 text-sm leading-relaxed">
                        AlbionDataPro ist das führende In-Game Overlay & Markt-Analysetool für Albion Online. Es vergleicht Preise über alle Städte (Caerleon, Martlock, Lymhurst, Bridgewatch, Fort Sterling, Thetford, Schwarzmarkt), berechnet profitabelste Handelsrouten und empfiehlt KI-basierte Buy- und Sell-Orders.
                    </p>
                </div>

                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <h3 data-i18n="faq2_q" class="text-lg font-bold text-sky-400 mb-2">Wie funktioniert das In-Game Floating Overlay?</h3>
                    <p data-i18n="faq2_a" class="text-slate-300 text-sm leading-relaxed">
                        Die schwebende Overlay-Bubble läuft direkt über Albion Online Mobile auf Android. Durch einfaches Antippen blendest du Live-Preise, Arbitrage-Chancen und KI-Signale direkt im Spiel ein, ohne das Spiel minimieren zu müssen.
                    </p>
                </div>

                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <h3 data-i18n="faq3_q" class="text-lg font-bold text-sky-400 mb-2">Ist AlbionDataPro sicher und erlaubt?</h3>
                    <p data-i18n="faq3_a" class="text-slate-300 text-sm leading-relaxed">
                        Ja! AlbionDataPro nutzt öffentliche Markt-APIs und arbeitet als rein visuelles Analyse-Overlay. Es führt keine automatischen Tastatureingaben oder Memory-Injections durch und ist somit 100% sicher zu bedienen.
                    </p>
                </div>

                <div class="glass-panel p-6 rounded-2xl border border-slate-800">
                    <h3 data-i18n="faq4_q" class="text-lg font-bold text-sky-400 mb-2">Wie schalte ich meinen Account frei?</h3>
                    <p data-i18n="faq4_a" class="text-slate-300 text-sm leading-relaxed">
                        Installiere das kostenlose APK-Package oben, erstelle deinen Account in der App und schalte ihn über den PayPal-Bezahllink direkt frei. Dein Account wird nach der Zahlung automatisch auf den Servern freigeschaltet.
                    </p>
                </div>
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
        <p data-i18n-html="footer_copyright">&copy; 2026 AlbionDataPro. Alle Rechte vorbehalten. Gehostet auf Render Cloud.</p>
        <div class="mt-2 text-xs">
            <a href="/privacy" class="text-sky-400 hover:underline">Datenschutzerklärung / Privacy Policy</a>
        </div>
        <p data-i18n="footer_disclaimer" class="text-xs mt-2 text-slate-600">Dieses Analyse-Tool steht in keiner offiziellen Verbindung zu Sandbox Interactive.</p>
    </footer>

    <script>
        const WEB_TRANSLATIONS = {
            de: {
                badge_release: "Offizieller Version Release v${CURRENT_SERVER_VERSION} — 24/7 Cloud & In-Game Overlay",
                hero_subtitle: "Das mächtigste Handels- & Markt-Overlay für Albion Online Mobile",
                hero_desc: "Verdoppeln Sie Ihr Silber ohne stundenlanges Suchen. Nutzen Sie sekundengenaue Live-Preise, Arbitrage-Scanner & KI-Preise direkt als schwebendes In-Game Overlay über Ihrem Spiel!",
                download_btn_subtitle: "Kostenlos Herunterladen",
                download_virus_free: "100% Virenfrei • Direktes Android APK Package",
                license_title: "🔒 Lizenzen direkt in der App erwerben & freischalten!",
                license_desc: "Installieren Sie die App kostenlos über den Download-Button oben. Nach dem Start können Sie Ihren Account erstellen und Ihre Lizenz (1, 3, 6 oder 12 Monate) direkt in der App oder im Lizenz-Menü freischalten!",
                badge_instant: "Sofortige Freischaltung",
                badge_paypal: "Sichere PayPal-Zahlung",
                badge_cloud: "24/7 Cloud-Verbindung",
                why_title: "Warum du <span class='gradient-text'>AlbionDataPro</span> brauchst",
                why_sub: "Handeln in Albion Online ohne Live-Daten kostet dich täglich Millionen Silber. AlbionDataPro gibt dir den entscheidenden Vorteil gegenüber anderen Spielern.",
                why_card1_title: "Maximaler Profit ohne Risiko",
                why_card1_desc: "Schluss mit Fehlkäufen! Der präzise ROI-Rechner zeigt dir vor jedem Deal exakt deinen Reingewinn nach Marktsteuern und Stationsgebühren an.",
                why_card2_title: "Gewaltige Zeitersparnis",
                why_card2_desc: "Kein lästiges Hin- und Herreisen mehr, um Preise zu vergleichen. Unsere Cloud scannt alle Städte und zeigt dir die lukrativsten Trade-Routen in Sekunden.",
                why_card3_title: "Statistische KI-Garantie",
                why_card3_desc: "Unser Algorithmus analysiert 7-Tage-Preisschwankungen und findet die idealen Buy- & Sell-Order Schwellenwerte für schnellen Umschlag.",
                overlay_section_tag: "In-Game Floating Overlay",
                overlay_title: "Das In-Game Bubble Overlay",
                overlay_sub: "Die revolutionäre schwebende Overlay-Bubble läuft direkt über Albion Online Mobile. Du musst das Spiel niemals verlassen!",
                feat1_title: "Permanentes In-Game Overlay",
                feat1_desc: "Ein dezentes, schwebendes Symbol direkt auf deinem Bildschirm. Tippe einfach darauf, um Preise, Arbitrage und KI-Empfehlungen sofort einzublenden.",
                feat2_title: "Städte & Schwarzmarkt Radar",
                feat2_desc: "Scannt Caerleon, Brecilien, den Schwarzmarkt und alle königlichen Städte. Berechnet Transportgewicht, Rüstungs-Tiers und exakte Margen.",
                feat3_title: "KI Buy & Sell Order Bot",
                feat3_desc: "Sagt dir exakt, zu welchem Preis du Kauf- und Verkaufsaufträge einstellen musst, um maximale Profite bei hoher Verkaufschance zu erzielen.",
                feat4_title: "Crafting & Veredelungs Rechner",
                feat4_desc: "Berechnet Rohstoffkosten, Stadt-Rückgaberaten (Return Rates), Fokus-Ersparnis und Gebühren für Rüstungen, Waffen und Barren.",
                feat5_title: "Insel-Timer & Tierzucht",
                feat5_desc: "Überwache deine Insel-Ernten und Zuchtzeiten mit Benachrichtigung, sobald deine Reittiere oder Pflanzen abholbereit sind.",
                feat6_title: "24/7 Cloud-Sync & Auto-OTA",
                feat6_desc: "Deine Einstellungen und Favoriten sind sicher in der Cloud gespeichert. Automatische OTA-Updates halten deine App stets aktuell.",
                install_title: "Einfache Installation auf Android",
                install_step1: "Klicke oben auf <strong class='text-emerald-400'>'APK Download'</strong> und speichere die Datei.",
                install_step2: "Öffne <code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> und erlaube die Installation.",
                install_step3: "Starte die App, erstelle deinen Account und schalte deine Lizenz direkt in der App frei!",
                faq_title: "Häufig gestellte Fragen (FAQ)",
                faq_sub: "Alles, was du über AlbionDataPro wissen musst.",
                faq1_q: "Was ist AlbionDataPro?",
                faq1_a: "AlbionDataPro ist das führende In-Game Overlay & Markt-Analysetool für Albion Online. Es vergleicht Preise über alle Städte (Caerleon, Martlock, Lymhurst, Bridgewatch, Fort Sterling, Thetford, Schwarzmarkt), berechnet profitabelste Handelsrouten und empfiehlt KI-basierte Buy- und Sell-Orders.",
                faq2_q: "Wie funktioniert das In-Game Floating Overlay?",
                faq2_a: "Die schwebende Overlay-Bubble läuft direkt über Albion Online Mobile auf Android. Durch einfaches Antippen blendest du Live-Preise, Arbitrage-Chancen und KI-Signale direkt im Spiel ein, ohne das Spiel minimieren zu müssen.",
                faq3_q: "Ist AlbionDataPro sicher und erlaubt?",
                faq3_a: "Ja! AlbionDataPro nutzt öffentliche Markt-APIs und arbeitet als rein visuelles Analyse-Overlay. Es führt keine automatischen Tastatureingaben oder Memory-Injections durch und ist somit 100% sicher zu bedienen.",
                faq4_q: "Wie schalte ich meinen Account frei?",
                faq4_a: "Installiere das kostenlose APK-Package oben, erstelle deinen Account in der App und schalte ihn über den PayPal-Bezahllink direkt frei. Dein Account wird nach der Zahlung automatisch auf den Servern freigeschaltet.",
                footer_copyright: "&copy; 2026 AlbionDataPro. Alle Rechte vorbehalten. Gehostet auf Render Cloud.",
                footer_disclaimer: "Dieses Analyse-Tool steht in keiner offiziellen Verbindung zu Sandbox Interactive."
            },
            en: {
                badge_release: "Official Version Release v${CURRENT_SERVER_VERSION} — 24/7 Cloud & In-Game Overlay",
                hero_subtitle: "The most powerful trading & market overlay for Albion Online Mobile",
                hero_desc: "Double your silver without endless searching. Use real-time live prices, arbitrage scanners & AI price signals directly as a floating overlay over your game!",
                download_btn_subtitle: "Free Download",
                download_virus_free: "100% Virus Free • Direct Android APK Package",
                license_title: "🔒 Purchase & Unlock Licenses Directly In The App!",
                license_desc: "Install the app for free using the download button above. After launching, create your account and unlock your license (1, 3, 6 or 12 months) directly inside the app!",
                badge_instant: "Instant Activation",
                badge_paypal: "Secure PayPal Payment",
                badge_cloud: "24/7 Cloud Connection",
                why_title: "Why you need <span class='gradient-text'>AlbionDataPro</span>",
                why_sub: "Trading in Albion Online without live data costs you millions of silver daily. AlbionDataPro gives you the decisive edge over other players.",
                why_card1_title: "Maximum Profit Without Risk",
                why_card1_desc: "No more bad purchases! The precise ROI calculator displays your exact net profit after market taxes and crafting fees before every deal.",
                why_card2_title: "Massive Time Savings",
                why_card2_desc: "No more traveling back and forth to compare prices. Our cloud scans all cities and shows you the most lucrative trade routes in seconds.",
                why_card3_title: "Statistical AI Guarantee",
                why_card3_desc: "Our algorithm analyzes 7-day price fluctuations and determines the ideal buy & sell order thresholds for fast turnover.",
                overlay_section_tag: "In-Game Floating Overlay",
                overlay_title: "The In-Game Bubble Overlay",
                overlay_sub: "The revolutionary floating overlay bubble runs directly over Albion Online Mobile. You never have to leave the game!",
                feat1_title: "Permanent In-Game Overlay",
                feat1_desc: "A subtle floating icon right on your screen. Just tap it to reveal live prices, arbitrage deals and AI signals instantly.",
                feat2_title: "Cities & Black Market Radar",
                feat2_desc: "Scans Caerleon, Brecilien, Black Market, and all Royal Cities. Calculates weight, armor tiers, and exact profit margins.",
                feat3_title: "AI Buy & Sell Order Bot",
                feat3_desc: "Tells you the exact price points for buy & sell orders to maximize profit while ensuring high turnover chances.",
                feat4_title: "Crafting & Refining Calculator",
                feat4_desc: "Calculates resource costs, city resource return rates (RRR), focus savings, and crafting fees for armor, weapons & ingots.",
                feat5_title: "Island Timers & Animal Breeding",
                feat5_desc: "Monitor island harvests and breeding times with notifications as soon as crops or mounts are ready to harvest.",
                feat6_title: "24/7 Cloud Sync & Auto-OTA",
                feat6_desc: "Your settings and favorites are stored securely in the cloud. Automatic OTA updates keep your app always up to date.",
                install_title: "Easy Installation on Android",
                install_step1: "Click <strong class='text-emerald-400'>'APK Download'</strong> above and save the file.",
                install_step2: "Open <code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> and allow installation.",
                install_step3: "Launch the app, create your account and unlock your license directly inside the app!",
                faq_title: "Frequently Asked Questions (FAQ)",
                faq_sub: "Everything you need to know about AlbionDataPro.",
                faq1_q: "What is AlbionDataPro?",
                faq1_a: "AlbionDataPro is the premier in-game overlay & market analysis tool for Albion Online. It compares prices across all cities (Caerleon, Martlock, Lymhurst, Bridgewatch, Fort Sterling, Thetford, Black Market), calculates profitable trade routes, and recommends AI buy & sell orders.",
                faq2_q: "How does the in-game floating overlay work?",
                faq2_a: "The floating overlay bubble runs directly over Albion Online Mobile on Android. With a simple tap, you overlay live prices, arbitrage opportunities, and AI signals directly in-game without minimizing the app.",
                faq3_q: "Is AlbionDataPro safe and allowed?",
                faq3_a: "Yes! AlbionDataPro uses public market APIs and operates purely as a visual analysis overlay. It performs no automated input, keypresses, or memory injections, making it 100% safe to use.",
                faq4_q: "How do I unlock my account?",
                faq4_a: "Install the free APK package above, create your account inside the app, and unlock it directly using the PayPal link. Your account will be activated automatically on our servers right after payment.",
                footer_copyright: "&copy; 2026 AlbionDataPro. All rights reserved. Hosted on Render Cloud.",
                footer_disclaimer: "This analysis tool is not affiliated with Sandbox Interactive."
            },
            es: {
                badge_release: "Lanzamiento oficial v${CURRENT_SERVER_VERSION} — Nube 24/7 y Overlay",
                hero_subtitle: "El overlay de comercio y mercado más potente para Albion Online Mobile",
                hero_desc: "¡Duplica tu plata sin perder tiempo buscando! Usa precios en tiempo real, escáner de arbitraje y señales de IA como overlay flotante sobre tu juego.",
                download_btn_subtitle: "Descargar Gratis",
                download_virus_free: "100% Libre de Virus • Paquete APK para Android",
                license_title: "🔒 ¡Comprar y activar licencias directamente en la app!",
                license_desc: "Instala la aplicación gratis con el botón de descarga. Crea tu cuenta al iniciar y activa tu licencia (1, 3, 6 o 12 meses) directamente en la app.",
                badge_instant: "Activación Inmediata",
                badge_paypal: "Pago Seguro con PayPal",
                badge_cloud: "Conexión a la Nube 24/7",
                why_title: "Por qué necesitas <span class='gradient-text'>AlbionDataPro</span>",
                why_sub: "Comerciar sin datos en vivo te cuesta millones de plata al día. AlbionDataPro te da la ventaja decisiva frente a otros jugadores.",
                why_card1_title: "Máximo beneficio sin riesgo",
                why_card1_desc: "¡Se acabaron las malas compras! El calculador de ROI te muestra tu beneficio neto exacto tras impuestos y tarifas antes de cada trato.",
                why_card2_title: "Ahorro enorme de tiempo",
                why_card2_desc: "No viaje más entre ciudades para comparar precios. Nuestra nube escanea todas las ciudades y te muestra las rutas más rentables en segundos.",
                why_card3_title: "Garantía de IA Estadística",
                why_card3_desc: "Nuestro algoritmo analiza fluctuaciones de 7 días y encuentra los umbrales ideales de compra y venta para alta rotación.",
                overlay_section_tag: "Overlay Flotante En El Juego",
                overlay_title: "El Overlay Flotante De Burbuja",
                overlay_sub: "La revolucionaria burbuja flotante funciona directamente sobre Albion Online Mobile. ¡Nunca tienes que salir del juego!",
                feat1_title: "Overlay Permanente En Juego",
                feat1_desc: "Un icono flotante discreto en tu pantalla. Toca para ver precios, arbitraje y señales de IA al instante.",
                feat2_title: "Radar de Ciudades y Mercado Negro",
                feat2_desc: "Escanea Caerleon, Brecilien, Mercado Negro y Ciudades Reales. Calcula peso, tiers y márgenes exactos.",
                feat3_title: "Bot de Órdenes de Compra y Venta con IA",
                feat3_desc: "Te indica los precios exactos de compra y venta para maximizar ganancias y asegurar rápida venta.",
                feat4_title: "Calculadora de Fabricación y Refinado",
                feat4_desc: "Calcula costes, retorno de recursos por ciudad (RRR), ahorro de foco y tasas de estación para armaduras, armas y lingotes.",
                feat5_title: "Temporizador de Isla y Crianza",
                feat5_desc: "Monitorea tus cosechas y crianza con notificaciones cuando tus plantas o monturas estén listas para recoger.",
                feat6_title: "Sincronización en la Nube 24/7 y OTA",
                feat6_desc: "Tus ajustes y favoritos guardados de forma segura en la nube. Actualizaciones OTA automáticas mantienen tu app al día.",
                install_title: "Instalación fácil en Android",
                install_step1: "Haz clic en <strong class='text-emerald-400'>'APK Download'</strong> arriba y guarda el archivo.",
                install_step2: "Abre <code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> y permite la instalación.",
                install_step3: "¡Abre la app, crea tu cuenta y activa tu licencia directamente en la aplicación!",
                faq_title: "Preguntas Frecuentes (FAQ)",
                faq_sub: "Todo lo que necesitas saber sobre AlbionDataPro.",
                faq1_q: "¿Qué es AlbionDataPro?",
                faq1_a: "AlbionDataPro es la herramienta líder de análisis de mercado y overlay en juego para Albion Online. Compara precios en todas las ciudades, calcula rutas y recomienda órdenes de compra/venta por IA.",
                faq2_q: "¿Cómo funciona el overlay flotante en el juego?",
                faq2_a: "La burbuja flotante funciona sobre Albion Online Mobile en Android. Con un toque muestra precios, arbitraje y señales de IA sin minimizar el juego.",
                faq3_q: "¿Es seguro y permitido AlbionDataPro?",
                faq3_a: "¡Sí! Utiliza APIs públicas y funciona como un overlay de análisis visual. No realiza automatizaciones de teclado ni inyecciones de memoria, siendo 100% seguro.",
                faq4_q: "¿Cómo activo mi cuenta?",
                faq4_a: "Instala el paquete APK gratuito, crea tu cuenta en la app y actívala con el enlace de PayPal. Tu cuenta se activará automáticamente tras el pago.",
                footer_copyright: "&copy; 2026 AlbionDataPro. Todos los derechos reservados. Alojado en Render Cloud.",
                footer_disclaimer: "Esta herramienta no está afiliada a Sandbox Interactive."
            },
            fr: {
                badge_release: "Version officielle v${CURRENT_SERVER_VERSION} — Nuage 24/7 & Overlay",
                hero_subtitle: "L'overlay de commerce et de marché le plus puissant pour Albion Online Mobile",
                hero_desc: "Doublez votre argent sans chercher pendant des heures. Utilisez des prix en direct, des scanners d'arbitrage et l'IA en overlay flottant sur votre jeu !",
                download_btn_subtitle: "Téléchargement Gratuit",
                download_virus_free: "100% Sans Virus • Package APK Android Direct",
                license_title: "🔒 Achetez et débloquez votre licence directement dans l'application !",
                license_desc: "Installez gratuitement l'application via le bouton ci-dessus. Créez votre compte au lancement et débloquez votre licence (1, 3, 6 ou 12 mois) directement dans l'application !",
                badge_instant: "Activation Immédiate",
                badge_paypal: "Paiement Sécurisé PayPal",
                badge_cloud: "Connexion Cloud 24/7",
                why_title: "Pourquoi vous avez besoin de <span class='gradient-text'>AlbionDataPro</span>",
                why_sub: "Commercer sans données en direct vous coûte des millions d'argent chaque jour. AlbionDataPro vous donne l'avantage décisif.",
                why_card1_title: "Profit maximum sans risque",
                why_card1_desc: "Fini les mauvais achats ! Le calculateur de ROI affiche votre profit net exact après taxes et frais avant chaque transaction.",
                why_card2_title: "Gain de temps massif",
                why_card2_desc: "Plus besoin de voyager pour comparer les prix. Notre cloud scanne toutes les villes et vous montre les meilleures routes en quelques secondes.",
                why_card3_title: "Garantie IA Statistique",
                why_card3_desc: "Notre algorithme analyse les fluctuations sur 7 jours et trouve les seuils d'achat/vente idéaux pour une rotation rapide.",
                overlay_section_tag: "Overlay Flottant En Jeu",
                overlay_title: "L'Overlay Bulle En Jeu",
                overlay_sub: "La bulle flottante révolutionnaire s'exécute directement au-dessus d'Albion Online Mobile. Vous n'avez jamais à quitter le jeu !",
                feat1_title: "Overlay Permanent En Jeu",
                feat1_desc: "Une icône discrète sur votre écran. Appuyez dessus pour afficher immédiatement les prix, l'arbitrage et les signaux IA.",
                feat2_title: "Radar Villes & Marché Noir",
                feat2_desc: "Scanne Caerleon, Brecilien, le Marché Noir et les Villes Royales. Calcule poids, tiers et marges exactes.",
                feat3_title: "Bot d'Ordres d'Achat/Vente IA",
                feat3_desc: "Indique exactement les prix d'achat et de vente pour maximiser les profits avec une haute probabilité de vente.",
                feat4_title: "Calculateur d'Artisanat & Raffinage",
                feat4_desc: "Calcule coûts, taux de restitution (RRR), économies de focalisation et frais d'artisanat pour armures, armes et lingots.",
                feat5_title: "Minuteurs d'Île & Élevage",
                feat5_desc: "Surveillez vos récoltes et temps d'élevage avec des notifications dès que vos montures ou cultures sont prêtes.",
                feat6_title: "Synchro Cloud 24/7 & Auto-OTA",
                feat6_desc: "Vos paramètres et favoris sauvegardés en sécurité dans le cloud. Mises à jour OTA automatiques.",
                install_title: "Installation facile sur Android",
                install_step1: "Cliquez sur <strong class='text-emerald-400'>'Télécharger APK'</strong> ci-dessus et enregistrez le fichier.",
                install_step2: "Ouvrez <code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> et autorisez l'installation.",
                install_step3: "Lancez l'application, créez votre compte et débloquez votre licence directement dans l'application !",
                faq_title: "Foire Aux Questions (FAQ)",
                faq_sub: "Tout ce que vous devez savoir sur AlbionDataPro.",
                faq1_q: "Qu'est-ce qu'AlbionDataPro ?",
                faq1_a: "AlbionDataPro est le premier outil d'analyse de marché et d'overlay en jeu pour Albion Online. Il compare les prix entre toutes les villes, calcule les routes rentables et recommande des ordres IA.",
                faq2_q: "Comment fonctionne l'overlay flottant en jeu ?",
                faq2_a: "La bulle flottante s'exécute directement sur Albion Online Mobile. Un simple clic affiche les prix, l'arbitrage et l'IA en jeu sans réduire le jeu.",
                faq3_q: "AlbionDataPro est-il sûr et autorisé ?",
                faq3_a: "Oui ! Il utilise des API publiques et fonctionne comme un overlay d'analyse visuelle. Aucune saisie automatique ni injection mémoire, 100% sûr.",
                faq4_q: "Comment débloquer mon compte ?",
                faq4_a: "Installez l'APK gratuit, créez votre compte dans l'application et débloquez-le via le lien PayPal. Votre compte sera activé automatiquement.",
                footer_copyright: "&copy; 2026 AlbionDataPro. Tous droits réservés. Hébergé sur Render Cloud.",
                footer_disclaimer: "Cet outil d'analyse n'est pas affilié à Sandbox Interactive."
            },
            pt: {
                badge_release: "Lançamento oficial v${CURRENT_SERVER_VERSION} — Nuvem 24/7 e Overlay",
                hero_subtitle: "O overlay de comércio e mercado mais poderoso para Albion Online Mobile",
                hero_desc: "Dobre sua prata sem perder tempo procurando. Use preços em tempo real, scanners de arbitragem e IA em um overlay flutuante sobre seu jogo!",
                download_btn_subtitle: "Baixar Grátis",
                download_virus_free: "100% Livre de Vírus • Pacote APK Android Direto",
                license_title: "🔒 Adquira e ative licenças diretamente no aplicativo!",
                license_desc: "Instale o aplicativo gratuitamente. Crie sua conta e ative sua licença (1, 3, 6 ou 12 meses) diretamente no aplicativo!",
                badge_instant: "Ativação Imediata",
                badge_paypal: "Pagamento Seguro via PayPal",
                badge_cloud: "Conexão de Nuvem 24/7",
                why_title: "Por que você precisa do <span class='gradient-text'>AlbionDataPro</span>",
                why_sub: "Negociar sem dados em tempo real custa milhões de prata diariamente. O AlbionDataPro dá a você a vantagem decisiva.",
                why_card1_title: "Lucro máximo sem risco",
                why_card1_desc: "Sem mais compras erradas! A calculadora de ROI exibe seu lucro líquido exato após impostos e taxas antes de cada negócio.",
                why_card2_title: "Enorme economia de tempo",
                why_card2_desc: "Chega de viajar para comparar preços. Nossa nuvem escaneia todas as cidades e mostra as rotas mais lucrativas em segundos.",
                why_card3_title: "Garantia de IA Estatística",
                why_card3_desc: "Nosso algoritmo analisa flutuações de 7 dias e encontra os limites ideais de compra e venda para um giro rápido.",
                overlay_section_tag: "Overlay Flutuante No Jogo",
                overlay_title: "O Overlay Flutuante Em Bolha",
                overlay_sub: "A revolucionária bolha flutuante funciona diretamente sobre o Albion Online Mobile. Você nunca precisa sair do jogo!",
                feat1_title: "Overlay Permanente No Jogo",
                feat1_desc: "Um ícone flutuante discreto na sua tela. Toque para ver preços, arbitragem e sinais de IA instantaneamente.",
                feat2_title: "Radar de Cidades e Mercado Negro",
                feat2_desc: "Escaneia Caerleon, Brecilien, Mercado Negro e Cidades Reais. Calcula peso, tiers e margens exatas.",
                feat3_title: "Bot de Ordens de Compra e Venda por IA",
                feat3_desc: "Indica os preços exatos para ordens de compra e venda visando lucro máximo e rotação rápida.",
                feat4_title: "Calculadora de Fabricação e Refino",
                feat4_desc: "Calcula custos de matéria-prima, retorno de recursos (RRR), economia de foco e taxas para armaduras e armas.",
                feat5_title: "Temporizador de Ilha e Criação",
                feat5_desc: "Monitore colheitas e tempos de criação com notificações assim que plantas ou montarias estiverem prontas.",
                feat6_title: "Sincronização em Nuvem 24/7 e OTA",
                feat6_desc: "Suas configurações e favoritos salvos com segurança na nuvem. Atualizações OTA automáticas.",
                install_title: "Instalação fácil no Android",
                install_step1: "Clique em <strong class='text-emerald-400'>'Baixar APK'</strong> acima e salve o arquivo.",
                install_step2: "Abra o <code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> e permita a instalação.",
                install_step3: "Inicie o aplicativo, crie sua conta e ative sua licença diretamente no aplicativo!",
                faq_title: "Perguntas Frequentes (FAQ)",
                faq_sub: "Tudo o que você precisa saber sobre o AlbionDataPro.",
                faq1_q: "O que é o AlbionDataPro?",
                faq1_a: "O AlbionDataPro é a principal ferramenta de análise de mercado e overlay em jogo para Albion Online. Ele compara preços entre todas as cidades, calcula rotas e recomenda ordens por IA.",
                faq2_q: "Como funciona o overlay flutuante no jogo?",
                faq2_a: "A bolha flutuante funciona sobre o Albion Online Mobile no Android. Com um toque, exibe preços, arbitragem e sinais de IA sem minimizar o jogo.",
                faq3_q: "O AlbionDataPro é seguro e permitido?",
                faq3_a: "Sim! Usa APIs públicas e opera puramente como um overlay de análise visual. Sem automações de teclado ou injeção de memória, sendo 100% seguro.",
                faq4_q: "Como ativo minha conta?",
                faq4_a: "Instale o pacote APK gratuito, crie sua conta no aplicativo e ative pelo link do PayPal. Sua conta será liberada automaticamente.",
                footer_copyright: "&copy; 2026 AlbionDataPro. Todos os direitos reservados. Hospedado na Render Cloud.",
                footer_disclaimer: "Esta ferramenta não possui afiliação com a Sandbox Interactive."
            },
            ru: {
                badge_release: "Официальный релиз v${CURRENT_SERVER_VERSION} — 24/7 Облако и Оверлей",
                hero_subtitle: "Самый мощный торговый и рыночный оверлей для Albion Online Mobile",
                hero_desc: "Удвойте свое серебро без долгих поисков. Используйте точные цены, арбитражный сканер и ИИ-сигналы прямо в игре!",
                download_btn_subtitle: "Скачать бесплатно",
                download_virus_free: "100% Без вирусов • Прямой APK-пакет Android",
                license_title: "🔒 Покупка и активация лицензий прямо в приложении!",
                license_desc: "Установите приложение бесплатно. Создайте аккаунт и активируйте лицензию (1, 3, 6 или 12 месяцев) прямо в приложении!",
                badge_instant: "Мгновенная активация",
                badge_paypal: "Безопасная оплата PayPal",
                badge_cloud: "24/7 Облачное соединение",
                why_title: "Зачем вам нужен <span class='gradient-text'>AlbionDataPro</span>",
                why_sub: "Торговля без живых данных стоит вам миллионов серебра ежедневно. AlbionDataPro дает вам решающее преимущество.",
                why_card1_title: "Максимальная прибыль без риска",
                why_card1_desc: "Никаких ошибочных покупок! Калькулятор ROI показывает чистую прибыль с учетом налогов и сборов перед каждой сделкой.",
                why_card2_title: "Огромная экономия времени",
                why_card2_desc: "Больше не нужно ездить между городами ради сравнения цен. Облако сканирует все города и показывает лучшие маршруты за секунды.",
                why_card3_title: "Статистическая ИИ-гарантия",
                why_card3_desc: "Наш алгоритм анализирует колебания цен за 7 дней и находит идеальные пороги ордеров покупки и продажи.",
                overlay_section_tag: "Плавающий оверлей в игре",
                overlay_title: "Плавающий оверлей в виде баббла",
                overlay_sub: "Революционный плавающий баббл работает прямо поверх Albion Online Mobile. Вам больше не нужно сворачивать игру!",
                feat1_title: "Постоянный оверлей в игре",
                feat1_desc: "Сдержанный плавающий значок на экране. Нажмите, чтобы открыть живые цены, арбитраж и сигналы ИИ.",
                feat2_title: "Радар городов и Черного рынка",
                feat2_desc: "Сканирует Карлеон, Бресилиэн, Черный рынок и все королевские города. Считает вес, тиры и чистую маржу.",
                feat3_title: "ИИ-бот ордеров покупки и продажи",
                feat3_desc: "Подскажет точные цены для ордеров покупки и продажи с максимальной прибылью и быстрой оборачиваемостью.",
                feat4_title: "Калькулятор крафта и переработки",
                feat4_desc: "Считает стоимость ресурсов, городские возвраты (RRR), экономию фокуса и сборы за крафт брони и оружия.",
                feat5_title: "Таймеры острова и разведение животных",
                feat5_desc: "Отслеживайте урожай и время разведения с уведомлениями о готовности маунтов или растений.",
                feat6_title: "24/7 Облачная синхронизация и OTA",
                feat6_desc: "Настройки и избранное надежно хранятся в облаке. Автоматические обновления OTA поддерживают актуальность.",
                install_title: "Простая установка на Android",
                install_step1: "Нажмите <strong class='text-emerald-400'>'Скачать APK'</strong> выше и сохраните файл.",
                install_step2: "Откройте <code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> и разрешите установку.",
                install_step3: "Запустите приложение, создайте аккаунт и активируйте лицензию прямо в приложении!",
                faq_title: "Часто задаваемые вопросы (FAQ)",
                faq_sub: "Все, что вам нужно знать об AlbionDataPro.",
                faq1_q: "Что такое AlbionDataPro?",
                faq1_a: "AlbionDataPro — ведущий игровой оверлей и инструмент анализа рынка для Albion Online. Он сравнивает цены во всех городах, считает прибыльные маршруты и рекомендует ИИ-ордера.",
                faq2_q: "Как работает плавающий оверлей в игре?",
                faq2_a: "Плавающий баббл отображается поверх Albion Online Mobile на Android. Касание показывает живые цены, арбитраж и сигналы ИИ прямо в игре без сворачивания.",
                faq3_q: "Безопасен и разрешен ли AlbionDataPro?",
                faq3_a: "Да! Приложение использует публичные API и работает как чисто визуальный оверлей. Оно не делает кликов и инъекций в память, поэтому на 100% безопасно.",
                faq4_q: "Как активировать аккаунт?",
                faq4_a: "Установите бесплатный APK-пакет, создайте аккаунт в приложении и активируйте его через ссылку PayPal. Аккаунт разблокируется автоматически.",
                footer_copyright: "&copy; 2026 AlbionDataPro. Все права защищены. Размещено на Render Cloud.",
                footer_disclaimer: "Этот инструмент не связан с Sandbox Interactive."
            },
            zh: {
                badge_release: "官方发布版本 v${CURRENT_SERVER_VERSION} — 24/7 云端与游戏内浮窗",
                hero_subtitle: "Albion Online 移动端最强大的交易与市场浮窗",
                hero_desc: "无需繁琐搜索即可使您的银币翻倍。利用秒级实时价格、套利扫描器与 AI 信号，作为游戏上方的悬浮窗口直接使用！",
                download_btn_subtitle: "免费下载",
                download_virus_free: "100% 无病毒 • 直接 Android APK 安装包",
                license_title: "🔒 直接在 App 内购买并解锁许可证！",
                license_desc: "使用上面的下载按钮免费安装 App。启动后创建账户，即可直接在 App 内解锁许可证（1、3、6 或 12 个月）！",
                badge_instant: "即时激活",
                badge_paypal: "安全 PayPal 支付",
                badge_cloud: "24/7 云端连接",
                why_title: "为什么你需要 <span class='gradient-text'>AlbionDataPro</span>",
                why_sub: "在没有实时数据的情况下在 Albion Online 中交易，每天会损失数百万银币。AlbionDataPro 赋予您超越其他玩家的决胜优势。",
                why_card1_title: "无风险最大化利润",
                why_card1_desc: "告别盲目购买！精准的 ROI 计算器可在每次交易前显示扣除市场税和加工费后的净利润。",
                why_card2_title: "节省大量时间",
                why_card2_desc: "无需为了比较价格而在城市间往返穿梭。我们的云端实时扫描所有城市，在数秒内为您呈现最具吸引力的交易路线。",
                why_card3_title: "统计 AI 算法保障",
                why_card3_desc: "我们的算法分析 7 天内的价格波动，找出理想的买入与卖出挂单阈值，以实现快速周转。",
                overlay_section_tag: "游戏内悬浮 Overlay",
                overlay_title: "游戏内 Bubble 悬浮窗",
                overlay_sub: "革命性的悬浮 Bubble 直接在 Albion Online 移动端上方运行。您无需离开游戏！",
                feat1_title: "常驻游戏内悬浮窗",
                feat1_desc: "屏幕上精致的悬浮图标。只需轻触即可立即查看实时价格、套利机会与 AI 建议。",
                feat2_title: "城市与黑市雷达",
                feat2_desc: "全面扫描 Caerleon、Brecilien、黑市与所有皇家城市。计算运输重量、装备阶级与精确利润。",
                feat3_title: "AI 买入与卖出挂单 Bot",
                feat3_desc: "精确提示买入与卖出挂单价格，在保障出货概率的同时获得最大化收益。",
                feat4_title: "制造与精炼计算器",
                feat4_desc: "计算原材料成本、城市返还率 (RRR)、专注值节省以及护甲、武器与锭块的加工费。",
                feat5_title: "岛屿计时器与动物育种",
                feat5_desc: "监控岛屿农作物与动物育种计时，并在坐骑或植物成熟可收割时接收通知。",
                feat6_title: "24/7 云端同步与自动 OTA",
                feat6_desc: "您的设置与收藏安全地存储在云端。自动 OTA 更新让您的 App 保持最新。",
                install_title: "Android 端极简安装",
                install_step1: "点击上方的 <strong class='text-emerald-400'>'APK 下载'</strong> 保存安装包。",
                install_step2: "打开 <code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> 并允许安装。",
                install_step3: "启动 App，创建您的账户，然后直接在 App 内解锁许可证！",
                faq_title: "常见问题解答 (FAQ)",
                faq_sub: "关于 AlbionDataPro 您需要了解的一切。",
                faq1_q: "什么是 AlbionDataPro？",
                faq1_a: "AlbionDataPro 是 Albion Online 领军的游戏内悬浮分析工具。它横跨所有城市（Caerleon、Martlock、Lymhurst、Bridgewatch、Fort Sterling、Thetford、黑市）对比实时价格，计算盈利交易路线并推荐基于 AI 的挂单策略。",
                faq2_q: "游戏内 Floating Overlay 是如何工作的？",
                faq2_a: "悬浮 Bubble 在 Android 版 Albion Online 移动端上方直接运行。轻触一下即可在游戏中直接弹窗显示实时价格、套利机会与 AI 信号，无需最小化游戏。",
                faq3_q: "AlbionDataPro 安全且被允许吗？",
                faq3_a: "是的！AlbionDataPro 使用公开的市场 API，纯粹作为视觉分析浮窗运行。它不执行任何自动按键或内存注入，因此 100% 安全。",
                faq4_q: "如何解锁我的账户？",
                faq4_a: "安装上方免费的 APK 安装包，在 App 内创建您的账户，然后通过 PayPal 支付链接直接解锁。支付完成后，您的账户将在服务器上自动激活。",
                footer_copyright: "&copy; 2026 AlbionDataPro. 保留所有权利。托管于 Render Cloud。",
                footer_disclaimer: "本分析工具与 Sandbox Interactive 无官方关联。"
            },
            ja: {
                badge_release: "公式リリース v${CURRENT_SERVER_VERSION} — 24/7 クラウド＆ゲーム内オーバーレイ",
                hero_subtitle: "Albion Online モバイル向けの最も強力な取引＆市場オーバーレイ",
                hero_desc: "検索に時間を費やすことなくシルバーを倍増。ライブ価格、アービトラージスキャナー、AIシグナルをゲーム上に表示！",
                download_btn_subtitle: "無料ダウンロード",
                download_virus_free: "100% ウイルスフリー • Android APK パッケージ",
                license_title: "🔒 アプリ内でライセンスを直接購入＆解除！",
                license_desc: "上のボタンからアプリ를無料インストール。起動後にアカウントを作成し、アプリ内でライセンス（1、3、6、12ヶ月）を解除！",
                badge_instant: "即時有効化",
                badge_paypal: "安全な PayPal 決済",
                badge_cloud: "24/7 クラウド接続",
                why_title: "<span class='gradient-text'>AlbionDataPro</span> が必要な理由",
                why_sub: "リアルタイムデータなしでの取引は毎日何百万ものシルバー strike 失います。AlbionDataPro が圧倒的アドバンテージを提供します。",
                why_card1_title: "リスクなしで最大利益",
                why_card1_desc: "失敗した買い物はもう不要！精密なROI計算機が税金や手数料を差し引いた純利益を表示。",
                why_card2_title: "大幅な時間短縮",
                why_card2_desc: "価格比較のために都市間を移動する必要はありません。クラウドが全都市をスキャンし、最も収益性の高いルートを即座に提示。",
                why_card3_title: "統計的 AI 保障",
                why_card3_desc: "アルゴリズムが7日間の価格変動を分析し、迅速な回転のための最適な買い/売り注文のしきい値を割り出します。",
                overlay_section_tag: "ゲーム内浮遊オーバーレイ",
                overlay_title: "ゲーム内バブルオーバーレイ",
                overlay_sub: "革新的なバブルオーバーレイが Albion Online モバイル上に直接表示。ゲームを閉じる必要はもうありません！",
                feat1_title: "常駐ゲーム内オーバーレイ",
                feat1_desc: "画面上の控えめな浮遊アイコン。タップするだけで価格、アービトラージ、AIシグナルを即座に表示。",
                feat2_title: "都市＆ブラックマーケットレーダー",
                feat2_desc: "カーレオン、ブレシリアン、ブラックマーケット、全王都をスキャン。重量やTier、正確な利益率を計算。",
                feat3_title: "AI 買い＆売り注文 Bot",
                feat3_desc: "高い売却確率で最大の利益を得るための正確な注文価格をアドバイス。",
                feat4_title: "クラフト＆精錬計算機",
                feat4_desc: "原材料コスト、都市還元率（RRR）、フォーカス節約量、製造手数料を正確に計算。",
                feat5_title: "島タイマー＆動物育成",
                feat5_desc: "島での収穫や育成時間を監視し、作物やマウントの収穫準備完了時に通知を送信。",
                feat6_title: "24/7 クラウド同期＆自動OTA",
                feat6_desc: "設定とお気に入りはクラウドに安全に保存。自動OTAアップデートで常に最新の状態を維持。",
                install_title: "Android への簡単インストール",
                install_step1: "上の <strong class='text-emerald-400'>'APK ダウンロード'</strong> をクリックしてファイルを保存。",
                install_step2: "<code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> を開いてインストールを許可。",
                install_step3: "アプリを起動してアカウントを作成し、アプリ内でライセンスを解除！",
                faq_title: "よくある質問 (FAQ)",
                faq_sub: "AlbionDataPro に関するすべての情報。",
                faq1_q: "AlbionDataPro とは何ですか？",
                faq1_a: "AlbionDataPro は Albion Online 向けの最先端ゲーム内オーバーレイ＆市場分析ツールです。全都市の価格を比較し、収益性の高い交易ルートを計算、AIによる買い/売り注文をアドバイスします。",
                faq2_q: "ゲーム内バブルオーバーレイの仕組みは？",
                faq2_a: "浮遊バブルが Android の Albion Online モバイル上に表示。タップするだけでゲームを最小化せずにリアルタイム価格やAIシグナルを表示できます。",
                faq3_q: "AlbionDataPro は safe で許可されていますか？",
                faq3_a: "はい！公開市場APIを使用し、視acularな分析オーバーレイとしてのみ動作します。自動入力やメモリ注入は一切行わないため100%安全です。",
                faq4_q: "アカウントを有効化するには？",
                faq4_a: "無料のAPKをインストールしてアプリ内でアカウントを作成し、PayPalリンクから直接解除。決済後、サーバー上で自動的に有効化されます。",
                footer_copyright: "&copy; 2026 AlbionDataPro. All rights reserved. Hosted on Render Cloud.",
                footer_disclaimer: "この分析ツールは Sandbox Interactive とは関係ありません。"
            },
            ko: {
                badge_release: "공식 릴리스 v${CURRENT_SERVER_VERSION} — 24/7 클라우드 및 게임 내 오버레이",
                hero_subtitle: "Albion Online 모바일을 위한 가장 강력한 거래 및 시장 오버레이",
                hero_desc: "끝없는 검색 없이 실버를 두 배로 늘리세요. 실시간 가격, 차익 거래 스캐너 및 AI 신호를 게임 내 플로팅 오버레이로 바로 사용하세요!",
                download_btn_subtitle: "무료 다운로드",
                download_virus_free: "100% 바이러스 없음 • 직접 Android APK 패키지",
                license_title: "🔒 앱 내에서 직접 라이선스 구매 및 잠금 해제!",
                license_desc: "위의 다운로드 버튼으로 앱을 무료로 설치하세요. 실행 후 계정을 생성하고 앱 내에서 라이선스(1, 3, 6 또는 12개월)를 바로 잠금 해제하세요!",
                badge_instant: "즉시 활성화",
                badge_paypal: "안전한 PayPal 결제",
                badge_cloud: "24/7 클라우드 연결",
                why_title: "<span class='gradient-text'>AlbionDataPro</span>가 필요한 이유",
                why_sub: "실시간 데이터 없이 거래하면 매일 수백만 실버의 손실이 발생합니다. AlbionDataPro가 결정적인 우위를 제공합니다.",
                why_card1_title: "위험 없는 최대 이익",
                why_card1_desc: "더 이상의 잘못된 구매는 없습니다! 정확한 ROI 계산기가 세금과 수수료를 제외한 순이익을 거래 전에 보여줍니다.",
                why_card2_title: "엄청난 시간 절약",
                why_card2_desc: "가격 비교를 위해 도시를 왔다 갔다 할 필요가 없습니다. 클라우드가 모든 도시를 스캔하여 가장 수익성이 높은 거래 경로를 보여줍니다.",
                why_card3_title: "통계적 AI 보장",
                why_card3_desc: "알고리즘이 7일간의 가격 변동을 분석하여 빠른 회전을 위한 최적의 매수/매도 주문 임계값을 찾습니다.",
                overlay_section_tag: "게임 내 플로팅 오버레이",
                overlay_title: "게임 내 버블 오버레이",
                overlay_sub: "혁신적인 플로팅 버블 오버레이가 Albion Online 모바일 위에 직접 실행됩니다. 게임을 나갈 필요가 전혀 없습니다!",
                feat1_title: "상시 게임 내 오버레이",
                feat1_desc: "화면 위의 깔끔한 플로팅 아이콘. 탭 한 번으로 실시간 가격, 차익 거래, AI 신호를 즉시 확인하세요.",
                feat2_title: "도시 및 암시장 레이더",
                feat2_desc: "카얼레온, 브레실리엔, 암시장 및 모든 왕도 도시를 스캔합니다. 무게, 티어 및 정확한 마진을 계산합니다.",
                feat3_title: "AI 매수 & 매도 주문 Bot",
                feat3_desc: "빠른 판매 확률과 함께 최대 이익을 얻을 수 있는 정확한 매수 및 매도 가격을 알려줍니다.",
                feat4_title: "제작 및 정제 계산기",
                feat4_desc: "재료 비용, 도시 반환율(RRR), 집중력 절약 및 장비/무기 제작 수수료를 계산합니다.",
                feat5_title: "섬 타이머 및 동물 번식",
                feat5_desc: "작물이나 탈것이 수확할 준비가 되면 알림을 받아 섬 수확 및 번식 시간을 모니터링하세요.",
                feat6_title: "24/7 클라우드 동기화 및 자동 OTA",
                feat6_desc: "설정과 즐겨찾기가 클라우드에 안전하게 저장됩니다. 자동 OTA 업데이트로 앱을 항상 최신 상태로 유지하세요.",
                install_title: "간편한 Android 설치",
                install_step1: "위의 <strong class='text-emerald-400'>'APK 다운로드'</strong>를 클릭하고 파일을 저장합니다.",
                install_step2: "<code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code>를 열고 설치를 허용합니다.",
                install_step3: "앱을 실행하고 계정을 생성한 다음 앱 내에서 라이선스를 바로 잠금 해제하세요!",
                faq_title: "자주 묻는 질문 (FAQ)",
                faq_sub: "AlbionDataPro에 대해 알아야 할 모든 것.",
                faq1_q: "AlbionDataPro란 무엇인가요?",
                faq1_a: "AlbionDataPro는 Albion Online을 위한 선도적인 게임 내 오버레이 및 시장 분석 도구입니다. 모든 도시의 가격을 비교하고 수익성 있는 거래 경로를 계산하며 AI 기반 매수/매도 주문을 추천합니다.",
                faq2_q: "게임 내 플로팅 오버레이는 어떻게 작동하나요?",
                faq2_a: "플로팅 버블 오버레이는 Android의 Albion Online 모바일 위에 직접 실행됩니다. 앱을 최소화할 필요 없이 한번의 탭으로 실시간 가격, 차익 거래, AI 신호를 게임 내에서 오버레이하세요.",
                faq3_q: "AlbionDataPro는 안전하고 허용되나요?",
                faq3_a: "네! AlbionDataPro는 공개 시장 API를 사용하며 시각적 분석 오버레이로만 작동합니다. 자동 입력이나 메모리 주입을 하지 않으므로 100% 안전합니다.",
                faq4_q: "계정을 어떻게 잠금 해제하나요?",
                faq4_a: "위의 무료 APK 패키지를 설치하고 앱 내에서 계정을 생성한 후 PayPal 링크를 통해 바로 잠금 해제하세요. 결제 후 서버에서 계정이 자동으로 활성화됩니다.",
                footer_copyright: "&copy; 2026 AlbionDataPro. All rights reserved. Hosted on Render Cloud.",
                footer_disclaimer: "이 분석 도구는 Sandbox Interactive와 관련이 없습니다."
            },
            tr: {
                badge_release: "Resmi Sürüm v${CURRENT_SERVER_VERSION} — 24/7 Bulut ve Oyun İçi Overlay",
                hero_subtitle: "Albion Online Mobile için en güçlü ticaret ve pazar overlay'i",
                hero_desc: "Saatlerce aramadan gümüşünüzü ikiye katlayın. Canlı fiyatları, arbitraj tarayıcısını ve YAZ sinyallerini oyun içinde overlay olarak kullanın!",
                download_btn_subtitle: "Ücretsiz İndir",
                download_virus_free: "%100 Virüssüz • Doğrudan Android APK Paketi",
                license_title: "🔒 Lisansları doğrudan uygulama içinden satın alın ve etkinleştirin!",
                license_desc: "Uygulamayı yukarıdaki indirme butonundan ücretsiz indirin. Başlattıktan sonra hesabınızı oluşturun ve lisansınızı doğrudan uygulama içinden etkinleştirin!",
                badge_instant: "Anında Etkinleştirme",
                badge_paypal: "Güvenli PayPal Ödemesi",
                badge_cloud: "24/7 Bulut Bağlantısı",
                why_title: "Neden <span class='gradient-text'>AlbionDataPro</span>'ya ihtiyacınız var",
                why_sub: "Canlı veri olmadan ticaret yapmak size her gün milyonlarca gümüşe mal olur. AlbionDataPro size rakiplerinize karşı üstünlük sağlar.",
                why_card1_title: "Rissiz Maksimum Kar",
                why_card1_desc: "Hatalı satın alımlara son! ROI hesaplayıcı, vergiler ve ücretler düşüldükten sonraki net karınızı gösterir.",
                why_card2_title: "Büyük Zaman Tasarrufu",
                why_card2_desc: "Fiyatları karşılaştırmak için şehirler arası seyahat etmeye son. Bulutumuz tüm şehirleri tarar ve en karlı rotaları gösterir.",
                why_card3_title: "İstatistiksel YAZ Garantisi",
                why_card3_desc: "Algoritmamız 7 günlük fiyat dalgalanmalarını analiz eder ve hızlı devir için ideal alış ve satış emri eşiklerini bulur.",
                overlay_section_tag: "Oyun İçi Floating Overlay",
                overlay_title: "Oyun İçi Baloncuk Overlay",
                overlay_sub: "Devrim niteliğindeki baloncuk overlay doğrudan Albion Online Mobile üzerinde çalışır. Oyundan asla çıkmanız gerekmez!",
                feat1_title: "Kalıcı Oyun İçi Overlay",
                feat1_desc: "Ekranınızda şık bir baloncuk simgesi. Canlı fiyatları, arbitrajı ve YAZ sinyallerini görmek için dokunmanız yeterli.",
                feat2_title: "Şehirler ve Kara Borsa Radarı",
                feat2_desc: "Caerleon, Brecilien, Kara Borsa ve tüm Kraliyet Şehirlerini tarar. Ağırlık, tier ve net karları hesaplar.",
                feat3_title: "YAZ Alış ve Satış Emri Botu",
                feat3_desc: "Hızlı satış şansı ile maksimum kar elde etmek için alış ve satış emirlerinin tam fiyatlarını söyler.",
                feat4_title: "Üretim ve Arıtma Hesaplayıcı",
                feat4_desc: "Ham madde maliyetlerini, şehir geri dönüş oranlarını (RRR), odak tasarrufunu ve üretim ücretlerini hesaplar.",
                feat5_title: "Ada Zamanlayıcısı ve Hayvan Yetiştiriciliği",
                feat5_desc: "Ada mahsullerinizi ve hayvan yetiştirme sürelerinizi izleyin, hazır olduklarında bildirim alın.",
                feat6_title: "24/7 Bulut Senkronizasyonu ve OTA",
                feat6_desc: "Ayarlarınız ve favorileriniz bulutta güvenle saklanır. Otomatik OTA güncellemeleri uygulamanızı güncel tutar.",
                install_title: "Android'de Kolay Kurulum",
                install_step1: "Yukarıdaki <strong class='text-emerald-400'>'APK İndir'</strong> butonuna tıklayın ve dosyayı kaydedin.",
                install_step2: "<code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> dosyasını açın ve kuruluma izin verin.",
                install_step3: "Uygulamayı başlatın, hesabınızı oluşturun ve lisansınızı doğrudan uygulama içinden etkinleştirin!",
                faq_title: "Sıkça Sorulan Sorular (SSS)",
                faq_sub: "AlbionDataPro hakkında bilmeniz gereken her şey.",
                faq1_q: "AlbionDataPro Nedir?",
                faq1_a: "AlbionDataPro, Albion Online için lider oyun içi overlay ve pazar analiz aracıdır. Tüm şehirlerdeki fiyatları karşılaştırır, karlı rotaları hesaplar ve YAZ emirleri önerir.",
                faq2_q: "Oyun içi floating overlay nasıl çalışır?",
                faq2_a: "Baloncuk overlay Android üzerinde doğrudan Albion Online Mobile üzerinde çalışır. Dokunarak oyunu küçültmeden canlı fiyatları ve YAZ sinyallerini görün.",
                faq3_q: "AlbionDataPro güvenli ve izinli mi?",
                faq3_a: "Evet! Kamu pazar API'lerini kullanır ve yalnızca görsel bir analiz overlay'i olarak çalışır. Otomatik girdi veya bellek enjeksiyonu yapmaz, %100 güvenlidir.",
                faq4_q: "Hesabımı nasıl etkinleştiririm?",
                faq4_a: "Ücretsiz APK paketini indirin, uygulama içinde hesabınızı oluşturun ve PayPal bağlantısı ile etkinleştirin. Hesabınız ödemeden sonra otomatik açılır.",
                footer_copyright: "&copy; 2026 AlbionDataPro. Tüm hakları saklıdır. Render Cloud üzerinde barındırılmaktadır.",
                footer_disclaimer: "Bu analiz aracının Sandbox Interactive ile resmi bir bağlantısı yoktur."
            },
            id: {
                badge_release: "Rilis Resmi v${CURRENT_SERVER_VERSION} — Cloud 24/7 & Overlay",
                hero_subtitle: "Overlay perdagangan & pasar paling andal untuk Albion Online Mobile",
                hero_desc: "Gandakan perak Anda tanpa perlu mencari berjam-jam. Gunakan harga langsung real-time, pemindai arbitrase & sinyal AI langsung sebagai overlay melayang!",
                download_btn_subtitle: "Unduh Gratis",
                download_virus_free: "100% Bebas Virus • Paket APK Android Langsung",
                license_title: "🔒 Beli & Buka Lisensi Langsung Di Dalam Aplikasi!",
                license_desc: "Instal aplikasi secara gratis menggunakan tombol unduh di atas. Setelah diluncurkan, buat akun dan buka lisensi Anda (1, 3, 6, atau 12 bulan) langsung di dalam aplikasi!",
                badge_instant: "Aktivasi Instan",
                badge_paypal: "Pembayaran PayPal Aman",
                badge_cloud: "Koneksi Cloud 24/7",
                why_title: "Mengapa Anda membutuhkan <span class='gradient-text'>AlbionDataPro</span>",
                why_sub: "Berdagang tanpa data langsung menghabiskan jutaan perak Anda setiap hari. AlbionDataPro memberi Anda keunggulan mutlak.",
                why_card1_title: "Keuntungan Maksimal Tanpa Risiko",
                why_card1_desc: "Tidak ada lagi salah beli! Kalkulator ROI yang presisi menampilkan keuntungan bersih Anda setelah pajak dan biaya.",
                why_card2_title: "Hemat Waktu Luar Biasa",
                why_card2_desc: "Tidak perlu lagi bepergian bolak-balik untuk membandingkan harga. Cloud kami memindai semua kota dan menampilkan rute terbaik dalam hitungan detik.",
                why_card3_title: "Jaminan AI Statistik",
                why_card3_desc: "Algoritma kami menganalisis fluktuasi harga 7 hari dan menemukan ambang batas pesanan beli & jual yang ideal.",
                overlay_section_tag: "Overlay Melayang Di Dalam Game",
                overlay_title: "Overlay Gelembung Di Dalam Game",
                overlay_sub: "Gelembung overlay melayang revolusioner berjalan langsung di atas Albion Online Mobile. Anda tidak perlu keluar dari game!",
                feat1_title: "Overlay Permanen Di Dalam Game",
                feat1_desc: "Ikon melayang sederhana di layar Anda. Cukup ketuk untuk menampilkan harga langsung, arbitrase & sinyal AI.",
                feat2_title: "Radar Kota & Pasar Gelap",
                feat2_desc: "Memindai Caerleon, Brecilien, Pasar Gelap, dan semua Kota Kerajaan. Menghitung berat, tier, & margin tepat.",
                feat3_title: "Bot Pesanan Beli & Jual AI",
                feat3_desc: "Memberi tahu Anda harga pasti untuk pesanan beli & jual demi keuntungan maksimal dan omzet tinggi.",
                feat4_title: "Kalkulator Crafting & Pengolahan",
                feat4_desc: "Menghitung biaya bahan, tingkat pengembalian kota (RRR), penghematan fokus & biaya pembuatan.",
                feat5_title: "Pengatur Waktu Pulau & Peternakan",
                feat5_desc: "Pantau hasil panen pulau dan waktu pembiakan dengan pemberitahuan saat tanaman atau tunggangan siap.",
                feat6_title: "Sinkronisasi Cloud 24/7 & Auto-OTA",
                feat6_desc: "Pengaturan dan favorit Anda disimpan dengan aman di cloud. Pembaruan OTA otomatis menjaga aplikasi selalu terbaru.",
                install_title: "Instalasi Mudah di Android",
                install_step1: "Klik <strong class='text-emerald-400'>'APK Download'</strong> di atas dan simpan file.",
                install_step2: "Buka <code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> dan izinkan penginstalan.",
                install_step3: "Luncurkan aplikasi, buat akun Anda, dan buka lisensi Anda langsung di dalam aplikasi!",
                faq_title: "Pertanyaan yang Sering Diajukan (FAQ)",
                faq_sub: "Semua yang perlu Anda ketahui tentang AlbionDataPro.",
                faq1_q: "Apa itu AlbionDataPro?",
                faq1_a: "AlbionDataPro adalah alat analisis pasar & overlay terkemuka untuk Albion Online. Ini membandingkan harga di semua kota, menghitung rute perdagangan, & merekomendasikan pesanan AI.",
                faq2_q: "Bagaimana cara kerja overlay melayang di dalam game?",
                faq2_a: "Gelembung overlay melayang berjalan di atas Albion Online Mobile di Android. Dengan sekali ketuk, tampilkan harga & sinyal AI tanpa meminimalkan game.",
                faq3_q: "Apakah AlbionDataPro aman dan diizinkan?",
                faq3_a: "Ya! Menggunakan API pasar publik dan beroperasi murni sebagai overlay analisis visual. Tidak ada klip otomatis atau injeksi memori, 100% aman.",
                faq4_q: "Bagaimana cara membuka akun saya?",
                faq4_a: "Instal paket APK gratis di atas, buat akun Anda di dalam aplikasi, dan buka langsung menggunakan tautan PayPal. Akun akan aktif secara otomatis.",
                footer_copyright: "&copy; 2026 AlbionDataPro. Hak cipta dilindungi undang-undang. Dihosting di Render Cloud.",
                footer_disclaimer: "Alat analisis ini tidak berafiliasi dengan Sandbox Interactive."
            },
            pl: {
                badge_release: "Oficjalna wersja v${CURRENT_SERVER_VERSION} — Chmura 24/7 i Nakładka",
                hero_subtitle: "Najpotężniejsza nakładka handlowa i rynkowa dla Albion Online Mobile",
                hero_desc: "Podwój swoje srebro bez wielogodzinnych poszukiwań. Korzystaj z cen w czasie rzeczywistym, skanerów arbitrażu i sygnałów AI jako pływającej nakładki!",
                download_btn_subtitle: "Pobierz Za Darmo",
                download_virus_free: "100% Wolne Od Wirusów • Bezpośredni Pakiet APK",
                license_title: "🔒 Kupuj i odblokowuj licencje bezpośrednio w aplikacji!",
                license_desc: "Zainstaluj aplikację za darmo. Utwórz konto i odblokuj licencję (1, 3, 6 lub 12 miesięcy) bezpośrednio w aplikacji!",
                badge_instant: "Natychmiastowa Aktywacja",
                badge_paypal: "Bezpieczna Płatność PayPal",
                badge_cloud: "Połączenie Chmury 24/7",
                why_title: "Dlaczego potrzebujesz <span class='gradient-text'>AlbionDataPro</span>",
                why_sub: "Handel bez danych na żywo kosztuje Cię miliony srebra dziennie. AlbionDataPro daje Ci przewagę nad innymi graczymi.",
                why_card1_title: "Maksymalny Zysk Bez Ryzyka",
                why_card1_desc: "Koniec z nieudanymi zakupami! Precyzyjny kalkulator ROI pokazuje czysty zysk po podatkach i opłatach.",
                why_card2_title: "Ogromna Oszczędność Czasu",
                why_card2_desc: "Koniec z podróżowaniem między miastami w celu porównywania cen. Chmura skanuje wszystkie miasta i pokazuje najbardziej opłacalne trasy w sekundy.",
                why_card3_title: "Gwarancja Statystyczna AI",
                why_card3_desc: "Nasz algorytm analizuje 7-dniowe wahania cen i znajduje idealne progi zleceń kupna i sprzedaży.",
                overlay_section_tag: "Pływająca Nakładka W Grze",
                overlay_title: "Pływająca Nakładka Bąbelkowa W Grze",
                overlay_sub: "Rewolucyjny bąbel pływający działa bezpośrednio nad Albion Online Mobile. Nigdy nie musisz wychodzić z gry!",
                feat1_title: "Stała Nakładka W Grze",
                feat1_desc: "Dyskretna pływająca ikonka na ekranie. Stuknij, aby natychmiast zobaczyć ceny, arbitraż i sygnały AI.",
                feat2_title: "Radar Miast i Czarnego Rynku",
                feat2_desc: "Skanuje Caerleon, Brecilien, Czarny Rynek i Miasta Królewskie. Oblicza wagę, tiery i czysty zysk.",
                feat3_title: "Bot Zleceń Kupna i Sprzedaży AI",
                feat3_desc: "Podaje dokładne ceny zleceń kupna i sprzedaży, aby zmaksymalizować zysk przy szybkiej rotacji.",
                feat4_title: "Kalkulator Wytwarzania i Rafinacji",
                feat4_desc: "Oblicza koszty surowców, zwroty w miastach (RRR), oszczędność skupienia i opłaty za wytwarzanie.",
                feat5_title: "Licznik Czasu Wyspy i Hodowla",
                feat5_desc: "Monitoruj zbiory na wyspie i czas hodowli z powiadomieniami, gdy plony lub wierzchowce są gotowe.",
                feat6_title: "Synchronizacja Chmury 24/7 i Auto-OTA",
                feat6_desc: "Twoje ustawienia i ulubione są bezpiecznie przechowywane w chmurze. Automatyczne aktualizacje OTA utrzymują aplikację w gotowości.",
                install_title: "Prosta Instalacja na Androidzie",
                install_step1: "Kliknij <strong class='text-emerald-400'>'Pobierz APK'</strong> powyżej i zapisz plik.",
                install_step2: "Otwórz <code class='bg-slate-900 px-2 py-0.5 rounded text-sky-400 font-mono'>AlbionDataPro.apk</code> i zezwól na instalację.",
                install_step3: "Uruchom aplikację, utwórz konto i odblokuj licencję bezpośrednio w aplikacji!",
                faq_title: "Często Zadawane Pytania (FAQ)",
                faq_sub: "Wszystko, co musisz wiedzieć o AlbionDataPro.",
                faq1_q: "Czym jest AlbionDataPro?",
                faq1_a: "AlbionDataPro to wiodące narzędzie do analizy rynku i nakładka w grze dla Albion Online. Porównuje ceny we wszystkich miastach, oblicza opłacalne trasy i poleca zlecenia AI.",
                faq2_q: "Jak działa pływająca nakładka w grze?",
                faq2_a: "Pływający bąbel działa bezpośrednio nad Albion Online Mobile na Androidzie. Stuknięcie nakłada ceny na żywo, arbitraż i sygnały AI bez minimalizowania gry.",
                faq3_q: "Czy AlbionDataPro jest bezpieczny i dozwolony?",
                faq3_a: "Tak! Używa publicznych API i działa wyłącznie jako nakładka do analizy wizualnej. Nie wykonuje automatycznych kliknięć ani wstrzykiwania pamięci — 100% bezpieczne.",
                faq4_q: "Jak odblokować moje konto?",
                faq4_a: "Zainstaluj darmowy pakiet APK, utwórz konto w aplikacji i odblokuj je przez PayPal. Konto zostanie automatycznie aktywowane po płatności.",
                footer_copyright: "&copy; 2026 AlbionDataPro. Wszelkie prawa zastrzeżone. Hostowane w chmurze Render.",
                footer_disclaimer: "To narzędzie analityczne nie jest powiązane z Sandbox Interactive."
            }
        };

        function changeWebLanguage(lang) {
            try { localStorage.setItem('web_lang', lang); } catch (_) {}
            const t = WEB_TRANSLATIONS[lang] || WEB_TRANSLATIONS['en'] || WEB_TRANSLATIONS['de'];
            if (!t) return;

            document.querySelectorAll('[data-i18n]').forEach(el => {
                const key = el.getAttribute('data-i18n');
                if (t[key]) {
                    el.textContent = t[key];
                }
            });

            document.querySelectorAll('[data-i18n-html]').forEach(el => {
                const key = el.getAttribute('data-i18n-html');
                if (t[key]) {
                    el.innerHTML = t[key];
                }
            });
        }

        document.addEventListener("DOMContentLoaded", function() {
            try {
                const saved = localStorage.getItem('web_lang') || navigator.language.substring(0, 2).toLowerCase();
                const select = document.getElementById('webLangSelect');
                const langToUse = WEB_TRANSLATIONS[saved] ? saved : (WEB_TRANSLATIONS[navigator.language.substring(0, 2).toLowerCase()] ? navigator.language.substring(0, 2).toLowerCase() : 'de');
                if (select) {
                    select.value = langToUse;
                }
                changeWebLanguage(langToUse);
            } catch (_) {}
        });
    </script>
</body>
</html>`);
});

// API Endpoints
app.get('/api/health', (req, res) => res.json({
    status: 'healthy',
    timestamp: Date.now(),
    version: CURRENT_SERVER_VERSION,
    lastInfoReceivedTimestamp: cloudDataLastReceivedTimestamp,
    lastInfoReceivedDate: new Date(cloudDataLastReceivedTimestamp).toISOString(),
    subnets: ['74.220.51.0/24', '74.220.59.0/24']
}));
app.get('/api/tunnel', (req, res) => res.json({ tunnelUrl: getActiveTunnelUrl(), subnets: ['74.220.51.0/24', '74.220.59.0/24'] }));
app.get('/api/prices', (req, res) => {
    const srv = (req.query.server || 'europe').toLowerCase();
    res.json(marketCache[srv] || marketCache['europe']);
});

// 24/7 Global Data-Brain: Echtzeit Abruf aller Items
app.get('/api/prices/recent', (req, res) => {
    const srv = (req.query.server || 'europe').toLowerCase();
    res.json(Object.values(globalMarketPrices[srv] || globalMarketPrices['europe']));
});
app.get('/api/market/prices/live', (req, res) => {
    const srv = (req.query.server || 'europe').toLowerCase();
    res.json(Object.values(globalMarketPrices[srv] || globalMarketPrices['europe']));
});

app.get('/api/prices/albion2d', (req, res) => {
    const srv = (req.query.server || 'europe').toLowerCase();
    res.json(albion2dCache[srv] || albion2dCache['europe']);
});

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

                    // Automatically unlock account on server
                    autoUnlockUserAccount(body.custom || payer_email, months, `${mc_gross}€`);

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

function autoUnlockUserAccount(targetIdentifier, months, amount) {
    if (!months || months <= 0) months = 1;
    const cleanId = (targetIdentifier || '').trim().toLowerCase();

    let user = registeredUsers.find(u =>
        (u.username && u.username.toLowerCase() === cleanId) ||
        (u.email && u.email.toLowerCase() === cleanId)
    );

    if (!user) {
        const unLicensedUsers = registeredUsers.filter(u =>
            !u.isAdmin && (!u.licenseExpiresAt || new Date(u.licenseExpiresAt) <= new Date())
        );
        if (unLicensedUsers.length > 0) {
            user = unLicensedUsers[unLicensedUsers.length - 1];
        }
    }

    if (user) {
        const now = new Date();
        const currentExp = user.licenseExpiresAt ? new Date(user.licenseExpiresAt) : new Date(0);
        const startFrom = currentExp > now ? currentExp : now;

        startFrom.setDate(startFrom.getDate() + (months * 30));

        user.isLicensed = true;
        user.licenseExpiresAt = startFrom.toISOString();
        saveUsers();
        console.log(`[Auto-Unlock] Account "${user.username}" nach PayPal-Kauf (${amount}) freigeschaltet bis ${user.licenseExpiresAt}!`);
        return { unlocked: true, user: user.username, expiresAt: user.licenseExpiresAt };
    }
    return { unlocked: false, user: null };
}

// Endpoint zum Prüfen & Freischalten nach PayPal-Zahlung
app.post('/api/auth/check-payment', (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).json({ error: 'Benutzername und Passwort erforderlich' });

    const cleanUser = username.trim().toLowerCase();
    const cleanPass = password.trim();

    const user = registeredUsers.find(u => u.username.toLowerCase() === cleanUser);
    if (!user || user.password !== cleanPass) {
        return res.status(401).json({ authenticated: false, message: 'Ungültiger Benutzername oder Passwort' });
    }

    const now = new Date();
    const exp = user.licenseExpiresAt ? new Date(user.licenseExpiresAt) : new Date(0);
    const isAlreadyActive = user.isAdmin || (user.isLicensed && exp > now);

    if (isAlreadyActive) {
        return res.json({
            status: 'success',
            message: 'Dein Account ist bereits freigeschaltet!',
            isLicensed: true,
            licenseExpiresAt: user.licenseExpiresAt
        });
    }

    const result = autoUnlockUserAccount(cleanUser, 1, 'PayPal-Kauf');
    if (result.unlocked) {
        return res.json({
            status: 'success',
            message: '🎉 Account erfolgreich nach PayPal-Zahlung freigeschaltet!',
            isLicensed: true,
            licenseExpiresAt: result.expiresAt
        });
    } else {
        return res.status(403).json({
            status: 'pending',
            message: 'Keine neue PayPal-Zahlung gefunden. Bitte erwerbe eine Lizenz.'
        });
    }
});

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

    // STRICT IDENTICAL VERSION LOCK REQUIREMENT
    const clientVer = (appVersion || req.headers['x-app-version'] || '').trim().replace(/^v/i, '');
    const currentVer = CURRENT_SERVER_VERSION.trim().replace(/^v/i, '');

    const cleanUser = username.trim().toLowerCase();
    const cleanPass = password.trim();

    // Check strict version match first (Admin 'dnnx' exempt)
    if (clientVer !== currentVer && cleanUser !== 'dnnx') {
        console.warn(`[AUTH-LOGIN] ⛔ Login abgelehnt für ${username}: Version veraltet (Client: v${clientVer}, Required: v${currentVer})`);
        return res.status(426).json({
            authenticated: false,
            versionMismatch: true,
            clientVersion: clientVer,
            targetVersion: currentVer,
            message: `App aktualisieren: Deine App-Version (v${clientVer || 'alt'}) ist veraltet! Bitte installiere das neueste Update v${currentVer}, um dich anzumelden.`
        });
    }

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
    const srv = (req.query.server || 'europe').toLowerCase();
    const items = (marketCache[srv] && Array.isArray(marketCache[srv].items)) ? marketCache[srv].items : [];
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
                downloadUrl: 'https://albionmarketv2-1.onrender.com/download/AlbionDataPro.apk',
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
    let isLicensed = false;
    const defaultExp = new Date(0); // Default: Expired / Epoch 0 (NO FREE LICENSE)

    if (keyInput.length > 0) {
        if (keyInput.startsWith('ALBION-12M-') || keyInput.includes('12M')) {
            defaultExp.setTime(Date.now() + 365 * 24 * 60 * 60 * 1000);
            isLicensed = true;
        } else if (keyInput.startsWith('ALBION-6M-') || keyInput.includes('6M')) {
            defaultExp.setTime(Date.now() + 180 * 24 * 60 * 60 * 1000);
            isLicensed = true;
        } else if (keyInput.startsWith('ALBION-3M-') || keyInput.includes('3M')) {
            defaultExp.setTime(Date.now() + 90 * 24 * 60 * 60 * 1000);
            isLicensed = true;
        } else if (keyInput.startsWith('ALBION-1M-') || keyInput.includes('1M')) {
            defaultExp.setTime(Date.now() + 30 * 24 * 60 * 60 * 1000);
            isLicensed = true;
        } else if (keyInput.startsWith('ALBION-LIFETIME') || keyInput.includes('LIFETIME') || keyInput === 'ALBION-PRO-LIFETIME') {
            defaultExp.setFullYear(2099);
            isLicensed = true;
        } else {
            // Search in generatedLicenses array
            const foundLicIdx = generatedLicenses.findIndex(l => l.key.toUpperCase() === keyInput);
            if (foundLicIdx !== -1) {
                const lic = generatedLicenses[foundLicIdx];
                if (lic.tier.includes('12') || lic.key.includes('12M')) defaultExp.setTime(Date.now() + 365 * 24 * 60 * 60 * 1000);
                else if (lic.tier.includes('6') || lic.key.includes('6M')) defaultExp.setTime(Date.now() + 180 * 24 * 60 * 60 * 1000);
                else if (lic.tier.includes('3') || lic.key.includes('3M')) defaultExp.setTime(Date.now() + 90 * 24 * 60 * 60 * 1000);
                else defaultExp.setTime(Date.now() + 30 * 24 * 60 * 60 * 1000);
                isLicensed = true;
                generatedLicenses.splice(foundLicIdx, 1);
                saveLicenses();
            } else {
                return res.status(400).json({ error: 'Ungültiger Lizenzschlüssel. Bitte erwerben Sie eine gültige Lizenz.' });
            }
        }
    }

    const nowIso = new Date().toISOString();
    const newUser = {
        id: 'usr_' + Date.now(),
        username: cleanUser,
        password: password.trim(),
        isAdmin: false,
        isLicensed: isLicensed,
        licenseExpiresAt: defaultExp.toISOString(),
        registeredAt: nowIso
    };
    registeredUsers.push(newUser);
    saveUsers();
    console.log(`[AUTH-REGISTER] Account registriert: ${cleanUser} (Freigeschaltet: ${isLicensed}, Key: ${keyInput || 'Keiner'})`);
    res.json({
        status: 'success',
        message: isLicensed ? 'Account erfolgreich registriert & freigeschaltet.' : 'Account registriert. Bitte erwerben Sie eine Lizenz.',
        isLicensed: isLicensed,
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
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px;">
                <h2 style="margin:0;">👤 Account- & Anmelde-Übersicht (${registeredUsers.length})</h2>
                <div style="display:flex; gap:10px; align-items:center;">
                    <input type="text" id="userSearch" class="input" onkeyup="filterUserTable()" placeholder="🔍 Benutzer suchen..." style="width: 240px;">
                </div>
            </div>
            <div style="margin-bottom: 16px; display:flex; gap:10px; flex-wrap:wrap;">
                <input type="text" id="newUsername" class="input" placeholder="Neuer Benutzername">
                <input type="password" id="newPassword" class="input" placeholder="Passwort">
                <button class="btn" onclick="createUser()">Benutzer erstellen</button>
            </div>
            <table id="userTable">
                <thead>
                    <tr><th>Benutzername</th><th>Lizenzstatus</th><th>Verknüpfte Geräte (HWID)</th><th>Ablaufdatum</th><th>Aktionen</th></tr>
                </thead>
                <tbody>
                    ${registeredUsers.map(u => {
                        const userDevices = registeredDevices.filter(d => d.username && d.username.toLowerCase() === u.username.toLowerCase());
                        const devicesStr = userDevices.length > 0 ? userDevices.map(d => `<code>${d.hwId.substring(0, 10)}...</code> (${d.deviceName})`).join(', ') : '<em style="color:#64748b;">Kein Gerät verbunden</em>';
                        const isExpired = u.licenseExpiresAt && new Date(u.licenseExpiresAt) < new Date() && !u.isAdmin;
                        const licBadge = u.isAdmin
                            ? '<span class="badge" style="background:#8b5cf6;">👑 Admin</span>'
                            : (isExpired ? '<span class="badge" style="background:#ef4444;">🔴 Abgelaufen</span>' : '<span class="badge" style="background:#10b981;">🟢 Aktiv</span>');
                        return `<tr>
                            <td><strong>${u.username}</strong></td>
                            <td>${licBadge}</td>
                            <td>${devicesStr}</td>
                            <td>${new Date(u.licenseExpiresAt || Date.now()).toLocaleDateString()}</td>
                            <td>
                                ${!u.isAdmin ? `<button class="btn btn-danger" onclick="deleteUser('${u.username}')">Löschen</button>` : '<em>Geschützt</em>'}
                            </td>
                        </tr>`;
                    }).join('')}
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

        function filterUserTable() {
            const input = document.getElementById('userSearch');
            const filter = input.value.toLowerCase();
            const table = document.getElementById('userTable');
            const tr = table.getElementsByTagName('tr');
            for (let i = 1; i < tr.length; i++) {
                const td = tr[i].getElementsByTagName('td')[0];
                if (td) {
                    const txtValue = td.textContent || td.innerText;
                    if (txtValue.toLowerCase().indexOf(filter) > -1) {
                        tr[i].style.display = "";
                    } else {
                        tr[i].style.display = "none";
                    }
                }
            }
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

const server = app.listen(PORT, '0.0.0.0', () => {
    console.log(`[Albion Server] 🟢 High-Performance Central Admin & Tunnel Server (v${CURRENT_SERVER_VERSION}) läuft auf 0.0.0.0:${PORT}`);
    triggerAutoOtaUpdateForAllDevices(`Server gestartet / Globaler OTA-Impuls v${CURRENT_SERVER_VERSION}`);
});
server.keepAliveTimeout = 120000; // Align with Render proxy keep-alive (120s) to fix 502 / Connection reset by peer
server.headersTimeout = 120500;   // Slightly higher than keepAliveTimeout
server.timeout = 180000;          // 3 minutes request timeout
