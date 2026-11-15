import { BrowserRouter, Routes, Route, Link, NavLink } from 'react-router-dom'
import Home from './pages/Home'
import Report from './pages/Report'
import Chat from './pages/Chat'
import PrReview from './pages/PrReview'
import PulseMark from './components/PulseMark'

const linkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : '')

export default function App() {
  return (
    <BrowserRouter>
      <nav className="nav" aria-label="Main">
        <div className="nav-inner">
          <Link to="/" className="logo" aria-label="CodePilot OS home">
            <PulseMark />
            <span>CodePilot <em>OS</em></span>
          </Link>
          <div className="nav-links">
            <NavLink to="/" end className={linkClass}>Analyze a repo</NavLink>
            <NavLink to="/pr-review" className={linkClass}>Review a PR</NavLink>
          </div>
        </div>
      </nav>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/report/:id" element={<Report />} />
        <Route path="/chat/:id" element={<Chat />} />
        <Route path="/pr-review" element={<PrReview />} />
      </Routes>
    </BrowserRouter>
  )
}
