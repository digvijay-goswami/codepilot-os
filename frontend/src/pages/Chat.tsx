import { useEffect, useRef, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { askRepo } from '../api'
import PulseMark from '../components/PulseMark'
import RichText from '../components/RichText'

type Msg = {
  role: 'user' | 'ai'
  text: string
  error?: boolean
  sources?: { file: string; start_line: number; end_line: number }[]
}

const SUGGESTIONS = [
  'What does this repository do?',
  'Where is the database configured?',
  'Where are the main entry points?',
]

export default function Chat() {
  const { id } = useParams()
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [messages, setMessages] = useState<Msg[]>([
    { role: 'ai', text: 'Ask me anything about this repository. I answer from its code and show which files I used.' },
  ])
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [messages, loading])

  const send = async (override?: string) => {
    const q = (override ?? question).trim()
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
        { role: 'ai', error: true, text: 'Error: ' + (typeof detail === 'string' ? detail : e.message) },
      ])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="chat">
      <div className="chat-head">
        <Link to={`/report/${id}`} className="back" style={{ marginBottom: 14 }}>← Back to report</Link>
        <h1>Chat with repository #{id}</h1>
        <p>Answers are drawn from this repository's code.</p>
      </div>

      <div className="chat-log" aria-live="polite">
        {messages.map((m, i) => (
          <div key={i} className={`msg ${m.role}${m.error ? ' error' : ''}`}>
            {m.role === 'ai' && <span className="avatar"><PulseMark size={16} /></span>}
            <div className="msg-body">
              {m.role === 'ai' ? <RichText text={m.text} /> : m.text}
              {m.sources && m.sources.length > 0 && (
                <div className="sources">
                  <span>Sources</span>
                  {m.sources.map((s, j) => (
                    <span key={j} className="source-chip">{s.file}:{s.start_line}-{s.end_line}</span>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
        {messages.length === 1 && !loading && (
          <div className="suggestions">
            {SUGGESTIONS.map((s) => (
              <button key={s} type="button" className="chip" onClick={() => send(s)}>{s}</button>
            ))}
          </div>
        )}
        {loading && <div className="thinking"><span className="spinner" />Searching the code</div>}
        <div ref={endRef} />
      </div>

      <form
        className="composer"
        onSubmit={(e) => {
          e.preventDefault()
          send()
        }}
      >
        <div className="command">
          <input
            className="field"
            aria-label="Your question"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Ask about this codebase"
            autoFocus
          />
          <button className="btn" type="submit" disabled={loading || !question.trim()}>Send</button>
        </div>
      </form>
    </div>
  )
}
