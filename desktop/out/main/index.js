"use strict";
const electron = require("electron");
const path = require("path");
const isDev = !!process.env.ELECTRON_RENDERER_URL;
let mainWindow;
function createWindow() {
  mainWindow = new electron.BrowserWindow({
    width: 1366,
    height: 900,
    frame: false,
    webPreferences: {
      preload: path.join(__dirname, "../preload/index.js"),
      nodeIntegration: false,
      contextIsolation: true,
      devTools: isDev
    }
  });
  if (isDev) {
    mainWindow.loadURL(process.env.ELECTRON_RENDERER_URL);
    mainWindow.webContents.on("before-input-event", (_, input) => {
      if (input.key === "F12") {
        mainWindow.webContents.toggleDevTools();
      }
    });
  } else {
    mainWindow.loadFile(path.join(__dirname, "../renderer/index.html"));
  }
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    electron.shell.openExternal(url);
    return { action: "deny" };
  });
}
electron.ipcMain.handle("window:minimize", () => mainWindow?.minimize());
electron.ipcMain.handle("window:maximize", () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize();
  } else {
    mainWindow.maximize();
  }
});
electron.ipcMain.handle("window:close", () => mainWindow?.close());
electron.ipcMain.handle("window:isMaximized", () => mainWindow?.isMaximized());
electron.app.whenReady().then(() => {
  electron.Menu.setApplicationMenu(null);
  createWindow();
  electron.app.on("activate", () => {
    if (electron.BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});
electron.app.on("window-all-closed", () => {
  if (process.platform !== "darwin") electron.app.quit();
});
