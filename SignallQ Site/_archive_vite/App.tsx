import { Route, Routes } from 'react-router-dom'
import { PwaToastStack } from './components/PwaToastStack'
import BufferbloatPage from './pages/BufferbloatPage'
import CgnatPage from './pages/CgnatPage'
import HistoricoPage from './pages/HistoricoPage'
import ComoMedimosPage from './pages/ComoMedimosPage'
import HomePage from './pages/HomePage'
import NotFoundPage from './pages/NotFoundPage'
import PrivacidadePage from './pages/PrivacidadePage'
import ProPage from './pages/ProPage'
import QuemSomosPage from './pages/QuemSomosPage'
import TermosPage from './pages/TermosPage'
import TestePage from './pages/TestePage'
import InternetLentaPage from './pages/diagnostico/InternetLentaPage'
import PingAltoPage from './pages/diagnostico/PingAltoPage'
import VideochamadaPage from './pages/diagnostico/VideochamadaPage'
import WifiLentoPage from './pages/diagnostico/WifiLentoPage'
import TesteLatenciaPage from './pages/ferramentas/TesteLatenciaPage'

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/pro" element={<ProPage />} />
        <Route path="/historico" element={<HistoricoPage />} />
        <Route path="/como-medimos" element={<ComoMedimosPage />} />
        <Route path="/quem-somos" element={<QuemSomosPage />} />
        <Route path="/privacidade" element={<PrivacidadePage />} />
        <Route path="/termos" element={<TermosPage />} />
        <Route path="/internet-boa-mas-travando" element={<BufferbloatPage />} />
        <Route path="/lag-em-jogos-online" element={<CgnatPage />} />
        <Route path="/teste" element={<TestePage />} />
        
        <Route path="/diagnostico/internet-lenta" element={<InternetLentaPage />} />
        <Route path="/diagnostico/ping-alto" element={<PingAltoPage />} />
        <Route path="/diagnostico/videochamada-travando" element={<VideochamadaPage />} />
        <Route path="/diagnostico/wifi-lento" element={<WifiLentoPage />} />
        <Route path="/ferramentas/teste-de-latencia" element={<TesteLatenciaPage />} />
        
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      <PwaToastStack />
    </>
  )
}
