import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { analyzeRepo } from '../api'

export default function Home() {
  const [url, setUrl] = useState('https://github.com/spring-projects/spring-petclinic')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const analyze = async () => {
    if (!url.includes('github.com')) {
      setError('Please enter a valid GitHub repository URL')
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
    <div className="container center" style={{ paddingTop: 80 }}>
      <h1 style={{ fontSize: 48, margin: '0 0 12px' }}>
        CodePilot <span style={{ color: '#8b5cf6' }}>OS</span>
      </h1>
      <p className="muted" style={{ fontSize: 18, marginBottom: 36 }}>
        Paste a public GitHub repo to get a health score, bugs, security issues, and a chat with its code.
      </p>
      <div className="row" style={{ maxWidth: 700, margin: '0 auto' }}>
        <input
          className="text"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://github.com/owner/repo"
        />
        <button className="btn" onClick={analyze} disabled={loading}>
          {loading ? (<><span className="spinner" />Analyzing...</>) : 'Analyze'}
        </button>
      </div>
      {loading && (
        <p className="muted" style={{ marginTop: 16 }}>
          This can take 1 to 3 minutes. The AI is reading the code and building the search index.
        </p>
      )}
      {error && <div className="error" style={{ maxWidth: 700, margin: '16px auto 0' }}>{error}</div>}
    </div>
  )
}