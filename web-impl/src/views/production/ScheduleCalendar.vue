<template>
  <div>
    <h2 v-if="showTitle">排产日历</h2>
    <el-calendar v-model="date">
      <template #date-cell="{ data }">
        <div :class="cellClass(data)">
          <p class="day-num">{{ data.day.split('-').slice(2).join('') }}</p>
          <p v-for="ev in eventsOn(data.day)" :key="ev.title + ev.date" class="ev-dot" :title="ev.title">
            {{ ev.title }}
          </p>
        </div>
      </template>
    </el-calendar>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

export interface CalendarEvent {
  title: string
  date: string
}

const props = withDefaults(
  defineProps<{
    events?: CalendarEvent[]
    showTitle?: boolean
  }>(),
  { events: () => [], showTitle: true },
)

const date = ref(new Date())
const cellClass = (data: { isSelected?: boolean }) => (data.isSelected ? 'is-selected' : '')

function eventsOn(day: string) {
  return props.events.filter((e) => e.date === day)
}
</script>

<style scoped>
.day-num {
  margin: 0;
  font-size: 12px;
}
.ev-dot {
  margin: 2px 0 0;
  font-size: 10px;
  color: var(--el-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.is-selected {
  background: var(--el-color-primary-light-9);
}
</style>
