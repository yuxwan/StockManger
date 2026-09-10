// 接口环境配置
// 开发者工具调试用本地后端：localhost，需在「详情-本地设置」勾选「不校验合法域名」
// 真机/体验版：切换为 https://yuxwan.com/stock/api/
// const BASE_URL = 'https://yuxwan.com/stock/api/'
const BASE_URL = 'http://192.168.5.10:8080/api/'

module.exports = {
  BASE_URL,
  BASE_UPLOAD_URL: BASE_URL + 'upload'
}
