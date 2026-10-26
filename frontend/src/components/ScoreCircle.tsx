export default function ScoreCircle({ score }: { score: number }) {
  const color = score >= 80 ? '#22c55e' : score >= 60 ? '#eab308' : '#ef4444'
  const radius = 52
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (Math.min(score, 100) / 100) * circumference

  return (
    <div style={{ position: 'relative', width: 180, height: 180 }}>
      <svg width="180" height="180" viewBox="0 0 120 120">
        <circle cx="60" cy="60" r={radius} stroke="#272735" strokeWidth="10" fill="none" />
        <circle
          cx="60"
          cy="60"
          r={radius}
          stroke={color}
          strokeWidth="10"
          fill="none"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          transform="rotate(-90 60 60)"
        />
      </svg>
      <div
        style={{
          position: 'absolute',
          inset: 0,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <div style={{ fontSize: 44, fontWeight: 700, color }}>{Math.round(score)}</div>
        <div style={{ fontSize: 12, color: '#9ca3af' }}>Health Score</div>
      </div>
    </div>
  )
}