import { useEffect, useState } from 'react'
import { BACKEND_URL } from '../config'

type GenResult = {
  tests: string
  language: string
  framework: string
  test_file_name: string
  truncated: boolean
}

export default function TestGenerator({ repoId }: { repoId: string }) {
  const [files, setFiles] = useState<string[]>([])
  const [selected, setSelected] = useState('')
  const [loadingFiles, setLoadingFiles] = useState(Boolean(repoId))
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<GenResult | null>(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!repoId) return
    let cancelled = false
    fetch(`${BACKEND_URL}/api/repositories/${repoId}/files`)
      .then(async (res) => {
        const data = await res.json().catch(() => ({}))
        if (!res.ok) throw new Error(data.error || `Could not load files (${res.status})`)
        return data
      })
      .then((data) => {
        if (cancelled) return
        const list: string[] = data.files ?? []
        setFiles(list)
        setSelected(list[0] ?? '')
      })
      .catch((e: any) => {
        if (!cancelled) setError(e.message || 'Could not load files')
      })
      .finally(() => {
        if (!cancelled) setLoadingFiles(false)
      })
    return () => {
      cancelled = true
    }
  }, [repoId])

  const generate = async () => {
    if (!selected) return
    setGenerating(true)
    setError('')
    setResult(null)
    setCopied(false)
    try {
      const res = await fetch(`${BACKEND_URL}/api/repositories/${repoId}/generate-tests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filePath: selected }),
      })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(data.error || `Request failed (${res.status})`)
      setResult(data)
    } catch (e: any) {
      setError(e.message || 'Test generation failed')
    } finally {
      setGenerating(false)
    }
  }

  const copy = async () => {
    if (!result) return
    try {
      await navigator.clipboard.writeText(result.tests)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      setError('Copy failed. Select the text in the code box and copy it by hand.')
    }
  }

  return (
    <section id="tests" className="section" style={{ scrollMarginTop: 80 }}>
      <div className="panel">
        <h2>Generate unit tests</h2>
        <p className="muted">
          Pick a source file and the AI writes tests for it using the usual framework for its language.
        </p>

        {loadingFiles ? (
          <p className="muted" style={{ marginTop: 20 }}>Loading files...</p>
        ) : files.length === 0 ? (
          <p className="muted" style={{ marginTop: 20 }}>No source files were found to write tests for.</p>
        ) : (
          <div className="tests-row">
            <select
              className="select"
              aria-label="Source file"
              value={selected}
              onChange={(e) => setSelected(e.target.value)}
              disabled={generating}
            >
              {files.map((f) => (
                <option key={f} value={f}>{f}</option>
              ))}
            </select>
            <button className="btn" onClick={generate} disabled={generating || !selected}>
              {generating ? (<><span className="spinner" />Writing tests</>) : 'Generate tests'}
            </button>
          </div>
        )}

        {generating && <p className="fineprint">This can take up to 30 seconds.</p>}
        {error && <div className="notice-error">{error}</div>}

        {result && (
          <>
            <div className="code">
              <div className="code-head">
                <span>
                  <span className="file">{result.test_file_name}</span>
                  <span className="meta"> &nbsp;{result.language}, {result.framework}</span>
                </span>
                <button className="btn btn-ghost btn-sm" onClick={copy}>
                  {copied ? 'Copied' : 'Copy'}
                </button>
              </div>
              <pre><code>{result.tests}</code></pre>
            </div>
            {result.truncated && (
              <p className="fineprint">
                The file is large, so the tests were written from the first part of it only.
              </p>
            )}
            <p className="fineprint">
              AI-written tests are a starting point. Read and run them before you rely on them.
            </p>
          </>
        )}
      </div>
    </section>
  )
}
