import IssueList from './IssueList'

export default function IssuesTable({ issues }: { issues: any[] }) {
  const items = (issues ?? []).map((i) => ({
    key: i.id,
    severity: i.severity,
    type: i.type,
    location: `${i.filePath}${i.lineNumber ? ':' + i.lineNumber : ''}`,
    description: i.description,
  }))
  const high = items.filter((i) => i.severity === 'HIGH').length

  return (
    <section className="section" aria-labelledby="issues-title">
      <div className="section-head">
        <h2 id="issues-title">Issues found</h2>
        <span className="muted">
          {items.length} in total{high > 0 ? `, ${high} high priority` : ''}
        </span>
      </div>
      {items.length === 0 ? (
        <p className="empty">No issues were found in the files that were analyzed.</p>
      ) : (
        <IssueList items={items} />
      )}
    </section>
  )
}
