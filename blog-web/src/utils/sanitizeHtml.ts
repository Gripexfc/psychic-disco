const BLOCKED_TAGS = ['script', 'iframe', 'object', 'embed', 'link', 'style']

// Generate a unique placeholder that won't appear in content
function placeholder(token: string, id: number): string {
  return `\x00${token}:${id}\x00`
}

export function markdownToHtml(markdown: string): string {
  if (!markdown) return ''

  let html = markdown
  let counter = 0

  // Step 1: Protect code blocks with placeholders
  html = html.replace(/```([\s\S]*?)```/g, (_, code) => {
    // Escape HTML in code content, preserve newlines
    const escaped = code
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
    return placeholder('CODE', counter++)
  })

  // Step 2: Protect inline code with placeholders
  html = html.replace(/`([^`]+)`/g, (_, code) => {
    const escaped = code
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
    return placeholder('INLINE', counter++) + escaped + placeholder('INLINE', counter++)
  })

  // Step 3: Escape HTML in remaining text
  html = html
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // Step 4: Convert markdown syntax to HTML
  // Headers
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')

  // Bold and italic
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // Links
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>')

  // Blockquotes
  html = html.replace(/^&gt; (.+)$/gm, '<blockquote>$1</blockquote>')

  // Horizontal rules
  html = html.replace(/^---$/gm, '<hr>')

  // Step 5: Wrap non-block lines in paragraphs
  const lines = html.split(/\n\n+/)
  html = lines.map(line => {
    line = line.trim()
    if (!line) return ''
    // Don't wrap if already a block element
    if (line.match(/^<(h[1-6]|blockquote|pre|ul|ol|li|hr|div)/)) {
      return line
    }
    // Don't wrap if placeholder
    if (line.includes('\x00')) {
      return line
    }
    return `<p>${line}</p>`
  }).join('\n')

  // Step 6: Handle single newlines within paragraphs -> <br>
  html = html.replace(/<\/p>\n<br>\n<p>/g, '<br>')

  // Step 7: Wrap consecutive <li> in <ul>
  html = html.replace(/(<li>[\s\S]*?<\/li>\n?)+/g, match => {
    return `<ul>${match}</ul>`
  })

  // Step 8: Restore code blocks
  html = html.replace(/\x00CODE:(\d+)\x00/g, (_, id) => {
    const idx = parseInt(id)
    const codeMatch = markdown.match(/```[\s\S]*?```/g)
    if (codeMatch && codeMatch[idx]) {
      let code = codeMatch[idx].replace(/^```|```$/g, '')
      return `<pre><code>${code}</code></pre>`
    }
    return ''
  })

  // Step 9: Restore inline code
  // This is tricky because we need to match pairs
  // Simplify: just find and restore inline code patterns
  html = html.replace(/<p>\x00INLINE:(\d+)\x00([\s\S]*?)\x00INLINE:(\d+)\x00<\/p>/g, (_, id1, content, id2) => {
    return `<p><code>${content}</code></p>`
  })

  // Clean up any remaining placeholders
  html = html.replace(/\x00[^\x00]+\x00/g, '')

  // Clean up empty paragraphs
  html = html.replace(/<p><\/p>/g, '')

  return html
}

export function sanitizeRichHtml(input: string): string {
  // If input looks like markdown, convert it
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
