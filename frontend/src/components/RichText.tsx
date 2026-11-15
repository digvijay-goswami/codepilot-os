import type { ReactNode } from 'react'

// Small renderer for AI answers: paragraphs, bullet and numbered lists,
// fenced code blocks, `inline code` and **bold**. No HTML is ever injected.
const BULLET = /^\s*[*-]\s+/
const NUMBERED = /^\s*\d+[.)]\s+/

function inline(text: string): ReactNode[] {
  return text.split(/(`[^`\n]+`|\*\*[^*\n]+\*\*)/g).map((part, i) => {
    if (part.length > 2 && part.startsWith('`') && part.endsWith('`')) {
      return <code key={i}>{part.slice(1, -1)}</code>
    }
    if (part.length > 4 && part.startsWith('**') && part.endsWith('**')) {
      return <strong key={i}>{part.slice(2, -2)}</strong>
    }
    return part
  })
}

function readList(lines: string[], start: number, marker: RegExp) {
  const items: string[] = []
  let i = start
  while (i < lines.length && marker.test(lines[i])) {
    let item = lines[i].replace(marker, '')
    i++
    // An indented line right after an item continues that item.
    while (
      i < lines.length &&
      /^\s+\S/.test(lines[i]) &&
      !BULLET.test(lines[i]) &&
      !NUMBERED.test(lines[i])
    ) {
      item += ' ' + lines[i].trim()
      i++
    }
    items.push(item)
  }
  return { items, next: i }
}

export default function RichText({ text }: { text: string }) {
  const lines = text.replace(/\r/g, '').split('\n')
  const out: ReactNode[] = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]

    if (line.trim().startsWith('```')) {
      const code: string[] = []
      i++
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        code.push(lines[i])
        i++
      }
      i++
      out.push(<pre key={out.length}><code>{code.join('\n')}</code></pre>)
      continue
    }

    if (BULLET.test(line)) {
      const { items, next } = readList(lines, i, BULLET)
      i = next
      out.push(<ul key={out.length}>{items.map((t, j) => <li key={j}>{inline(t)}</li>)}</ul>)
      continue
    }

    if (NUMBERED.test(line)) {
      const { items, next } = readList(lines, i, NUMBERED)
      i = next
      out.push(<ol key={out.length}>{items.map((t, j) => <li key={j}>{inline(t)}</li>)}</ol>)
      continue
    }

    if (!line.trim()) {
      i++
      continue
    }

    const para: string[] = []
    while (
      i < lines.length &&
      lines[i].trim() &&
      !lines[i].trim().startsWith('```') &&
      !BULLET.test(lines[i]) &&
      !NUMBERED.test(lines[i])
    ) {
      para.push(lines[i].trim())
      i++
    }
    out.push(<p key={out.length}>{inline(para.join(' '))}</p>)
  }

  return <div className="rich">{out}</div>
}
