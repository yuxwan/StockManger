import { app, BrowserWindow, shell, Menu, ipcMain } from 'electron'
import { join } from 'path'

const isDev = !!process.env.ELECTRON_RENDERER_URL

let mainWindow

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1366,
    height: 900,
    frame: false,
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      nodeIntegration: false,
      contextIsolation: true,
      devTools: isDev
    }
  })

  if (isDev) {
    mainWindow.loadURL(process.env.ELECTRON_RENDERER_URL)
    // 开发模式下允许 F12 打开 DevTools
    mainWindow.webContents.on('before-input-event', (_, input) => {
      if (input.key === 'F12') {
        mainWindow.webContents.toggleDevTools()
      }
    })
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })
}

ipcMain.handle('window:minimize', () => mainWindow?.minimize())
ipcMain.handle('window:maximize', () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize()
  } else {
    mainWindow.maximize()
  }
})
ipcMain.handle('window:close', () => mainWindow?.close())
ipcMain.handle('window:isMaximized', () => mainWindow?.isMaximized())

app.whenReady().then(() => {
  Menu.setApplicationMenu(null)
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
