import { useMemo, useState } from 'react'

export type IssueItem = {
  key: string | number
  severity: string
  type: string
  location: string
  description: string
}

const FILTERS = ['ALL', 'HIGH', 'MEDIUM', 'LOW'] as const
type Filter = (typeof FILTERS)[number]
const ORDER: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 }

const label = (s: string) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : 'Unknown')

export default function IssueList({ items }: { items: IssueItem[] }) {
  const [filter, setFilter] = useState<Filter>('ALL')

  const sorted = useMemo(
    () => [...items].sort((a, b) => (ORDER[a.severity] ?? 3) - (ORDER[b.severity] ?? 3)),
    [items],
  )
  const counts: Record<string, number> = { ALL: items.length, HIGH: 0, MEDIUM: 0, LOW: 0 }
  items.forEach((i) => {
    counts[i.severity] = (counts[i.severity] ?? 0) + 1
  })
  const visible = filter === 'ALL' ? sorted : sorted.filter((i) => i.severity === filter)

  return (
    <div>
      <div className="filters" role="group" aria-label="Filter by severity">
        {FILTERS.filter((f) => f === 'ALL' || counts[f] > 0).map((f) => (
          <button
            key={f}
            type="button"
            className="chip"
            aria-pressed={filter === f}
            onClick={() => setFilter(f)}
          >
            {f === 'ALL' ? 'All' : label(f)}
            <span className="chip-count">{counts[f]}</span>
          </button>
        ))}
      </div>
      <ul className="issues">
        {visible.map((i) => (
          <li key={i.key} className="issue">
            <span className={`sev sev-${i.severity}`}>{label(i.severity)}</span>
            <div>
              <p className="issue-desc">{i.description}</p>
              <div className="issue-foot">
                <span className={`tag tag-${i.type}`}>{label(i.type)}</span>
                <code className="issue-loc">{i.location}</code>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
