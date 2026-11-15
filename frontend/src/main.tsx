import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './index.css'
import { BACKEND_URL, AI_URL } from './config'

// Render's free tier puts services to sleep after 15 idle minutes.
// Ping both on page load so they are already waking up while the user types a repo URL.
if (BACKEND_URL) fetch(`${BACKEND_URL}/api/health`).catch(() => {})
if (AI_URL) fetch(`${AI_URL}/health`).catch(() => {})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)