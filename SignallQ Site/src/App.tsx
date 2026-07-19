import { Route, Routes } from 'react-router-dom'
import HistoricoPage from './pages/HistoricoPage'
import HomePage from './pages/HomePage'
import NotFoundPage from './pages/NotFoundPage'
import PrivacidadePage from './pages/PrivacidadePage'
import ProPage from './pages/ProPage'
import QuemSomosPage from './pages/QuemSomosPage'
import TermosPage from './pages/TermosPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/pro" element={<ProPage />} />
      <Route path="/historico" element={<HistoricoPage />} />
      <Route path="/quem-somos" element={<QuemSomosPage />} />
      <Route path="/privacidade" element={<PrivacidadePage />} />
      <Route path="/termos" element={<TermosPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
