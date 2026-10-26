import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getHealth } from '../api'
import ScoreCircle from '../components/ScoreCircle'
import RadarCard from '../components/RadarCard'
import IssuesTable from '../components/IssuesTable'

export default function Report() {
  const { id } = useParams()
  const [data, setData] = useState<any>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getHealth(id ?? '')
      .then((res) => setData(res.data))
      .catch((e) => setError(e.response?.data?.error || e.message || 'Could not load report'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="container center muted">Loading report...</div>
  if (error || !data) {
    return (
      <div className="container">
        <div className="error">{error || 'No report found'}</div>
        <p><Link to="/" className="muted">← Back</Link></p>
      </div>
    )
  }

  const r = data.report
  const scores = [
    { label: 'Quality', value: r.qualityScore },
    { label: 'Security', value: r.securityScore },
    { label: 'Testing', value: r.testingScore },
    { label: 'Performance', value: r.performanceScore },
    { label: 'Maintainability', value: r.maintainabilityScore },
  ]

  return (
    <div className="container">
      <Link to="/" className="muted">← Back to Dashboard</Link>
      <div className="grid2" style={{ marginTop: 20 }}>
        <div className="card center" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
          <ScoreCircle score={r.healthScore} />
          <div className="muted">Overall Engineering Health</div>
          <Link to={`/chat/${id}`} className="btn-link">Chat with this repo →</Link>
        </div>
        <RadarCard report={r} />
      </div>

      <div className="grid5">
        {scores.map((s) => (
          <div key={s.label} className="card center" style={{ padding: 16 }}>
            <div className="score-label">{s.label}</div>
            <div className="score-value">{Math.round(s.value)}</div>
          </div>
        ))}
      </div>

      <IssuesTable issues={data.issues} />
    </div>
  )
}