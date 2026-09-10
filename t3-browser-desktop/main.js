const { app, BrowserWindow, session } = require('electron');

// Same tracker blocklist as the METMC OS Android version -- keep these in sync.
const BLOCKED_HOSTS = [
  "doubleclick.net", "googlesyndication.com", "googleadservices.com",
  "google-analytics.com", "googletagmanager.com", "googletagservices.com",
  "facebook.com/tr", "connect.facebook.net", "adnxs.com", "scorecardresearch.com",
  "outbrain.com", "taboola.com", "criteo.com", "amazon-adsystem.com",
  "adsrvr.org", "moatads.com", "quantserve.com", "mopub.com"
];

function createWindow() {
  // Private session: in-memory partition, wiped when the app closes,
  // never written to disk.
  const privateSession = session.fromPartition('t3-private', { cache: false });

  privateSession.webRequest.onBeforeRequest((details, callback) => {
    const blocked = BLOCKED_HOSTS.some(host => details.url.includes(host));
    callback({ cancel: blocked });
  });

  const win = new BrowserWindow({
    width: 1200,
    height: 800,
    title: 'T3 Private Browser',
    webPreferences: {
      session: privateSession,
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  win.loadURL('https://duckduckgo.com/');
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  // Private session data is in-memory only -- closing the app
  // is itself the "clear everything" step, nothing to explicitly wipe.
  if (process.platform !== 'darwin') app.quit();
});
