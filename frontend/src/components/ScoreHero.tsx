import { useEffect, useState } from 'react'
import { band } from '../lib/score'

// The one animated moment on the report: the score counts up and the marker slides along the ruler.
export default function ScoreHero({ score }: { score: number }) {
  const target = Math.max(0, Math.min(100, Math.round(score)))
  const [shown, setShown] = useState(0)
  const [pos, setPos] = useState(0)
  const b = band(target)

  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      const raf = requestAnimationFrame(() => {
        setShown(target)
        setPos(target)
      })
      return () => cancelAnimationFrame(raf)
    }
    const start = performance.now()
    const duration = 900
    let raf = 0
    const tick = (now: number) => {
      const p = Math.min(1, (now - start) / duration)
      const eased = 1 - Math.pow(1 - p, 3)
      setShown(Math.round(target * eased))
      setPos(target * eased)
      if (p < 1) raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [target])

  return (
    <div>
      <div className={`score-row score-${b.key}`}>
        <span className="sr-only">Health score {target} out of 100, {b.label}</span>
        <span className="score-num" aria-hidden="true">{shown}</span>
        <span className="score-side" aria-hidden="true">
          <span className="score-of">out of 100</span>
          <span className="score-band">{b.label}</span>
        </span>
      </div>
      <div className="ruler" aria-hidden="true">
        <div className="ruler-track">
          <span className="ruler-mark" style={{ left: `${pos}%` }} />
        </div>
        <div className="ruler-scale">
          <span>At risk</span>
          <span>Fair</span>
          <span>Healthy</span>
        </div>
      </div>
    </div>
  )
}
