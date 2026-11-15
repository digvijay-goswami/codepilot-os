import { useState } from 'react'
import IssueList from '../components/IssueList'
import { BACKEND_URL } from '../config'

type Issue = { file: string; line: number; severity: string; type: string; description: string }
type Suggestion = { file: string; suggestion: string }
type Review = {
  verdict: string
  risk_level: string
  summary: string
  issues?: Issue[]
  suggestions?: Suggestion[]
  positives?: string[]
}
type PrResult = {
  prUrl: string
  title: string
  author: string
  changedFiles: number
  additions: number
  deletions: number
  diffTruncated: boolean
  review: Review
}

const VERDICT_LABEL: Record<string, string> = {
  APPROVE: 'Approve',
  REQUEST_CHANGES: 'Request changes',
  COMMENT: 'Comment',
}

const risk = (r: string) => (r ? r.charAt(0) + r.slice(1).toLowerCase() + ' risk' : '')

export default function PrReview() {
  const [url, setUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<PrResult | null>(null)

  const runReview = async () => {
    if (!/github\.com\/[^/]+\/[^/]+\/pull\/\d+/.test(url)) {
      setError('Enter a pull request link like https://github.com/owner/repo/pull/123')
      return
    }
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const res = await fetch(`${BACKEND_URL}/api/pr-review`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prUrl: url.trim() }),
      })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(data.error || `Request failed (${res.status})`)
      setResult(data)
    } catch (e: any) {
      setError(e.message || 'Review failed')
    } finally {
      setLoading(false)
    }
  }

  const review = result?.review
  const issues = review?.issues ?? []
  const suggestions = review?.suggestions ?? []
  const positives = review?.positives ?? []

  return (
    <main className="page">
      <section className="hero" style={{ paddingTop: 24 }}>
        <h1 style={{ fontSize: 'clamp(2.2rem, 4.6vw, 3.6rem)' }}>Review a pull request</h1>
        <p className="hero-lead">
          Paste a public GitHub pull request link. You get a verdict, the issues found in the diff
          and suggestions for the author.
        </p>
        <form
          className="command"
          onSubmit={(e) => {
            e.preventDefault()
            if (!loading) runReview()
          }}
        >
          <input
            className="field"
            aria-label="Pull request link"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="https://github.com/owner/repo/pull/123"
          />
          <button className="btn" type="submit" disabled={loading}>
            {loading ? (<><span className="spinner" />Reviewing</>) : 'Review PR'}
          </button>
        </form>
        {loading && <div className="progress" role="progressbar" aria-label="Review in progress" />}
        {error && <div className="notice-error" role="alert">{error}</div>}
      </section>

      {result && review && (
        <>
          <section className={`verdict-banner verdict-${review.verdict}`} style={{ marginTop: 48 }}>
            <div>
              <div className="verdict-label">{VERDICT_LABEL[review.verdict] ?? review.verdict}</div>
              <h2 className="pr-title">{result.title}</h2>
              <div className="pr-meta">
                <span>by {result.author || 'unknown'}</span>
                <span>{result.changedFiles} {result.changedFiles === 1 ? 'file' : 'files'} changed</span>
                <span className="pr-add">+{result.additions}</span>
                <span className="pr-del">-{result.deletions}</span>
                <a href={result.prUrl} target="_blank" rel="noreferrer" style={{ color: 'var(--accent)' }}>
                  Open on GitHub
                </a>
              </div>
              <p className="pr-summary">{review.summary}</p>
              {result.diffTruncated && (
                <p className="fineprint">
                  This pull request is large, so only the first part of the diff was reviewed.
                </p>
              )}
            </div>
            <span className="risk">{risk(review.risk_level)}</span>
          </section>

          <section className="section" aria-labelledby="pr-issues">
            <div className="section-head"><h2 id="pr-issues">Issues found</h2><span className="muted">{issues.length} in total</span></div>
            {issues.length === 0 ? (
              <p className="empty">No issues were found in this diff.</p>
            ) : (
              <IssueList
                items={issues.map((it, i) => ({
                  key: i,
                  severity: it.severity,
                  type: it.type,
                  location: `${it.file}${it.line ? ':' + it.line : ''}`,
                  description: it.description,
                }))}
              />
            )}
          </section>

          {suggestions.length > 0 && (
            <section className="section" aria-labelledby="pr-suggestions">
              <div className="section-head"><h2 id="pr-suggestions">Suggestions</h2></div>
              <ul className="list">
                {suggestions.map((s, i) => (
                  <li key={i}><code>{s.file}</code><br />{s.suggestion}</li>
                ))}
              </ul>
            </section>
          )}

          {positives.length > 0 && (
            <section className="section" aria-labelledby="pr-positives">
              <div className="section-head"><h2 id="pr-positives">Done well</h2></div>
              <ul className="list">
                {positives.map((p, i) => <li key={i}>{p}</li>)}
              </ul>
            </section>
          )}
        </>
      )}
    </main>
  )
}
