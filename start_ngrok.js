// start_ngrok.js - Autonomous Ngrok Tunnel & Localhost Server Manager
const { spawn, execSync } = require('child_process');
const http = require('http');
const path = require('path');
const fs = require('fs');

process.chdir(__dirname);

function killPort4000() {
    try {
        if (process.platform === 'win32') {
            execSync('cmd /c "for /f \"tokens=5\" %a in (\'netstat -aon ^| findstr :4000\') do taskkill /F /PID %a" >nul 2>&1');
        }
    } catch (_) {}
}

console.log('[Ngrok Manager] ⚡ Starte Localhost Server (Port 4000)...');
killPort4000();
const serverScript = path.join(__dirname, 'server.js');
const serverProcess = spawn('node', [serverScript], { stdio: 'inherit', cwd: __dirname });

serverProcess.on('exit', (code) => {
    console.log(`[Ngrok Manager] ⚠️ Server beendet (Code: ${code}). Starte neu...`);
});

console.log('[Ngrok Manager] 🌍 Starte High-Performance Ngrok Tunnel (EU Region + Kompression)...');
const ngrokProcess = spawn('ngrok', ['http', '4000', '--region=eu', '--compression'], {
    shell: true,
    stdio: 'inherit',
    cwd: __dirname
});

ngrokProcess.on('exit', (code) => {
    console.log(`[Ngrok Manager] ⚠️ Ngrok beendet (Code: ${code}).`);
});

// Poll ngrok API to get public URL and update config automatically
const checkNgrokUrl = () => {
    http.get('http://127.0.0.1:4040/api/tunnels', (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
            try {
                const json = JSON.parse(data);
                const publicUrl = json.tunnels?.[0]?.public_url;
                if (publicUrl) {
                    console.log(`\n========================================================`);
                    console.log(`🟢 NGROK TUNNEL AKTIV: ${publicUrl}`);
                    console.log(`========================================================\n`);

                    // Update server_config.json automatically
                    const configPath = path.join(__dirname, 'server_config.json');
                    const config = {
                        serverUrl: publicUrl,
                        ngrokUrl: publicUrl,
                        autoConnect: true,
                        note: "Automatic Ngrok Public Tunnel Configuration URL"
                    };
                    fs.writeFileSync(configPath, JSON.stringify(config, null, 4), 'utf-8');

                    // Also save current_tunnel.json
                    fs.writeFileSync(path.join(__dirname, 'current_tunnel.json'), JSON.stringify({ tunnelUrl: publicUrl }, null, 2));
                }
            } catch (_) {}
        });
    }).on('error', () => {
        // ngrok API not ready yet, retry
    });
};

// Check ngrok API every 2 seconds until successful
const interval = setInterval(() => {
    checkNgrokUrl();
}, 2000);
