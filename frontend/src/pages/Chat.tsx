import { useEffect, useRef, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { askRepo } from '../api'

type Msg = {
  role: 'user' | 'ai'
  text: string
  sources?: { file: string; start_line: number; end_line: number }[]
}

export default function Chat() {
  const { id } = useParams()
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [messages, setMessages] = useState<Msg[]>([
    { role: 'ai', text: 'Hi! Ask me anything about this repository, for example: Where is the database configured?' },
  ])
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  const send = async () => {
    const q = question.trim()
    if (!q || loading) return
    setQuestion('')
    setMessages((m) => [...m, { role: 'user', text: q }])
    setLoading(true)
    try {
      const res = await askRepo(Number(id), q)
      setMessages((m) => [...m, { role: 'ai', text: res.data.answer, sources: res.data.sources }])
    } catch (e: any) {
      const detail = e.response?.data?.detail
      setMessages((m) => [
        ...m,
        { role: 'ai', text: 'Error: ' + (typeof detail === 'string' ? detail : e.message) },
      ])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container" style={{ maxWidth: 760 }}>
      <Link to={`/report/${id}`} className="muted">← Back to report</Link>
      <h2 style={{ margin: '16px 0 4px' }}>Chat with repository #{id}</h2>
      <p className="muted" style={{ marginTop: 0 }}>Answers come from the code stored in your vector database.</p>

      <div className="card chat-box">
        {messages.map((m, i) => (
          <div key={i} className={`msg ${m.role}`}>
            {m.text}
            {m.sources && m.sources.length > 0 && (
              <div className="sources">
                Sources: {m.sources.map((s) => `${s.file} (${s.start_line}-${s.end_line})`).join(' · ')}
              </div>
            )}
          </div>
        ))}
        {loading && <div className="muted"><span className="spinner" />Thinking...</div>}
        <div ref={endRef} />
      </div>

      <div className="row">
        <input
          className="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') send() }}
          placeholder="Ask about this codebase..."
        />
        <button className="btn" onClick={send} disabled={loading}>Send</button>
      </div>
    </div>
  )
}