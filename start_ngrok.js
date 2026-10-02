const { spawn } = require('child_process');

console.log('[AlbionDataPro] 🟢 Starte lokalen Server mit Auto-Watch (--watch)...');
const server = spawn('node', ['--watch', 'server.js'], { stdio: 'inherit', shell: true });

server.on('close', (code) => {
    console.log(`[AlbionDataPro] Server beendet mit Code ${code}`);
    process.exit(code);
});

setTimeout(() => {
    console.log('[AlbionDataPro] 🌐 Starte Ngrok Tunnel (speller-importer-captivate.ngrok-free.dev)...');
    const ngrok = spawn('ngrok', ['http', '--domain=speller-importer-captivate.ngrok-free.dev', '4000'], { stdio: 'inherit', shell: true });

    ngrok.on('close', (code) => {
        console.log(`[AlbionDataPro] Ngrok beendet mit Code ${code}`);
        server.kill();
        process.exit(code);
    });
}, 2000);
