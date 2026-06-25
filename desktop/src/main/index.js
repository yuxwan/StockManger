import { app, BrowserWindow, shell, Menu, ipcMain } from 'electron'
import { join } from 'path'
import { autoUpdater } from 'electron-updater'

const isDev = !!process.env.ELECTRON_RENDERER_URL

let mainWindow

// 自动更新配置
autoUpdater.autoDownload = false
autoUpdater.autoInstallOnAppQuit = true

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1366,
    height: 900,
    minWidth: 1200,
    minHeight: 700,
    frame: false,
    icon: join(__dirname, '../../logo.png'),
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

// ======= App 信息 IPC =======
ipcMain.handle('app:getVersion', () => app.getVersion())

// ======= 窗口控制 IPC =======
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

let manualUpdateCheck = false

// ======= 自动更新 IPC =======
ipcMain.handle('update:check', () => {
  manualUpdateCheck = true
  autoUpdater.checkForUpdates()
})

ipcMain.handle('update:download', () => {
  autoUpdater.downloadUpdate()
})

ipcMain.handle('update:install', () => {
  autoUpdater.quitAndInstall(false, true)
})

// ======= AutoUpdater 事件 → 发送给渲染进程 =======
function setupAutoUpdaterEvents() {
  autoUpdater.on('checking-for-update', () => {
    mainWindow?.webContents.send('update:checking', { manual: manualUpdateCheck })
  })

  autoUpdater.on('update-available', (info) => {
    mainWindow?.webContents.send('update:available', { info, manual: manualUpdateCheck })
    manualUpdateCheck = false
  })

  autoUpdater.on('update-not-available', () => {
    mainWindow?.webContents.send('update:not-available', { manual: manualUpdateCheck })
    manualUpdateCheck = false
  })

  autoUpdater.on('download-progress', (progress) => {
    mainWindow?.webContents.send('update:progress', progress)
  })

  autoUpdater.on('update-downloaded', (info) => {
    mainWindow?.webContents.send('update:downloaded', info)
  })

  autoUpdater.on('error', (err) => {
    mainWindow?.webContents.send('update:error', err.message || err.toString())
    manualUpdateCheck = false
  })
}

app.whenReady().then(() => {
  Menu.setApplicationMenu(null)
  createWindow()
  setupAutoUpdaterEvents()

  // 生产环境启动后自动检查更新（延迟 3 秒确保窗口已就绪）
  if (!isDev) {
    setTimeout(() => {
      autoUpdater.checkForUpdates()
    }, 3000)
  }

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
