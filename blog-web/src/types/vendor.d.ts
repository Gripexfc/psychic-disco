declare module '@tiptap/extension-link' {
  const Link: { configure: (options?: Record<string, unknown>) => unknown }
  export default Link
}

declare module '@tiptap/extension-placeholder' {
  const Placeholder: { configure: (options?: Record<string, unknown>) => unknown }
  export default Placeholder
}

declare module '@tiptap/extension-image' {
  const Image: { configure: (options?: Record<string, unknown>) => unknown }
  export default Image
}

declare module '@tiptap/starter-kit' {
  const StarterKit: { configure: (options?: Record<string, unknown>) => unknown }
  export default StarterKit
}

declare module '@tiptap/vue-3' {
  import type { DefineComponent, Ref } from 'vue'

  export const EditorContent: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export function useEditor(config: Record<string, unknown>): Ref<any>
}

