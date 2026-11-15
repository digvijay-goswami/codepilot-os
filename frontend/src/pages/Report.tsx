import TestGenerator from '../components/TestGenerator'
import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getHealth } from '../api'
import ScoreHero from '../components/ScoreHero'
import RadarCard from '../components/RadarCard'
import IssuesTable from '../components/IssuesTable'
import { band } from '../lib/score'

export default function Report() {
  const { id } = useParams()
  const [data, setData] = useState<any>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revealed, setRevealed] = useState(false)

  useEffect(() => {
    getHealth(id ?? '')
      .then((res) => setData(res.data))
      .catch((e) => setError(e.response?.data?.error || e.message || 'Could not load report'))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (!data) return
    const t = setTimeout(() => setRevealed(true), 80)
    return () => clearTimeout(t)
  }, [data])

  if (loading) {
    return (
      <main className="page">
        <p className="muted"><span className="spinner" /> &nbsp;Loading report...</p>
      </main>
    )
  }
  if (error || !data) {
    return (
      <main className="page">
        <Link to="/" className="back">← Analyze another repo</Link>
        <div className="notice-error" role="alert">{error || 'No report was found for this repository.'}</div>
      </main>
    )
  }

  const r = data.report
  const issues: any[] = data.issues ?? []
  const high = issues.filter((i) => i.severity === 'HIGH').length
  const scores = [
    { label: 'Quality', value: Math.round(r.qualityScore) },
    { label: 'Security', value: Math.round(r.securityScore) },
    { label: 'Testing', value: Math.round(r.testingScore) },
    { label: 'Performance', value: Math.round(r.performanceScore) },
    { label: 'Maintainability', value: Math.round(r.maintainabilityScore) },
  ]
  const weakest = scores.reduce((a, b) => (b.value < a.value ? b : a))
  const strongest = scores.reduce((a, b) => (b.value > a.value ? b : a))

  return (
    <main className="page">
      <Link to="/" className="back">← Analyze another repo</Link>

      <section className="score-hero" aria-label="Overall health">
        <ScoreHero score={r.healthScore} />
        <div>
          <dl className="hero-notes">
            <div className="note-row"><dt>Weakest area</dt><dd>{weakest.label}, {weakest.value}</dd></div>
            <div className="note-row"><dt>Strongest area</dt><dd>{strongest.label}, {strongest.value}</dd></div>
            <div className="note-row">
              <dt>Issues found</dt>
              <dd>{issues.length}{high > 0 ? `, ${high} high priority` : ''}</dd>
            </div>
          </dl>
          <div className="hero-actions">
            <Link to={`/chat/${id}`} className="btn">Chat with this repo</Link>
            <a href="#tests" className="btn btn-ghost">Generate tests</a>
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="dims-title">
        <div className="section-head"><h2 id="dims-title">Where the score comes from</h2></div>
        <div className="dimensions">
          <RadarCard report={r} />
          <div>
            {scores.map((s) => (
              <div key={s.label} className={`dim dim-${band(s.value).key}`}>
                <span className="dim-name">{s.label}</span>
                <div className="dim-track" role="img" aria-label={`${s.label} ${s.value} out of 100`}>
                  <div className="dim-fill" style={{ width: revealed ? `${s.value}%` : '0%' }} />
                </div>
                <span className="dim-val">{s.value}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <IssuesTable issues={issues} />

      <TestGenerator repoId={id ?? ''} />
    </main>
  )
}
