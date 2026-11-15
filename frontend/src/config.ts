// Base URLs of the Spring Boot backend and the FastAPI AI service.
//
// Locally (and in Docker/nginx) these are empty, so requests stay same-origin
// and go through the Vite proxy / nginx exactly as before.
// On Vercel, set VITE_BACKEND_URL and VITE_AI_URL to the Render URLs
// (for example https://codepilot-backend.onrender.com, no trailing slash).
export const BACKEND_URL = (import.meta.env.VITE_BACKEND_URL ?? '').replace(/\/$/, '')
export const AI_URL = (import.meta.env.VITE_AI_URL ?? '').replace(/\/$/, '')
