const { spawn } = require('child_process');
const https = require('https');

console.log('[AlbionDataPro] 🟢 Starte lokalen Server mit Auto-Watch (--watch)...');
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

setTimeout(() => {
    console.log('[AlbionDataPro] 🌐 Starte Ngrok Tunnel (speller-importer-captivate.ngrok-free.dev)...');
    const ngrok = spawn('ngrok', ['http', '--domain=speller-importer-captivate.ngrok-free.dev', '4000'], { stdio: 'inherit', shell: true });

    // Start periodic keep-alive pings every 8 minutes (480,000 ms)
    setInterval(pingRender, 8 * 60 * 1000);
    // Initial ping and periodic pings
    setTimeout(pingRender, 5000);

    ngrok.on('close', (code) => {
        console.log(`[AlbionDataPro] Ngrok beendet mit Code ${code}`);
        server.kill();
        process.exit(code);
    });
}, 2000);
