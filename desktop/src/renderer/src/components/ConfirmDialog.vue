<template>
  <Transition name="confirm">
    <div v-if="show" class="fixed inset-0 z-[200] flex items-center justify-center" @click.self="onCancel">
      <div class="fixed inset-0 bg-black/30 dark:bg-black/50" />
      <div
        class="relative rounded-2xl bg-surface dark:bg-[#252525] shadow-xl border border-outline-variant/20 dark:border-[#333]"
        :style="{ width }"
      >
        <!-- 标题 -->
        <div class="flex items-center gap-3 px-6 pt-6" v-if="title || $slots.header">
          <div v-if="iconType === 'warning'"
            class="w-9 h-9 rounded-xl bg-red-100 dark:bg-red-950/30 flex items-center justify-center shrink-0 mt-0.5">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-red-600 dark:text-red-400">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="text-base font-body font-bold text-on-surface dark:text-inverse-on-surface">{{ title }}</h3>
            <p v-if="content" class="mt-1.5 text-sm font-body text-on-surface-variant dark:text-gray-400 leading-relaxed">{{ content }}</p>
          </div>
          <slot name="header" />
        </div>

        <!-- 主体内容 -->
        <div class="px-6" :class="title ? 'pt-4 pb-2' : 'pt-6 pb-2'">
          <slot />
        </div>

        <!-- 底部按钮 -->
        <div v-if="$slots.footer" class="flex items-center justify-end gap-2 px-6 pb-6 pt-4">
          <slot name="footer" />
        </div>
        <div v-else-if="!hideDefaultFooter" class="flex items-center justify-end gap-2 px-6 pb-6 pt-4">
          <button
            class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors"
            @click="onCancel"
          >{{ cancelText }}</button>
          <button
            v-if="type === 'error'"
            class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-red-500 hover:bg-red-600 transition-colors"
            @click="onConfirm"
          >{{ confirmText }}</button>
          <button
            v-else
            class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity"
            @click="onConfirm"
          >{{ confirmText }}</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
const props = defineProps({
  show: Boolean,
  title: { type: String, default: '' },
  content: { type: String, default: '' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  width: { type: String, default: '360px' },
  type: { type: String, default: 'default' }, // 'default' | 'error'
  iconType: { type: String, default: '' }, // 'warning' | ''
  hideDefaultFooter: { type: Boolean, default: false }
})

const emit = defineEmits(['update:show', 'confirm'])

function onConfirm() {
  emit('confirm')
}

function onCancel() {
  emit('update:show', false)
}
</script>

<style scoped>
.confirm-enter-active {
  transition: opacity 200ms ease;
}
.confirm-leave-active {
  transition: opacity 150ms ease;
}
.confirm-enter-from,
.confirm-leave-to {
  opacity: 0;
}
.confirm-enter-active > div:last-child {
  transition: transform 200ms cubic-bezier(0.34, 1.56, 0.64, 1), opacity 200ms ease;
}
.confirm-leave-active > div:last-child {
  transition: transform 150ms ease, opacity 150ms ease;
}
.confirm-enter-from > div:last-child {
  transform: scale(0.92);
  opacity: 0;
}
.confirm-leave-to > div:last-child {
  transform: scale(0.92);
  opacity: 0;
}
</style>
