import axios from 'axios'

// Analysis can take a few minutes, so allow a long timeout
const api = axios.create({ timeout: 300000 })

export const analyzeRepo = (githubUrl: string) =>
  api.post('/api/repositories/analyze', { githubUrl })

export const getHealth = (id: string) =>
  api.get(`/api/repositories/${id}/health-score`)

export const askRepo = (repoId: number, question: string) =>
  api.post('/api/ai/chat', { repo_id: repoId, question })
