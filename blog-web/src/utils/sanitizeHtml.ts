const BLOCKED_TAGS = ['script', 'iframe', 'object', 'embed', 'link', 'style']

export function sanitizeRichHtml(input: string): string {
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

