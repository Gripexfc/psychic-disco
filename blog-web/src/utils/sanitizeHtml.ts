const BLOCKED_TAGS = ['script', 'iframe', 'object', 'embed', 'link', 'style']

export function markdownToHtml(markdown: string): string {
  if (!markdown) return ''
  let html = markdown
    // Code blocks (before inline code) — no HTML escaping here yet
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    // Inline code
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // Headers
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^# (.+)$/gm, '<h1>$1</h1>')
    // Bold and italic
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    // Links
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>')
    // Blockquotes
    .replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
    // Unordered lists
    .replace(/^[\-\*] (.+)$/gm, '<li>$1</li>')
    // Horizontal rules
    .replace(/^---$/gm, '<hr>')
    // Paragraphs (double line breaks)
    .replace(/\n\n/g, '</p><p>')
    // Line breaks
    .replace(/\n/g, '<br>')
  // Wrap in paragraph
  html = '<p>' + html + '</p>'
  // Now escape HTML characters that are NOT part of the markdown-converted tags
  html = html
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  // Clean up empty paragraphs
  html = html.replace(/<p><\/p>/g, '')
  // Wrap consecutive <li> in <ul>
  html = html.replace(/(<li>.*?<\/li>)(?=(?:(?!<li>)[\s\S])*<li>)/g, '<ul>$1</ul>')
  return html
}

export function sanitizeRichHtml(input: string): string {
  // If input looks like markdown (contains # headers or markdown-specific chars), convert it
  if (input && (input.includes('## ') || input.includes('```') || input.includes('**'))) {
    input = markdownToHtml(input)
  }
  const parser = new DOMParser()
  const doc = parser.parseFromString(input, 'text/html')

  BLOCKED_TAGS.forEach((tag) => {
    doc.querySelectorAll(tag).forEach((el) => el.remove())
  })

  doc.querySelectorAll<HTMLElement>('*').forEach((node) => {
    ;[...node.attributes].forEach((attr) => {
      const name = attr.name.toLowerCase()
      const value = attr.value.trim().toLowerCase()
      const isEventAttr = name.startsWith('on')
      const isJsHref = (name === 'href' || name === 'src') && value.startsWith('javascript:')
      if (isEventAttr || isJsHref) {
        node.removeAttribute(attr.name)
      }
    })
  })

  return doc.body.innerHTML
}

export function plainTextFromHtml(input: string): string {
  const parser = new DOMParser()
  const doc = parser.parseFromString(input, 'text/html')
  return doc.body.textContent?.trim() ?? ''
}

