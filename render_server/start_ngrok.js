const { spawn, exec } = require('child_process');
const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');
const FormData = require('form-data');

console.log('[AlbionDataPro] 🟢 Starte lokalen Server mit Auto-Watch (--watch)...');

// Automatische Suche nach der neuesten APK, Bereitstellung im Download-Ordner UND automatischer Render-Upload!
function findSyncAndUploadLatestApk() {
    try {
        const downloadsDir = path.join(__dirname, 'downloads');
        if (!fs.existsSync(downloadsDir)) fs.mkdirSync(downloadsDir, { recursive: true });

        const debugApk = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
        const releaseApk = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk');

        let latestSource = null;
        let latestMtime = 0;

        if (fs.existsSync(debugApk)) {
            const stat = fs.statSync(debugApk);
            if (stat.mtimeMs > latestMtime) {
                latestMtime = stat.mtimeMs;
                latestSource = debugApk;
            }
        }
        if (fs.existsSync(releaseApk)) {
            const stat = fs.statSync(releaseApk);
            if (stat.mtimeMs > latestMtime) {
                latestMtime = stat.mtimeMs;
                latestSource = releaseApk;
            }
        }

        if (latestSource) {
            const targetPath = path.join(downloadsDir, 'DataPro.apk');

            // Prüfen ob sich die APK geändert hat (mittels Mtime)
            const targetStat = fs.existsSync(targetPath) ? fs.statSync(targetPath) : null;
            if (!targetStat || latestMtime > targetStat.mtimeMs) {
                fs.copyFileSync(latestSource, targetPath);
                console.log(`[AlbionDataPro] 📦 Neue APK erkannt & synchronisiert von: ${path.relative(__dirname, latestSource)}`);

                // Direkt auch autonom zu Render hochladen!
                uploadApkToRenderServer(targetPath);
            }
        }
    } catch (e) {
        console.error('[AlbionDataPro] ⚠️ Fehler bei automatischer APK-Suche:', e.message);
    }
}

// Funktion zum automatischen Hochladen der APK auf Render
function uploadApkToRenderServer(apkPath) {
    try {
        // Lese Version aus build.gradle aus
        const buildGradlePath = path.join(__dirname, 'app', 'build.gradle');
        let versionName = '2.0.9';
        if (fs.existsSync(buildGradlePath)) {
            const content = fs.readFileSync(buildGradlePath, 'utf8');
            const match = content.match(/versionName\s+['"]([^'"]+)['"]/);
            if (match) versionName = match[1];
        }

        console.log(`[AlbionDataPro] 🚀 Lade neueste APK (v${versionName}) autonom auf Render hoch...`);
        const form = new FormData();
        form.append('apkFile', fs.createReadStream(apkPath));

        const req = https.request({
            hostname: 'albionmarketv2-1.onrender.com',
            path: `/api/admin/upload-apk?version=${versionName}`,
            method: 'POST',
            headers: {
                'Authorization': 'Bearer AlbionDataPro_Military_Admin_SuperSecret_2026#Key',
                ...form.getHeaders()
            }
        }, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                if (res.statusCode === 200) {
                    console.log(`[AlbionDataPro] ✅ Render-Webseite & Download-Button erfolgreich auf v${versionName} aktualisiert!`);
                } else {
                    console.log(`[AlbionDataPro] ⚠️ Render Upload Status ${res.statusCode}: ${data}`);
                }
            });
        });

        req.on('error', (err) => {
            console.log(`[AlbionDataPro] ℹ️ Render Upload im Hintergrund nicht erreichbar: ${err.message}`);
        });

        form.pipe(req);
    } catch (e) {
        console.error('[AlbionDataPro] ⚠️ Render Auto-Upload Fehler:', e.message);
    }
}

// Vor Serverstart einmalig neueste APK suchen & hochladen
findSyncAndUploadLatestApk();
// Und kontinuierlich alle 5 Sekunden prüfen
setInterval(findSyncAndUploadLatestApk, 5000);

const server = spawn('node', ['--watch', 'server.js'], { stdio: 'inherit', shell: true });

server.on('close', (code) => {
    console.log(`[AlbionDataPro] Server beendet mit Code ${code}`);
    process.exit(code);
});

// Keep-Alive Function to prevent Render from spinning down (every 8 minutes)
function pingRender() {
    const targetUrl = 'https://albionmarketv2-1.onrender.com';
    https.get(targetUrl, (res) => {
        console.log(`[AlbionDataPro] 🏓 Keep-Alive Ping an Render (${targetUrl}) - Status: ${res.statusCode}`);
    }).on('error', (err) => {
        console.log(`[AlbionDataPro] ⚠️ Keep-Alive Ping Fehler: ${err.message}`);
    });
}

// Automatischen Gradle Build ausführen
function runAutomaticBuild() {
    console.log('[AlbionDataPro] ⏰ Starte automatischen Gradle Build...');
    const gradlew = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
    exec(`${gradlew} assembleDebug`, { cwd: __dirname }, (error, stdout, stderr) => {
        if (error) {
            console.error(`[AlbionDataPro] ❌ Automatischer Build fehlgeschlagen: ${error.message}`);
            return;
        }
        findSyncAndUploadLatestApk();
        console.log(`[AlbionDataPro] ✅ Automatischer Build & Sync erfolgreich abgeschlossen!`);
    });
}

setTimeout(() => {
    console.log('[AlbionDataPro] 🌐 Starte Ngrok Tunnel (speller-importer-captivate.ngrok-free.dev)...');
    const ngrok = spawn('ngrok', ['http', '--domain=speller-importer-captivate.ngrok-free.dev', '4000'], { stdio: 'inherit', shell: true });

    setInterval(pingRender, 8 * 60 * 1000);
    setTimeout(pingRender, 5000);

    setTimeout(() => {
        runAutomaticBuild();
        setInterval(runAutomaticBuild, 30 * 60 * 1000);
    }, 120000);

    ngrok.on('close', (code) => {
        console.log(`[AlbionDataPro] Ngrok beendet mit Code ${code}`);
        server.kill();
        process.exit(code);
    });
}, 2000);
