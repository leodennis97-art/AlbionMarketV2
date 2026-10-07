module.exports = {
  apps: [{
    name: "albion-market-server",
    script: "./server.js",
    watch: true,
    env: {
      NODE_ENV: "production",
      PORT: 4000
    }
  }]
};
