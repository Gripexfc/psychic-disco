<script setup lang="ts">
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps<{ modelValue: string; disabled?: boolean }>()
const emit = defineEmits<{
  'update:modelValue': [value: string]
  notify: [message: string]
}>()

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: { levels: [2, 3] },
      codeBlock: {
        HTMLAttributes: { class: 'content-code-block' },
      },
    }),
    Link.configure({
      openOnClick: false,
      autolink: true,
      protocols: ['http', 'https', 'mailto'],
      HTMLAttributes: { rel: 'noopener noreferrer nofollow' },
    }),
    Placeholder.configure({
      placeholder: '输入正文内容... 支持标题、列表、引用、代码块、链接等',
    }),
    Image.configure({
      inline: false,
      allowBase64: true,
    }),
  ],
  editorProps: {
    attributes: {
      class: 'editor-content',
      spellcheck: 'false',
    },
  },
  onUpdate: ({ editor: currentEditor }: { editor: { getHTML: () => string } }) => {
    emit('update:modelValue', currentEditor.getHTML())
  },
})
const imageInputRef = ref<HTMLInputElement | null>(null)
const quickInputType = ref<'link' | 'image' | ''>('')
const quickInputValue = ref('')

const canEdit = computed(() => !props.disabled && !!editor.value)

function exec(action: () => void) {
  if (!canEdit.value) {
    return
  }
  action()
}

function runChain(transform: (chain: any) => any) {
  exec(() => {
    if (!editor.value) {
      return
    }
    transform(editor.value.chain().focus()).run()
  })
}

const toggleH2 = () => runChain((chain) => chain.toggleHeading({ level: 2 }))
const toggleH3 = () => runChain((chain) => chain.toggleHeading({ level: 3 }))
const toggleBold = () => runChain((chain) => chain.toggleBold())
const toggleItalic = () => runChain((chain) => chain.toggleItalic())
const toggleBulletList = () => runChain((chain) => chain.toggleBulletList())
const toggleOrderedList = () => runChain((chain) => chain.toggleOrderedList())
const toggleQuote = () => runChain((chain) => chain.toggleBlockquote())
const toggleCodeBlock = () => runChain((chain) => chain.toggleCodeBlock())
const undo = () => runChain((chain) => chain.undo())
const redo = () => runChain((chain) => chain.redo())

function insertImage(src: string, alt = 'image') {
  runChain((chain) => chain.setImage({ src, alt }))
}

function setImageByUrl() {
  quickInputType.value = 'image'
  quickInputValue.value = ''
}

function openImagePicker() {
  imageInputRef.value?.click()
}

async function fileToDataUrl(file: File): Promise<string> {
  return await new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result ?? ''))
    reader.onerror = () => reject(new Error('读取图片失败'))
    reader.readAsDataURL(file)
  })
}

async function handleLocalImage(file: File) {
  if (!file.type.startsWith('image/')) {
    emit('notify', '仅支持图片文件上传。')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    emit('notify', '图片过大，请控制在 2MB 内。')
    return
  }
  try {
    const dataUrl = await fileToDataUrl(file)
    insertImage(dataUrl, file.name || 'local-image')
  } catch {
    emit('notify', '图片插入失败，请重试。')
  }
}

async function onImageFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  await handleLocalImage(file)
  input.value = ''
}

function setLink() {
  if (!editor.value) {
    return
  }
  quickInputType.value = 'link'
  quickInputValue.value = editor.value.getAttributes('link').href ?? ''
}

function closeQuickInput() {
  quickInputType.value = ''
  quickInputValue.value = ''
}

function applyQuickInput() {
  if (!editor.value || !quickInputType.value) {
    return
  }
  const value = quickInputValue.value.trim()
  if (quickInputType.value === 'link') {
    if (!value) {
      exec(() => editor.value?.chain().focus().unsetLink().run())
      closeQuickInput()
      return
    }
    if (!/^https?:\/\//.test(value) && !value.startsWith('mailto:')) {
      emit('notify', '链接地址不合法，请使用 http(s) 或 mailto。')
      return
    }
    exec(() => editor.value?.chain().focus().extendMarkRange('link').setLink({ href: value }).run())
    closeQuickInput()
    return
  }
  if (!/^https?:\/\//.test(value) && !value.startsWith('data:image/')) {
    emit('notify', '图片地址不合法，请使用 http(s) 或 data:image。')
    return
  }
  insertImage(value, 'remote-image')
  closeQuickInput()
}

watch(
  () => props.modelValue,
  (value) => {
    if (!editor.value || editor.value.getHTML() === value) {
      return
    }
    editor.value.commands.setContent(value, false)
  },
)

watch(
  () => props.disabled,
  (disabled) => {
    editor.value?.setEditable(!disabled)
  },
  { immediate: true },
)

watch(
  () => editor.value,
  (instance) => {
    if (!instance) {
      return
    }
    instance.on('paste', async ({ event }: { event: ClipboardEvent }) => {
      const file = event.clipboardData?.files?.[0]
      if (file && file.type.startsWith('image/')) {
        event.preventDefault()
        await handleLocalImage(file)
      }
    })
  },
)

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<template>
  <div class="editor-wrap">
    <div class="toolbar">
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="toggleH2">H2</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="toggleH3">H3</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="toggleBold">B</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="toggleItalic">I</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="toggleBulletList">UL</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="toggleOrderedList">OL</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="toggleQuote">引用</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="toggleCodeBlock">代码块</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="setLink">链接</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="setImageByUrl">图片链接</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="openImagePicker">上传图片</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="undo">撤销</button>
      <button class="tool-btn" type="button" :disabled="!canEdit" @click="redo">重做</button>
    </div>
    <div v-if="quickInputType" class="quick-input-wrap">
      <input
        v-model="quickInputValue"
        class="quick-input"
        :placeholder="quickInputType === 'link' ? '输入链接 URL（https://）' : '输入图片 URL（https://）'"
      />
      <button class="tool-btn" type="button" @click="applyQuickInput">确认</button>
      <button class="tool-btn" type="button" @click="closeQuickInput">取消</button>
    </div>
    <EditorContent v-if="editor" :editor="editor" class="editor" />
    <div v-else class="editor editor-loading">编辑器加载中...</div>
    <input ref="imageInputRef" class="image-input" type="file" accept="image/*" @change="onImageFileChange" />
  </div>
</template>

<style scoped>
.editor-wrap {
  display: grid;
  gap: 0.55rem;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.tool-btn {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  color: #eef2ff;
  border-radius: 0.55rem;
  padding: 0.3rem 0.55rem;
  cursor: pointer;
  font-size: 0.82rem;
}

.tool-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.editor {
  min-height: 220px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.06);
  border-radius: 0.7rem;
  padding: 0.75rem;
  color: #eef2ff;
  line-height: 1.7;
}

.quick-input-wrap {
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.quick-input {
  min-width: min(360px, 100%);
  flex: 1;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.08);
  color: #eef2ff;
  border-radius: 0.55rem;
  padding: 0.4rem 0.55rem;
}

.editor-loading {
  display: grid;
  place-items: center;
  opacity: 0.8;
}

.image-input {
  display: none;
}

.editor :deep(.ProseMirror) {
  min-height: 220px;
  outline: none;
}

.editor :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  color: rgba(226, 232, 240, 0.65);
  float: left;
  height: 0;
  pointer-events: none;
}
</style>

