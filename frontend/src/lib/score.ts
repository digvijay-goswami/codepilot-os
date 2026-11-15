export type Band = { key: 'good' | 'warn' | 'bad'; label: string }

// Same thresholds the old ring used: 80+ is healthy, 60 to 79 is fair.
export function band(score: number): Band {
  if (score >= 80) return { key: 'good', label: 'Healthy' }
  if (score >= 60) return { key: 'warn', label: 'Fair' }
  return { key: 'bad', label: 'At risk' }
}
