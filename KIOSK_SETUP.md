# Live Game Kiosk — iPad Setup

The kiosk is an iPad running Safari pointed at `/kiosk` on the app's host.
It holds a long-lived JWT for a `kiosk-system` service account and submits
completed matches to `POST /api/games` like any other client.

## 1. Pair the kiosk (recommended: admin approval)

On the iPad:
1. Open Safari → `https://<app-host>/kiosk/pair`
2. Tap **Request Pairing** (optionally type a device name first, e.g. "Main Table")
3. Screen switches to "Waiting for Approval"

On the admin's device (phone or laptop):
1. Sign in to the app as an admin user
2. A yellow **"N pending kiosk"** pill appears in the toolbar
3. Tap it → `/admin/kiosk-pairings` shows the pending request
4. Tap **Approve**

Within ~2 seconds the iPad auto-advances to the attract screen. The token is
stored in the iPad's `localStorage` and survives Safari/iPadOS restarts.

### Fallbacks for headless/scripted setups

**6-digit pairing code** — admin runs:
```sh
curl -X POST "$APP_URL/api/auth/kiosk-pairing-codes" \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{"deviceName":"main-table"}'
```
Returns `{"code":"482193",...}`. On the kiosk pair screen tap
"Have a 6-digit code?" and enter the digits on the keypad. Codes expire in 5 min.

**Direct long-lived JWT** — admin runs:
```sh
curl -X POST "$APP_URL/api/auth/kiosk-token" \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{"deviceName":"main-table"}'
```
Paste the `token` value into the kiosk's "Paste full token" fallback screen.

## 2. Configure the iPad for kiosk use

- **Auto-Lock**: Settings → Display & Brightness → Auto-Lock → **Never**
- **Guided Access** (optional, prevents accidental exits):
  Settings → Accessibility → Guided Access → On.
  Triple-click the Home/Power button to start a session on the kiosk page.
- **Fullscreen**:
  1. Open the kiosk URL in Safari
  2. Share → **Add to Home Screen**
  3. Launch from the new icon — opens fullscreen, no address bar
- **Orientation lock**: Control Center → rotation lock on (optional)

## 3. Manage paired kiosks

`/admin/kiosk-pairings` (Admin → Kiosks in the sidenav or user menu):

- **Pending Requests** — approve or deny incoming pairings
- **Paired Kiosks** — each device shows:
  - Green dot = heartbeated within last 2 min (online)
  - Gray dot = offline or iPad is asleep
  - Last-seen relative time
  - **Revoke** button — kicks the device back to the pairing screen on its next API call

## 4. Operational notes

- **Token rotation**: 30 days before expiry, the kiosk's attract screen shows a
  yellow "expires in N days" banner. Re-pair at that point.
- **Reboot safety**: in-progress match state persists in `localStorage` under
  `kiosk.activeMatch`. The attract screen offers **Resume Match** if state is
  found and not yet submitted.
- **Submit failure retry**: if the network drops when a match finishes, the
  summary screen shows **Retry Submit**. State stays until submit succeeds or
  is discarded.
- **Ranked guards**: guests cannot be created in the player picker when the
  match is ranked.
- **Emergency kill switch**: rotating `app.jwt.secret` invalidates every kiosk
  JWT globally. Devices bounce back to the pairing screen on their next call.

## 5. Dev/production notes for engineers

- `environment.prod.ts` points at `https://ping-pong-api.rcktapp.io`. Update if
  the hosting URL changes.
- `.browserslistrc` downlevels builds to support iOS 15 Safari — keep it while
  any paired iPads are on iOS 15.x (e.g. iPad Mini 4 maxes out there). Drop it
  when you fleet-upgrade to iOS 16+.
- The dev proxy (`frontend/proxy.conf.json`) routes `/api/*` and `/easter-eggs-ws/*`
  to `localhost:8080` during `npm start`. Not used in production builds.
- Backend CORS auto-allows private-LAN origins on port 4200 for on-device dev
  testing. Production should set `ALLOWED_ORIGINS` env var to the exact prod
  frontend origin.
- Kiosk device registry persists in the `kiosk_device` table; paired devices
  survive backend restarts. Revocations persist too.
