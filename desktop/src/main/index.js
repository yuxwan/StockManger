import { app, BrowserWindow, shell, Menu, ipcMain } from 'electron'
import { join } from 'path'
import { copyFileSync, existsSync, mkdirSync } from 'fs'
import { autoUpdater } from 'electron-updater'

const isDev = !!process.env.ELECTRON_RENDERER_URL

const gotTheLock = app.requestSingleInstanceLock()
if (!gotTheLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore()
      mainWindow.focus()
    }
  })
}

let mainWindow

app.setPath('userData', join(app.getPath('appData'), 'Stock'))
app.setPath('cache', join(app.getPath('appData'), 'Stock', 'Cache'))

autoUpdater.autoDownload = false
autoUpdater.autoInstallOnAppQuit = true

function copyInstallerToDownloads(srcPath) {
  try {
    const exeDir = app.getPath('exe').replace(/[\\/][^\\/]+$/, '')
    const destPath = join(exeDir, 'Stock-Setup.exe')
    copyFileSync(srcPath, destPath)
    return destPath
  } catch (e) {
    console.error('复制安装包失败:', e)
    try {
      const downloadsDir = app.getPath('downloads')
      if (!existsSync(downloadsDir)) mkdirSync(downloadsDir, { recursive: true })
      const destPath = join(downloadsDir, 'Stock-Setup.exe')
      copyFileSync(srcPath, destPath)
      return destPath
    } catch (e2) {
      console.error('复制安装包到下载目录也失败:', e2)
      return ''
    }
  }
}

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

ipcMain.handle('app:getVersion', () => app.getVersion())

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
    const filePath = autoUpdater.downloadedFile || ''
    let savedPath = ''
    if (filePath) {
      savedPath = copyInstallerToDownloads(filePath)
    }
    mainWindow?.webContents.send('update:downloaded', { ...info, savedPath })
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
