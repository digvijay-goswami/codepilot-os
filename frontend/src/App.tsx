import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import Home from './pages/Home'
import Report from './pages/Report'
import Chat from './pages/Chat'

export default function App() {
  return (
    <BrowserRouter>
      <nav className="nav">
        <Link to="/" className="logo">CodePilot <span>OS</span></Link>
        <span className="muted" style={{ fontSize: 12 }}>Spring Boot · Gemini · RAG</span>
      </nav>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/report/:id" element={<Report />} />
        <Route path="/chat/:id" element={<Chat />} />
      </Routes>
    </BrowserRouter>
  )
}