import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { analyzeRepo } from '../api'

export default function Home() {
  const [url, setUrl] = useState('https://github.com/spring-projects/spring-petclinic')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const analyze = async () => {
    if (!url.includes('github.com')) {
      setError('Enter a GitHub repository link, like https://github.com/owner/repo')
      return
    }
    setLoading(true)
    setError('')
    try {
      const res = await analyzeRepo(url)
      navigate(`/report/${res.data.repositoryId}`)
    } catch (e: any) {
      setError(e.response?.data?.error || e.message || 'Analysis failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="page">
      <section className="hero">
        <h1>Find out what is wrong with a codebase before you ship it</h1>
        <p className="hero-lead">
          Paste a public GitHub repository. You get a health score, the bugs and security issues
          with their file and line, and a chat that answers from the code.
        </p>

        <form
          className="command"
          onSubmit={(e) => {
            e.preventDefault()
            if (!loading) analyze()
          }}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
            <circle cx="6" cy="5" r="2.2" />
            <circle cx="6" cy="19" r="2.2" />
            <circle cx="18" cy="8" r="2.2" />
            <path d="M6 7.2v9.6M18 10.2c0 4-6 3-12 6" />
          </svg>
          <input
            className="field"
            aria-label="GitHub repository link"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="https://github.com/owner/repo"
          />
          <button className="btn" type="submit" disabled={loading}>
            {loading ? (<><span className="spinner" />Analyzing</>) : 'Analyze repo'}
          </button>
        </form>

        {loading ? (
          <>
            <div className="progress" role="progressbar" aria-label="Analysis in progress" />
            <p className="hero-hint">
              Reading the code and building the search index. This takes 1 to 3 minutes.
            </p>
          </>
        ) : (
          <p className="hero-hint">
            Have a pull request instead? <Link to="/pr-review">Review a pull request</Link>.
          </p>
        )}
        {error && <div className="notice-error" role="alert">{error}</div>}
      </section>

      <section className="offers" aria-label="What you get">
        <div className="offer">
          <h3>A health score you can read at a glance</h3>
          <p>Five scores for quality, security, testing, performance and maintainability, rolled into one number.</p>
        </div>
        <div className="offer">
          <h3>Issues you can act on</h3>
          <p>Every issue names the file and line, says how serious it is, and explains why it matters.</p>
        </div>
        <div className="offer">
          <h3>Questions and tests on demand</h3>
          <p>Ask the repository how it works, or generate unit tests for any source file.</p>
        </div>
      </section>
    </main>
  )
}
