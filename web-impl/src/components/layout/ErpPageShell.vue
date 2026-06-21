<template>
  <div class="erp-page-shell">
    <header v-if="title" class="erp-page-header">
      <div class="erp-page-header__main">
        <h1 class="erp-page-title">{{ title }}</h1>
        <p v-if="description" class="erp-page-desc">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="erp-page-header__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="erp-page-body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
}>()
</script>

<style scoped>
.erp-page-shell {
  min-height: 100%;
}

.erp-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--erp-space-lg);
  margin-bottom: var(--erp-space-lg);
}

.erp-page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--erp-text-primary);
  line-height: 1.4;
}

.erp-page-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--erp-text-secondary);
  line-height: 1.5;
}

.erp-page-header__actions {
  flex-shrink: 0;
}

.erp-page-body {
  background: var(--erp-bg-card);
  border: 1px solid var(--erp-border);
  border-radius: var(--erp-radius-lg);
  padding: var(--erp-space-lg);
  box-shadow: var(--erp-shadow-sm), inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
</style>
