<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    description?: string
    confirmText?: string
    cancelText?: string
    danger?: boolean
    closeOnMask?: boolean
  }>(),
  {
    description: '',
    confirmText: '确认',
    cancelText: '取消',
    danger: false,
    closeOnMask: true,
  },
)

const emit = defineEmits<{
  close: []
  confirm: []
  cancel: []
}>()

function onMaskClick() {
  if (props.closeOnMask) {
    emit('close')
  }
}
</script>

<template>
  <Transition name="fade">
    <div v-if="open" class="modal-mask" @click="onMaskClick">
      <div class="modal-card" role="dialog" aria-modal="true" @click.stop>
        <header class="modal-header">
          <h3>{{ title }}</h3>
          <button class="icon-close" type="button" @click="emit('close')">×</button>
        </header>
        <p v-if="description" class="modal-desc">{{ description }}</p>
        <div class="modal-body">
          <slot />
        </div>
        <footer class="modal-actions">
          <button class="btn btn-ghost" type="button" @click="emit('cancel')">{{ cancelText }}</button>
          <button class="btn" :class="danger ? 'btn-danger' : 'btn-main'" type="button" @click="emit('confirm')">
            {{ confirmText }}
          </button>
        </footer>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: rgba(2, 6, 23, 0.72);
  display: grid;
  place-items: center;
  padding: 1rem;
}

.modal-card {
  width: min(420px, 100%);
  border-radius: 1.1rem;
  border: 1px solid rgba(129, 140, 248, 0.35);
  background:
    linear-gradient(160deg, rgba(15, 23, 42, 0.98), rgba(30, 41, 59, 0.96)),
    radial-gradient(circle at 15% 20%, rgba(34, 211, 238, 0.15), transparent 45%);
  box-shadow:
    0 24px 60px rgba(2, 6, 23, 0.58),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  padding: 1.1rem;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  font-size: 1.1rem;
  color: #e2e8f0;
}

.icon-close {
  border: 0;
  border-radius: 0.55rem;
  width: 1.8rem;
  height: 1.8rem;
  cursor: pointer;
  color: #e2e8f0;
  background: rgba(148, 163, 184, 0.18);
  transition: background-color 0.2s ease;
}

.icon-close:hover {
  background: rgba(148, 163, 184, 0.32);
}

.modal-desc {
  color: #cbd5e1;
  margin-top: 0.7rem;
  line-height: 1.6;
}

.modal-body {
  margin-top: 0.8rem;
}

.modal-actions {
  margin-top: 1rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.btn {
  border: 0;
  border-radius: 0.75rem;
  padding: 0.6rem 0.95rem;
  cursor: pointer;
  font-weight: 600;
  transition: transform 0.15s ease;
}

.btn:hover {
  transform: translateY(-1px);
}

.btn-main {
  background: linear-gradient(135deg, #22d3ee, #818cf8);
  color: #04111f;
}

.btn-danger {
  background: linear-gradient(135deg, #f97316, #ef4444);
  color: #fff7ed;
}

.btn-ghost {
  background: rgba(148, 163, 184, 0.2);
  color: #e2e8f0;
}

@media (max-width: 520px) {
  .modal-actions {
    justify-content: stretch;
  }

  .modal-actions .btn {
    width: 100%;
  }
}
</style>

