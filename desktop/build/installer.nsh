!macro preInit
  ; 设置默认安装目录
  SetRegView 64
  WriteRegExpandStr HKLM "${INSTALL_REGISTRY_KEY}" InstallLocation "$PROGRAMFILES64\Stock"
  WriteRegExpandStr HKCU "${INSTALL_REGISTRY_KEY}" InstallLocation "$PROGRAMFILES64\Stock"
  SetRegView 32
  WriteRegExpandStr HKLM "${INSTALL_REGISTRY_KEY}" InstallLocation "$PROGRAMFILES\Stock"
  WriteRegExpandStr HKCU "${INSTALL_REGISTRY_KEY}" InstallLocation "$PROGRAMFILES\Stock"
!macroend

!macro customInstall
  ; 安装前关闭正在运行的旧版本
  ; 只杀已安装路径下的，不杀安装器本身
  nsExec::Exec `taskkill /f /im "${PRODUCT_FILENAME}.exe" /fi "PID ne $PID"`
!macroend
