<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="themeOverrides">
    <n-loading-bar-provider>
      <LoadingBarInit />
      <n-message-provider>
        <n-dialog-provider>
          <router-view />
        </n-dialog-provider>
      </n-message-provider>
    </n-loading-bar-provider>
  </n-config-provider>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { darkTheme } from 'naive-ui'
import LoadingBarInit from './components/LoadingBarInit.vue'

const isDark = ref(document.documentElement.classList.contains('dark'))
let observer

onMounted(() => {
  observer = new MutationObserver(() => {
    isDark.value = document.documentElement.classList.contains('dark')
  })
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})
onUnmounted(() => observer?.disconnect())



const naiveTheme = computed(() => (isDark.value ? darkTheme : null))

const themeOverrides = computed(() => ({
  common: {
    fontFamily: '"Plus Jakarta Sans", sans-serif',
    fontSize: '13px',
    borderRadius: '8px',
    primaryColor: isDark.value ? '#f2f0f0' : '#1b1c1c',
    primaryColorHover: isDark.value ? '#ffffff' : '#000000',
    primaryColorPressed: isDark.value ? '#e0e0e0' : '#333333',
    borderColor: isDark.value ? '#444' : '#cfc4c5',
    dividerColor: isDark.value ? '#333' : '#e3e2e2',
    bodyColor: isDark.value ? '#1a1a1a' : '#fbf9f9',
    cardColor: isDark.value ? '#252525' : '#efeded',
    textColor1: isDark.value ? '#f2f0f0' : '#1b1c1c',
    textColor2: isDark.value ? '#ccc' : '#4c4546',
    textColor3: isDark.value ? '#999' : '#7e7576',
    hoverColor: isDark.value ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)',
    closeColor: isDark.value ? '#999' : '#7e7576',
    closeColorHover: isDark.value ? '#999' : '#7e7576',
    placeholderColor: isDark.value ? '#555' : '#b0a8a9',
    tableHeaderColor: isDark.value ? '#252525' : '#efeded',
    actionColor: isDark.value ? '#252525' : '#fbf9f9',
    popoverColor: isDark.value ? '#252525' : '#ffffff',
    clearColor: isDark.value ? '#555' : '#b0a8a9',
    clearColorHover: isDark.value ? '#f2f0f0' : '#1b1c1c',
    scrollbarColor: isDark.value ? '#444' : '#cfc4c5',
    scrollbarColorHover: isDark.value ? '#666' : '#7e7576'
  },
  Pagination: {
    itemBorder: 'none',
    itemBorderActive: 'none',
    itemBorderHover: 'none',
    itemColor: 'transparent',
    itemColorHover: isDark.value ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)',
    itemColorActive: isDark.value ? '#f2f0f0' : '#1b1c1c',
    itemColorActiveHover: isDark.value ? '#ffffff' : '#000000',
    itemTextColor: isDark.value ? '#f2f0f0' : '#1b1c1c',
    itemTextColorHover: isDark.value ? '#f2f0f0' : '#1b1c1c',
    itemTextColorActive: isDark.value ? '#1a1a1a' : '#ffffff',
    itemTextColorDisabled: isDark.value ? '#555' : '#ccc',
    itemFontWeightActive: '600',
    itemBorderRadius: '8px',
    itemFontSize: '13px',
    itemSizeSmall: '28px',
    inputWidth: '60px'
  },
  DatePicker: {
    itemColorActive: isDark.value ? '#f2f0f0' : '#1b1c1c',
    itemTextColorActive: isDark.value ? '#1a1a1a' : '#ffffff',
    panelColor: isDark.value ? '#252525' : '#ffffff',
    calendarDaysTextColor: isDark.value ? '#999' : '#7e7576',
    actionDividerColor: isDark.value ? '#333' : '#e3e2e2',
    calendarDividerColor: isDark.value ? '#333' : '#e3e2e2'
  },
  Button: isDark.value ? {
    textColor: '#f2f0f0',
    textColorHover: '#ffffff',
    textColorFocus: '#f2f0f0',
    textColorDisabled: '#666',
    border: '1px solid #555',
    borderHover: '1px solid #888',
    borderFocus: '1px solid #555',
    borderDisabled: '1px solid #333',
    color: 'transparent',
    colorHover: 'rgba(255,255,255,0.08)',
    colorFocus: 'transparent',
    colorDisabled: '#252525',
    primaryTextColor: '#1a1a1a',
    primaryTextColorHover: '#1a1a1a',
    primaryColor: '#f2f0f0',
    primaryColorHover: '#ffffff',
    primaryColorFocus: '#f2f0f0',
    primaryColorDisabled: '#444'
  } : {},
  Card: {
    borderRadius: '24px',
    paddingMedium: '24px',
    paddingSmall: '16px',
    paddingLarge: '24px'
  },
  Input: {
    border: isDark.value ? '1px solid #444' : '1px solid #cfc4c5',
    borderHover: isDark.value ? '1px solid #555' : '1px solid #b0a8a9',
    borderFocus: isDark.value ? '1px solid #444' : '1px solid #cfc4c5',
    boxShadowFocus: '0 0 0 2px transparent'
  },
  Select: {
    border: '1px solid transparent',
    borderHover: '1px solid transparent',
    borderFocus: '1px solid transparent',
    boxShadowFocus: '0 0 0 2px transparent'
  },
  Drawer: {
    color: isDark.value ? '#1a1a1a' : '#fbf9f9'
  },
  Menu: {
    itemColorActive: isDark.value ? '#ffffff' : '#000000',
    itemColorActiveCollapsed: isDark.value ? '#ffffff' : '#000000',
    itemColorActiveHover: isDark.value ? '#ffffff' : '#000000',
    itemTextColor: isDark.value ? '#f2f0f0' : '#1b1c1c',
    itemTextColorActive: isDark.value ? '#1a1a1a' : '#ffffff',
    itemTextColorHover: isDark.value ? '#f2f0f0' : '#1b1c1c',
    itemColorHover: isDark.value ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)',
    itemIconColor: isDark.value ? '#f2f0f0' : '#1b1c1c',
    itemIconColorActive: isDark.value ? '#1a1a1a' : '#ffffff',
    itemIconColorHover: isDark.value ? '#f2f0f0' : '#1b1c1c',
    itemBorderRadius: '12px',
    itemHeight: '42px',
    groupTextColor: isDark.value ? '#999' : '#7e7576',
    arrowColor: isDark.value ? '#f2f0f0' : '#1b1c1c'
  }
}))
</script>

<style>
.n-base-selection {
  --n-border-active: 1px solid transparent !important;
  --n-border-focus: 1px solid transparent !important;
  --n-border-hover: 1px solid transparent !important;
  --n-box-shadow-active: none !important;
  --n-box-shadow-focus: none !important;
  --n-box-shadow-hover: none !important;
}

/* Loading bar 置于标题栏下方 */
.n-loading-bar-container {
  top: 40px !important;
}

.n-base-close {
  --n-color-hover: transparent !important;
  --n-icon-color-hover: var(--n-icon-color) !important;
  background: transparent !important;
}
.n-base-close:hover,
.n-base-close:hover::before,
.n-base-close:hover::after {
  background: transparent !important;
}
.n-base-close:hover svg,
.n-base-close:hover svg * {
  color: inherit !important;
  fill: inherit !important;
}
.n-card-header__close {
  --n-color-hover: transparent !important;
  --n-icon-color-hover: var(--n-icon-color) !important;
  background: transparent !important;
}
.n-card-header__close:hover {
  background: transparent !important;
}
.n-card-header__close:hover svg,
.n-card-header__close:hover svg * {
  color: inherit !important;
  fill: inherit !important;
}


</style>