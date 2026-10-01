// auto_restart_watchdog.js - Hyper-Sensitive 1.5-Second Watchdog with Full-Screen Red Alert & Tunnel/Server Recovery
const { exec, spawn } = require('child_process');
const http = require('http');
const https = require('https');
const path = require('path');
const fs = require('fs');
const localtunnel = require('localtunnel');

let isStartingServer = false;
let serverProcess = null;
let currentTunnel = null;
let warningProcess = null;
let isOfflineAlertActive = false;
let hasOpenedBrowser = false;

function showRedAlert() {
    if (isOfflineAlertActive) return;
    isOfflineAlertActive = true;

    const psCommand = `
        Add-Type -AssemblyName System.Windows.Forms;
        Add-Type -AssemblyName System.Drawing;
        $f = New-Object System.Windows.Forms.Form;
        $f.FormBorderStyle = 'None';
        $f.WindowState = 'Maximized';
        $f.TopMost = $true;
        $f.BackColor = [System.Drawing.Color]::FromArgb(220, 38, 38);
        $l = New-Object System.Windows.Forms.Label;
        $l.Text = "🚨 KRITISCHER ALARM: ALBION SERVER ODER TUNNEL OFFLINE!\n\nSofortiger automatischer Neustart läuft...";
        $l.Font = New-Object System.Drawing.Font('Segoe UI', 26, [System.Drawing.FontStyle]::Bold);
        $l.ForeColor = [System.Drawing.Color]::White;
        $l.TextAlign = 'MiddleCenter';
        $l.Dock = 'Fill';
        $f.Controls.Add($l);
        [void]$f.ShowDialog();
    `;

    warningProcess = spawn('powershell.exe', ['-WindowStyle', 'Hidden', '-Command', psCommand], {
        detached: true,
        stdio: 'ignore'
    });
    warningProcess.unref();
}

function hideRedAlert() {
    if (!isOfflineAlertActive) return;
    isOfflineAlertActive = false;
    try {
        exec('taskkill /f /im powershell.exe /fi "WINDOWTITLE eq *ALARM*" >nul 2>&1');
    } catch (_) {}
    if (warningProcess) {
        try { warningProcess.kill('SIGKILL'); } catch (_) {}
        warningProcess = null;
    }
}

function killPort4000() {
    try {
        if (process.platform === 'win32') {
            exec('cmd /c "for /f \"tokens=5\" %a in (\'netstat -aon ^| findstr :4000\') do taskkill /F /PID %a" >nul 2>&1');
        }
    } catch (_) {}
}

function runServer() {
    if (isStartingServer) return;
    isStartingServer = true;
    console.log('[Hyper-Watchdog] ⚡ Starte Localhost Server (server.js)...');

    if (serverProcess) {
        try { serverProcess.kill('SIGKILL'); } catch (_) {}
        serverProcess = null;
    }

    killPort4000();
    const serverScript = path.join(__dirname, 'server.js');
    serverProcess = spawn('node', [serverScript], {
        cwd: __dirname,
        stdio: 'inherit'
    });
    isStartingServer = false;

    serverProcess.on('exit', () => {
        console.log('[Hyper-Watchdog] ⚠️ Server beendet. Starte in 500ms neu...');
        setTimeout(runServer, 500);
    });
}

async function startTunnel() {
    try {
        if (currentTunnel) {
            try { currentTunnel.close(); } catch (_) {}
            currentTunnel = null;
        }
        console.log('[Hyper-Watchdog] 🌍 Starte programmatischen Public Tunnel (Port 4000)...');
        const tunnel = await localtunnel({ port: 4000 });
        currentTunnel = tunnel;
        console.log(`[Hyper-Watchdog] 🟢 Public Tunnel aktiv: ${tunnel.url}`);

        fs.writeFileSync(path.join(__dirname, 'current_tunnel.json'), JSON.stringify({ tunnelUrl: tunnel.url }, null, 2));
        const configPath = path.join(__dirname, 'server_config.json');
        const config = { serverUrl: tunnel.url, ngrokUrl: tunnel.url, autoConnect: true };
        fs.writeFileSync(configPath, JSON.stringify(config, null, 4));

        tunnel.on('close', () => {
            console.log('[Hyper-Watchdog] ⚠️ Tunnel geschlossen. Starte in 1s neu...');
            setTimeout(startTunnel, 1000);
        });
    } catch (e) {
        console.error('[Hyper-Watchdog] Tunnel Fehler:', e.message);
        setTimeout(startTunnel, 2000);
    }
}

// Hyper-Sensitive Check every 1.5 seconds (checking both Localhost AND Tunnel)
setInterval(() => {
    let localOk = false;
    let tunnelOk = false;
    let checkedCount = 0;

    const checkComplete = () => {
        checkedCount++;
        if (checkedCount === 2) {
            if (localOk && tunnelOk) {
                hideRedAlert();

                if (!hasOpenedBrowser) {
                    hasOpenedBrowser = true;
                    console.log('[Hyper-Watchdog] 🌐 Öffne Admin-Dashboard in Google Chrome...');
                    exec('start chrome http://localhost:4000/admin', (err) => {
                        if (err) exec('start http://localhost:4000/admin');
                    });
                }
            } else {
                // Immediate reaction on failure (1st failed check triggers alert & recovery)
                showRedAlert();
                if (!localOk) runServer();
                if (!tunnelOk) startTunnel();
            }
        }
    };

    // 1. Check Localhost
    const req = http.get('http://127.0.0.1:4000/', { timeout: 1500 }, (res) => {
        localOk = (res.statusCode === 200);
        res.resume();
        checkComplete();
    });
    req.on('error', () => {
        localOk = false;
        checkComplete();
    });
    req.setTimeout(1500, () => {
        req.destroy();
        localOk = false;
        checkComplete();
    });

    // 2. Check Public Tunnel
    let tunnelUrl = 'https://witty-catfish-22.loca.lt';
    try {
        const cfg = JSON.parse(fs.readFileSync(path.join(__dirname, 'current_tunnel.json'), 'utf8'));
        if (cfg.tunnelUrl) tunnelUrl = cfg.tunnelUrl;
    } catch (_) {}

    const tunnelReq = https.get(`${tunnelUrl}/api/health`, {
        headers: { 'Bypass-Tunnel-Reminder': 'true' },
        timeout: 2000
    }, (res) => {
        tunnelOk = (res.statusCode === 200);
        res.resume();
        checkComplete();
    });
    tunnelReq.on('error', () => {
        tunnelOk = false;
        checkComplete();
    });
    tunnelReq.setTimeout(2000, () => {
        tunnelReq.destroy();
        tunnelOk = false;
        checkComplete();
    });

}, 1500);

// Initial start
console.log('[Hyper-Watchdog] 🚀 Hyper-Sensitiver Watchdog gestartet (Intervall: 1.5s)...');
runServer();
setTimeout(startTunnel, 2000);
