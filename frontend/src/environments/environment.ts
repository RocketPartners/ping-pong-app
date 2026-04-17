export const environment = {
  production: false,
  // Empty so the dev server's proxy (see proxy.conf.json) handles /api and /easter-eggs-ws.
  // This lets the iPad hit http://<mac-lan-ip>:4200 without a separate backend CORS entry.
  apiUrl: ''
};