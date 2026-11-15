import axios from 'axios'
import { BACKEND_URL, AI_URL } from './config'

// Analysis can take a few minutes, so allow a long timeout
const api = axios.create({ timeout: 300000 })

export const analyzeRepo = (githubUrl: string) =>
  api.post(`${BACKEND_URL}/api/repositories/analyze`, { githubUrl })

export const getHealth = (id: string) =>
  api.get(`${BACKEND_URL}/api/repositories/${id}/health-score`)

export const askRepo = (repoId: number, question: string) =>
  api.post(`${AI_URL}/api/ai/chat`, { repo_id: repoId, question })
