import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('electronAPI', {
  platform: process.platform,
  getAppVersion: () => ipcRenderer.invoke('app:getVersion'),
  minimize: () => ipcRenderer.invoke('window:minimize'),
  maximize: () => ipcRenderer.invoke('window:maximize'),
  close: () => ipcRenderer.invoke('window:close'),
  isMaximized: () => ipcRenderer.invoke('window:isMaximized'),

  // ======= 自动更新 API =======
  onUpdateChecking: (callback) => {
    ipcRenderer.on('update:checking', (_event, data) => callback(data))
  },
  onUpdateAvailable: (callback) => {
    ipcRenderer.on('update:available', (_event, data) => callback(data.info ? { ...data.info, manual: data.manual } : data))
  },
  onUpdateNotAvailable: (callback) => {
    ipcRenderer.on('update:not-available', (_event, data) => callback(data))
  },
  onUpdateProgress: (callback) => {
    ipcRenderer.on('update:progress', (_event, progress) => callback(progress))
  },
  onUpdateDownloaded: (callback) => {
    ipcRenderer.on('update:downloaded', (_event, info) => callback(info))
  },
  onUpdateError: (callback) => {
    ipcRenderer.on('update:error', (_event, msg) => callback(msg))
  },
  checkUpdate: () => ipcRenderer.invoke('update:check'),
  downloadUpdate: () => ipcRenderer.invoke('update:download'),
  installUpdate: () => ipcRenderer.invoke('update:install'),
  removeAllUpdateListeners: () => {
    ipcRenderer.removeAllListeners('update:checking')
    ipcRenderer.removeAllListeners('update:available')
    ipcRenderer.removeAllListeners('update:not-available')
    ipcRenderer.removeAllListeners('update:progress')
    ipcRenderer.removeAllListeners('update:downloaded')
    ipcRenderer.removeAllListeners('update:error')
  }
})
