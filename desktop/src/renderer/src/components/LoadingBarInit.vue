<script setup>
import { useLoadingBar } from 'naive-ui'
import { loadingBarRef } from '../utils/loadingBar'

const loadingBar = useLoadingBar()
loadingBarRef.value = loadingBar
</script>

<template></template>
