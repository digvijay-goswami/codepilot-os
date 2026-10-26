export default function IssuesTable({ issues }: { issues: any[] }) {
  if (!issues || issues.length === 0) {
    return <div className="card center muted">No issues found.</div>
  }
  const high = issues.filter((i) => i.severity === 'HIGH').length

  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <div style={{ padding: 20, display: 'flex', justifyContent: 'space-between' }}>
        <strong>Detected Issues ({issues.length})</strong>
        <span className="badge sev-HIGH">{high} high priority</span>
      </div>
      <div style={{ overflowX: 'auto' }}>
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Severity</th>
              <th>Location</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            {issues.map((i) => (
              <tr key={i.id}>
                <td><span className={`badge type-${i.type}`}>{i.type}</span></td>
                <td><span className={`badge sev-${i.severity}`}>{i.severity}</span></td>
                <td className="mono">{i.filePath}:{i.lineNumber}</td>
                <td>{i.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}